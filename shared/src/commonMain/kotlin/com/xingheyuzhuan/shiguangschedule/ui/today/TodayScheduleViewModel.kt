package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.api.exam.ExamImportItem
import com.xingheyuzhuan.shiguangschedule.data.api.exam.ExamSyncException
import com.xingheyuzhuan.shiguangschedule.data.api.exam.NjuptExamSyncClient
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEvent
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.ScheduleEventRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.uuid.Uuid

@KoinViewModel
class TodayScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val timeScheduleRepository: TimeScheduleRepository,
    private val scheduleEventRepository: ScheduleEventRepository,
    private val njuptExamSyncClient: NjuptExamSyncClient
) : ViewModel() {

    companion object {
        private const val DEFAULT_SEMESTER_TOTAL_WEEKS = 20
        private const val MAX_TIME_SORT_KEY = "99:99"
    }

    val gridStyle: StateFlow<ScheduleGridStyle> = styleSettingsRepository.styleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleGridStyle())

    /** 上次使用的南邮考试同步班级代码。 */
    val njuptExamClassCode: StateFlow<String> = appSettingsRepository.getNjuptExamClassCode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _njuptExamSyncState = MutableStateFlow(NjuptExamSyncUiState())
    val njuptExamSyncState: StateFlow<NjuptExamSyncUiState> = _njuptExamSyncState.asStateFlow()

    private var njuptExamSyncJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = appSettingsRepository.getAppSettings()
        .flatMapLatest { settings ->
            val tableId = settings.currentCourseTableId
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val dayOfWeek = today.dayOfWeek.isoDayNumber

            combine(
                appSettingsRepository.calculateCurrentWeekFromDb(),
                appSettingsRepository.getCourseTableConfigFlow(tableId),
                timeScheduleRepository.observeEffectiveTimeSlots(tableId, today)
            ) { weekIndex: Int?, config: CourseTableConfig?, timeSlots: List<TimeSlot> ->

                val startDate = config?.semesterStartDate?.let {
                    try { LocalDate.parse(it) } catch (e: Exception) { null }
                }

                val totalWeeks = config?.semesterTotalWeeks ?: DEFAULT_SEMESTER_TOTAL_WEEKS
                val firstDayOfWeek = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber

                // 判定今天是否处于生效的假期中
                val isSkippedDay = settings.holidays.any { holiday ->
                    holiday.isHoliday && holiday.dates.contains(today)
                }

                val status = when {
                    config?.semesterStartDate == null -> TodayStatus.NoSemesterConfig
                    startDate != null && today < startDate -> TodayStatus.Vacation
                    weekIndex == null -> TodayStatus.SemesterEnded
                    else -> TodayStatus.Normal
                }

                // 记录状态与精准计算所需的物理配置
                DataSnapshot(
                    status = status,
                    weekIndex = weekIndex,
                    timeSlots = timeSlots,
                    startDate = startDate,
                    totalWeeks = totalWeeks,
                    firstDayOfWeek = firstDayOfWeek,
                    isSkippedDay = isSkippedDay
                )
            }.flatMapLatest { snapshot ->
                val coursesFlow = if (snapshot.status == TodayStatus.Normal && snapshot.weekIndex != null && !snapshot.isSkippedDay) {
                    courseTableRepository.getCoursesForDay(tableId, snapshot.weekIndex, dayOfWeek)
                } else {
                    flowOf(emptyList())
                }
                val eventsFlow = scheduleEventRepository.getEventsForDateFlow(tableId, today.toString())
                val upcomingExamsFlow = scheduleEventRepository.getUpcomingExamsFlow(tableId, today.toString())

                combine(coursesFlow, eventsFlow, upcomingExamsFlow) { courses, events, exams ->
                    createSuccessState(courses, snapshot, today, events, exams)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)

    /**
     * 添加日程事件（每日日程、考试、补课）
     */
    fun addEvent(
        title: String,
        type: String,
        date: String,
        startTime: String? = null,
        endTime: String? = null,
        location: String? = null,
        detail: String? = null
    ) {
        viewModelScope.launch {
            val settings = appSettingsRepository.getAppSettings().first()
            val event = ScheduleEvent(
                id = Uuid.random().toString(),
                courseTableId = settings.currentCourseTableId,
                title = title.trim(),
                type = type,
                date = date.trim(),
                startTime = startTime?.trim()?.takeIf { it.isNotBlank() },
                endTime = endTime?.trim()?.takeIf { it.isNotBlank() },
                location = location?.trim()?.takeIf { it.isNotBlank() },
                detail = detail?.trim()?.takeIf { it.isNotBlank() }
            )
            scheduleEventRepository.insertEvent(event)
        }
    }

    /**
     * 批量添加日程事件，自动补全当前课表 ID。
     */
    fun batchAddEvents(events: List<ScheduleEvent>) {
        if (events.isEmpty()) return
        viewModelScope.launch {
            val settings = appSettingsRepository.getAppSettings().first()
            val preparedEvents = events.map { event ->
                event.copy(
                    id = event.id.ifBlank { Uuid.random().toString() },
                    courseTableId = settings.currentCourseTableId
                )
            }
            scheduleEventRepository.insertEvents(preparedEvents)
        }
    }

    /**
     * 查询并同步南邮 StuDev 班级在线考试安排。
     */
    fun syncNjuptExams(classCode: String) {
        val normalizedCode = classCode.filterNot { it.isWhitespace() }.uppercase()
        if (normalizedCode.isEmpty()) {
            _njuptExamSyncState.value = NjuptExamSyncUiState(
                hasQueried = true,
                errorMessage = "请输入班级代码"
            )
            return
        }

        njuptExamSyncJob?.cancel()
        njuptExamSyncJob = viewModelScope.launch {
            appSettingsRepository.saveNjuptExamClassCode(normalizedCode)
            _njuptExamSyncState.value = NjuptExamSyncUiState(isQuerying = true)

            try {
                val exams = njuptExamSyncClient.fetchExams(normalizedCode)
                _njuptExamSyncState.value = if (exams.isEmpty()) {
                    NjuptExamSyncUiState(
                        hasQueried = true,
                        errorMessage = "未查询到该班级的考试安排"
                    )
                } else {
                    NjuptExamSyncUiState(
                        hasQueried = true,
                        exams = exams
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _njuptExamSyncState.value = NjuptExamSyncUiState(
                    hasQueried = true,
                    errorMessage = (e as? ExamSyncException)?.message
                        ?: "网络连接失败，请检查网络后重试"
                )
            }
        }
    }

    /**
     * 重置南邮考试同步状态并取消进行中的查询。
     */
    fun resetNjuptExamSyncState() {
        njuptExamSyncJob?.cancel()
        njuptExamSyncJob = null
        _njuptExamSyncState.value = NjuptExamSyncUiState()
    }

    /**
     * 切换每日待办的完成状态
     */
    fun toggleEventCompleted(event: ScheduleEvent) {
        viewModelScope.launch {
            scheduleEventRepository.updateEvent(event.copy(isCompleted = !event.isCompleted))
        }
    }

    /**
     * 删除指定事件
     */
    fun deleteEvent(event: ScheduleEvent) {
        viewModelScope.launch {
            scheduleEventRepository.deleteEvent(event)
        }
    }

    /**
     * 内部辅助快照类
     */
    private data class DataSnapshot(
        val status: TodayStatus,
        val weekIndex: Int?,
        val timeSlots: List<TimeSlot>,
        val startDate: LocalDate?,
        val totalWeeks: Int,
        val firstDayOfWeek: Int,
        val isSkippedDay: Boolean
    )

    private fun createSuccessState(
        courses: List<CourseWithWeeks>,
        snapshot: DataSnapshot,
        today: LocalDate,
        events: List<ScheduleEvent> = emptyList(),
        upcomingExams: List<ScheduleEvent> = emptyList()
    ): TodayUiState.Success {
        val slotMap = snapshot.timeSlots.associateBy { it.number }

        val displayModels = courses.map { item ->
            val startSlot = slotMap[item.course.startSection]
            val endSlot = slotMap[item.course.endSection]
            CourseDisplayModel(
                course = item.course,
                startTime = item.course.customStartTime ?: startSlot?.startTime,
                endTime = item.course.customEndTime ?: endSlot?.endTime
            )
        }.sortedWith(
            compareBy<CourseDisplayModel> { it.startTime ?: MAX_TIME_SORT_KEY }
                .thenBy { it.endTime ?: MAX_TIME_SORT_KEY }
        )

        return TodayUiState.Success(
            courses = displayModels,
            weekIndex = snapshot.weekIndex ?: 0,
            today = today,
            status = snapshot.status,
            startDate = snapshot.startDate,
            totalWeeks = snapshot.totalWeeks,
            firstDayOfWeek = snapshot.firstDayOfWeek,
            events = events,
            upcomingExams = upcomingExams
        )
    }
}

/**
 * 南邮考试在线同步的界面状态。
 */
data class NjuptExamSyncUiState(
    val isQuerying: Boolean = false,
    val hasQueried: Boolean = false,
    val exams: List<ExamImportItem> = emptyList(),
    val errorMessage: String? = null
)

data class CourseDisplayModel(
    val course: Course,
    val startTime: String?,
    val endTime: String?
)

enum class TodayStatus { Normal, NoSemesterConfig, SemesterEnded, Vacation }

sealed class TodayUiState {
    data object Loading : TodayUiState()
    data class Success(
        val courses: List<CourseDisplayModel>,
        val weekIndex: Int,
        val today: LocalDate,
        val status: TodayStatus,
        val startDate: LocalDate?,
        val totalWeeks: Int,
        val firstDayOfWeek: Int,
        val events: List<ScheduleEvent> = emptyList(),
        val upcomingExams: List<ScheduleEvent> = emptyList()
    ) : TodayUiState()
}