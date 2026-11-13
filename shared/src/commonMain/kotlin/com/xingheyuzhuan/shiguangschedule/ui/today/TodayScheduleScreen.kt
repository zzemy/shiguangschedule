package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.api.exam.ExamImportItem
import com.xingheyuzhuan.shiguangschedule.data.api.exam.IcsExamParser
import com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEvent
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.nlp.SmartScheduleParser
import com.xingheyuzhuan.shiguangschedule.tool.FileManagerCallbacks
import com.xingheyuzhuan.shiguangschedule.tool.rememberFileManager
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import com.xingheyuzhuan.shiguangschedule.ui.theme.rememberCourseTextColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.Uuid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.calendar_today_24px
import shiguangschedule.shared.generated.resources.close_24px
import shiguangschedule.shared.generated.resources.course_position_prefix
import shiguangschedule.shared.generated.resources.course_teacher_prefix
import shiguangschedule.shared.generated.resources.date_format_year_month_day
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.label_credit
import shiguangschedule.shared.generated.resources.label_remark
import shiguangschedule.shared.generated.resources.status_semester_ended
import shiguangschedule.shared.generated.resources.text_no_courses_today
import shiguangschedule.shared.generated.resources.title_current_week
import shiguangschedule.shared.generated.resources.title_semester_not_set
import shiguangschedule.shared.generated.resources.title_today_schedule
import shiguangschedule.shared.generated.resources.title_vacation_until_start
import shiguangschedule.shared.generated.resources.update_24px
import shiguangschedule.shared.generated.resources.week_days_full_names
import kotlin.time.Clock
import kotlin.time.Instant

private const val DEFAULT_TIME_ZERO = "00:00"
private const val EMPTY_TIME_PLACEHOLDER = "--:--"
private const val DEFAULT_DAYS_ZERO = "0"
private const val DEFAULT_OVERDUE_DAYS = 1
private const val TIMELINE_TIME_SORT_KEY = "99:99"

/** 考试 / 补课的细微左侧色条颜色。 */
private val ExamAccent = Color(0xFFE0523F)
private val MakeupAccent = Color(0xFF3B78E7)

@Composable
private fun eventAccentColor(type: String): Color = when (type) {
    "EXAM" -> ExamAccent
    "MAKEUP" -> MakeupAccent
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun CountdownChip(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
private fun DeleteAction(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.delete_24px),
            contentDescription = "删除",
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CompactCheckbox(checked: Boolean, onCheckedChange: () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

/** 统一的通透悬浮卡片：12dp 内边距 + 14dp 圆角。 */
@Composable
private fun EventCardShell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

private fun countdownLabel(days: Int): String = when {
    days <= 0 -> "今天"
    days == 1 -> "明天"
    else -> "还有 $days 天"
}

/** 统一短日期格式：11.21 08:00 - 09:50 · 教2-302 */
private fun ScheduleEvent.shortDateMeta(): String {
    val dateText = formatShortDate(date)
    val timeText = when {
        !startTime.isNullOrBlank() && !endTime.isNullOrBlank() ->
            "${startTime.take(5)} - ${endTime.take(5)}"
        !startTime.isNullOrBlank() -> startTime.take(5)
        else -> ""
    }
    val dateTimeText = listOfNotNull(
        dateText.takeIf { it.isNotBlank() },
        timeText.takeIf { it.isNotBlank() }
    ).joinToString(" ")
    val placeText = listOfNotNull(
        location?.takeIf { it.isNotBlank() },
        teacher?.takeIf { it.isNotBlank() }?.let { "教师：$it" },
        detail?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    return listOfNotNull(
        dateTimeText.takeIf { it.isNotBlank() },
        placeText.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
}

/**
 * 添加日程抽屉的表单草稿。
 *
 * 抽屉被下滑关闭时不会销毁草稿，再次打开可继续编辑；仅在保存成功后清空。
 */
public data class DraftState(
    val smartInput: String = "",
    val type: String = "DAILY",
    val title: String = "",
    val date: String = "",
    val isAllDay: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
    val location: String = "",
    val detail: String = "",
    val teacher: String = ""
) {
    val hasInput: Boolean
        get() = smartInput.isNotBlank() ||
            title.isNotBlank() ||
            startTime.isNotBlank() ||
            endTime.isNotBlank() ||
            location.isNotBlank() ||
            detail.isNotBlank() ||
            teacher.isNotBlank() ||
            type != "DAILY"
}

private fun ScheduleEvent.toDraftState(): DraftState = DraftState(
    type = type,
    title = title,
    date = date,
    isAllDay = startTime.isNullOrBlank(),
    startTime = startTime.orEmpty(),
    endTime = endTime.orEmpty(),
    location = location.orEmpty(),
    detail = detail.orEmpty(),
    teacher = teacher.orEmpty()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: TodayScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridStyle by viewModel.gridStyle.collectAsState()
    val examSyncState by viewModel.njuptExamSyncState.collectAsState()
    val savedClassCode by viewModel.njuptExamClassCode.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    var addDraft by remember { mutableStateOf<DraftState?>(null) }
    var editingEvent by remember { mutableStateOf<ScheduleEvent?>(null) }
    var showNjuptExamSyncSheet by remember { mutableStateOf(false) }
    var icsPreviewExams by remember { mutableStateOf<List<ExamImportItem>?>(null) }

    val fileManager = rememberFileManager(
        FileManagerCallbacks(
            onFileImported = { bytes, _ ->
                if (bytes != null) {
                    scope.launch {
                        val parsedExams = withContext(Dispatchers.Default) {
                            IcsExamParser.parse(bytes.decodeToString())
                        }
                        if (parsedExams.isEmpty()) {
                            ToastManager.show("未在该 .ics 文件中找到日程事件")
                        } else {
                            icsPreviewExams = parsedExams
                        }
                    }
                }
            }
        )
    )

    val todayFromState = (uiState as? TodayUiState.Success)?.today
    val semesterStartDate = (uiState as? TodayUiState.Success)?.startDate
    val currentWeek = (uiState as? TodayUiState.Success)?.weekIndex?.takeIf { it > 0 }
    val fallbackToday = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val defaultDate = (todayFromState ?: fallbackToday).toString()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.title_today_schedule),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (addDraft == null) addDraft = DraftState(date = defaultDate)
                            showAddSheet = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.add_24px),
                            contentDescription = "添加日程",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is TodayUiState.Loading -> { /* 可放置圆圈加载 */ }
                is TodayUiState.Success -> {
                    TodayContent(
                        state = state,
                        gridStyle = gridStyle,
                        isDark = isDark,
                        onToggleEvent = { viewModel.toggleEventCompleted(it) },
                        onDeleteEvent = { viewModel.deleteEvent(it) },
                        onEditEvent = { editingEvent = it }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddScheduleEventBottomSheet(
            defaultDate = defaultDate,
            draft = addDraft ?: DraftState(date = defaultDate),
            onDraftChange = { addDraft = it },
            onDismiss = { showAddSheet = false },
            onOpenNjuptSync = {
                showAddSheet = false
                viewModel.resetNjuptExamSyncState()
                showNjuptExamSyncSheet = true
            },
            onImportIcs = {
                showAddSheet = false
                fileManager.importFile(listOf("ics"))
            },
            onConfirm = { title, type, date, startTime, endTime, location, detail, teacher ->
                viewModel.addEvent(title, type, date, startTime, endTime, location, detail, teacher)
                addDraft = null
                showAddSheet = false
            },
            semesterStartDate = semesterStartDate,
            currentWeek = currentWeek
        )
    }

    editingEvent?.let { event ->
        AddScheduleEventBottomSheet(
            defaultDate = defaultDate,
            initialEvent = event,
            onDismiss = { editingEvent = null },
            onOpenNjuptSync = {},
            onImportIcs = {},
            onConfirm = { _, _, _, _, _, _, _, _ -> },
            onUpdate = { updated ->
                viewModel.updateEvent(updated)
                editingEvent = null
            },
            onDelete = { target ->
                viewModel.deleteEvent(target)
                editingEvent = null
            },
            semesterStartDate = semesterStartDate,
            currentWeek = currentWeek
        )
    }

    if (showNjuptExamSyncSheet) {
        NjuptExamSyncBottomSheet(
            state = examSyncState,
            savedClassCode = savedClassCode,
            onQuery = viewModel::syncNjuptExams,
            onImport = { exams ->
                viewModel.batchAddEvents(exams.map { it.toScheduleEvent() })
                ToastManager.show("成功导入 ${exams.size} 门考试")
                viewModel.resetNjuptExamSyncState()
                showNjuptExamSyncSheet = false
            },
            onFallbackToIcs = {
                viewModel.resetNjuptExamSyncState()
                showNjuptExamSyncSheet = false
                fileManager.importFile(listOf("ics"))
            },
            onDismiss = {
                viewModel.resetNjuptExamSyncState()
                showNjuptExamSyncSheet = false
            }
        )
    }

    icsPreviewExams?.let { exams ->
        IcsExamPreviewBottomSheet(
            exams = exams,
            onDismiss = { icsPreviewExams = null },
            onImport = { selectedExams ->
                viewModel.batchAddEvents(selectedExams.map { it.toScheduleEvent() })
                ToastManager.show("成功导入 ${selectedExams.size} 项日程")
                icsPreviewExams = null
            }
        )
    }
}

private sealed interface TodayTimelineEntry {
    val startTime: String?
    val endTime: String?

    data class CourseEntry(val model: CourseDisplayModel) : TodayTimelineEntry {
        override val startTime: String? get() = model.startTime
        override val endTime: String? get() = model.endTime
    }

    data class EventEntry(val event: ScheduleEvent) : TodayTimelineEntry {
        override val startTime: String? get() = event.startTime
        override val endTime: String? get() = event.endTime
    }
}

@Composable
fun TodayContent(
    state: TodayUiState.Success,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean,
    onToggleEvent: (ScheduleEvent) -> Unit,
    onDeleteEvent: (ScheduleEvent) -> Unit,
    onEditEvent: (ScheduleEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTime = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }

    val todayIso = state.today.toString()
    val futureExams = remember(state.upcomingExams, todayIso) {
        state.upcomingExams.filter { it.type == "EXAM" && it.date > todayIso }
    }
    val futureMakeups = remember(state.upcomingMakeups) {
        state.upcomingMakeups.filter { it.type == "MAKEUP" }
    }
    val futureTodos = remember(state.upcomingTodos, todayIso) {
        state.upcomingTodos.filter {
            it.type == "DAILY" && it.date > todayIso && !it.isCompleted
        }
    }
    val allDayEvents = remember(state.events) {
        state.events.filter { it.startTime.isNullOrBlank() }
    }
    val timelineEntries = remember(state.courses, state.events) {
        buildList<TodayTimelineEntry> {
            state.courses.forEach { add(TodayTimelineEntry.CourseEntry(it)) }
            state.events.forEach { event ->
                if (!event.startTime.isNullOrBlank()) {
                    add(TodayTimelineEntry.EventEntry(event))
                }
            }
        }.sortedWith(
            compareBy<TodayTimelineEntry> { it.startTime ?: TIMELINE_TIME_SORT_KEY }
                .thenBy { it.endTime ?: TIMELINE_TIME_SORT_KEY }
        )
    }

    val targetTimelineIndex = remember(timelineEntries, currentTime) {
        val firstActiveIndex = timelineEntries.indexOfFirst { entry ->
            try {
                LocalTime.parse(entry.endTime ?: DEFAULT_TIME_ZERO) >= currentTime
            } catch (e: Exception) {
                true
            }
        }

        if (firstActiveIndex == -1) {
            (timelineEntries.size - 1).coerceAtLeast(0)
        } else {
            firstActiveIndex
        }
    }

    val initialFirstVisibleItemIndex = remember(
        targetTimelineIndex,
        timelineEntries.size,
        futureExams.size,
        futureMakeups.size,
        futureTodos.size,
        allDayEvents.size
    ) {
        if (timelineEntries.isEmpty()) {
            0
        } else {
            val examItemCount = if (futureExams.isNotEmpty()) 1 else 0
            val makeupItemCount = if (futureMakeups.isNotEmpty()) 1 else 0
            val todoItemCount = if (futureTodos.isNotEmpty()) 1 else 0
            val allDayItemCount = if (allDayEvents.isNotEmpty()) 1 else 0
            val headerCount = 1
            examItemCount + makeupItemCount + todoItemCount + allDayItemCount +
                headerCount + targetTimelineIndex
        }
    }

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val weekDays = stringArrayResource(Res.array.week_days_full_names)

        val year = state.today.year.toString()
        val month = state.today.month.number.toString()
        val day = state.today.day.toString()

        val formattedDate = stringResource(Res.string.date_format_year_month_day, year, month, day)

        val dateStr = remember(state.today, weekDays, formattedDate) {
            val weekDayName = weekDays.getOrNull(state.today.dayOfWeek.isoDayNumber - 1).orEmpty()
            "$formattedDate $weekDayName"
        }

        val subTitle = when (state.status) {
            TodayStatus.NoSemesterConfig -> stringResource(Res.string.title_semester_not_set)

            TodayStatus.Vacation -> {
                val days = if (state.startDate != null) {
                    state.today.daysUntil(state.startDate).toString()
                } else DEFAULT_DAYS_ZERO
                stringResource(Res.string.title_vacation_until_start, days)
            }

            TodayStatus.SemesterEnded -> {
                val overdueDays = if (state.startDate != null) {
                    val targetDayOfWeek = DayOfWeek(state.firstDayOfWeek)
                    val daysShift = (state.startDate.dayOfWeek.ordinal - targetDayOfWeek.ordinal + 7) % 7
                    val firstWeekStart = LocalDate.fromEpochDays(state.startDate.toEpochDays() - daysShift)

                    val semesterEndDate = LocalDate.fromEpochDays(
                        firstWeekStart.toEpochDays() + (state.totalWeeks * 7) - 1
                    )
                    semesterEndDate.daysUntil(state.today).coerceAtLeast(DEFAULT_OVERDUE_DAYS)
                } else {
                    DEFAULT_OVERDUE_DAYS
                }
                stringResource(Res.string.status_semester_ended, overdueDays)
            }

            TodayStatus.Normal -> stringResource(Res.string.title_current_week, state.weekIndex.toString())
        }

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = dateStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }

        Spacer(modifier = Modifier.height(8.dp))

        val hasContent = futureExams.isNotEmpty() ||
            futureMakeups.isNotEmpty() ||
            futureTodos.isNotEmpty() ||
            allDayEvents.isNotEmpty() ||
            timelineEntries.isNotEmpty()

        if (!hasContent) {
            EmptyStateView()
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (futureExams.isNotEmpty()) {
                    item {
                        UpcomingExamsSection(
                            today = state.today,
                            exams = futureExams,
                            onEdit = onEditEvent,
                            onDelete = onDeleteEvent
                        )
                    }
                }

                if (futureMakeups.isNotEmpty()) {
                    item {
                        UpcomingMakeupsSection(
                            today = state.today,
                            makeups = futureMakeups,
                            onEdit = onEditEvent,
                            onDelete = onDeleteEvent
                        )
                    }
                }

                if (futureTodos.isNotEmpty()) {
                    item {
                        UpcomingTodosSection(
                            today = state.today,
                            todos = futureTodos,
                            onToggle = onToggleEvent,
                            onEdit = onEditEvent,
                            onDelete = onDeleteEvent
                        )
                    }
                }

                if (allDayEvents.isNotEmpty()) {
                    item {
                        AllDayEventsSection(
                            events = allDayEvents,
                            onToggle = onToggleEvent,
                            onEdit = onEditEvent
                        )
                    }
                }

                if (timelineEntries.isNotEmpty()) {
                    item {
                        Text(
                            text = "今日时间线",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    itemsIndexed(timelineEntries) { _, entry ->
                        when (entry) {
                            is TodayTimelineEntry.CourseEntry -> {
                                CourseTimelineItem(entry.model, gridStyle, isDark)
                            }

                            is TodayTimelineEntry.EventEntry -> {
                                ScheduleEventTimelineItem(
                                    event = entry.event,
                                    onToggle = { onToggleEvent(entry.event) },
                                    onEdit = { onEditEvent(entry.event) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun AllDayEventsSection(
    events: List<ScheduleEvent>,
    onToggle: (ScheduleEvent) -> Unit,
    onEdit: (ScheduleEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "全天事项",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        events.forEach { event ->
            ScheduleEventItem(
                event = event,
                onToggle = { onToggle(event) },
                onEdit = { onEdit(event) }
            )
        }
    }
}

@Composable
fun CourseTimelineItem(
    model: CourseDisplayModel,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean
) {
    val currentTime = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }
    val isFinished = remember(model.endTime, currentTime) {
        try {
            LocalTime.parse(model.endTime ?: DEFAULT_TIME_ZERO) < currentTime
        } catch (e: Exception) { false }
    }

    val colorPair = gridStyle.courseColorMaps.getOrElse(model.course.colorInt) {
        ScheduleGridStyle.DEFAULT_COLOR_MAPS[0]
    }
    val themeColor = if (isDark) colorPair.dark else colorPair.light
    val cardAlpha = gridStyle.courseBlockAlphaFloat.coerceIn(0f, 1f)
    val cardColor = themeColor.copy(alpha = cardAlpha)
    val textColors = rememberCourseTextColors(
        background = cardColor.compositeOver(MaterialTheme.colorScheme.surface),
        mode = gridStyle.courseTextColorMode,
        globalColor = gridStyle.courseTextColorLong?.let { Color(it) },
        tintStrength = gridStyle.courseTextTintStrength,
        contrastTarget = gridStyle.courseTextContrastRatio,
        sourceColor = themeColor
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (isFinished) 0.5f else 1f)
    ) {
        Column(
            modifier = Modifier.width(65.dp).padding(top = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = model.startTime ?: EMPTY_TIME_PLACEHOLDER,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    textDecoration = if (isFinished) TextDecoration.LineThrough else null
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = model.endTime ?: EMPTY_TIME_PLACEHOLDER,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val cardShape = MaterialTheme.shapes.medium
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(color = cardColor, shape = cardShape)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = model.course.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (isFinished) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.Bold,
                        color = textColors.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (model.course.position.isNotBlank()) {
                        Text(
                            text = stringResource(Res.string.course_position_prefix, model.course.position),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColors.secondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (model.course.teacher.isNotBlank()) {
                        Text(
                            text = stringResource(Res.string.course_teacher_prefix, model.course.teacher),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColors.secondary
                        )
                    }

                    val credit = model.course.credit
                    if (credit != null && credit > 0f) {
                        val creditValStr = if (credit % 1f == 0f) {
                            credit.toInt().toString()
                        } else {
                            credit.toString()
                        }
                        val creditLabel = stringResource(Res.string.label_credit)
                        Text(
                            text = "$creditLabel: $creditValStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColors.secondary
                        )
                    }
                }
            }
            model.course.remark?.takeIf { it.isNotBlank() }?.let { remark ->
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, start = 4.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.label_remark),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = remark,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.text_no_courses_today),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun UpcomingTodosSection(
    today: LocalDate,
    todos: List<ScheduleEvent>,
    onToggle: (ScheduleEvent) -> Unit,
    onEdit: (ScheduleEvent) -> Unit,
    onDelete: (ScheduleEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "近期待办",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        todos.forEach { todo ->
            val days = try {
                today.daysUntil(LocalDate.parse(todo.date))
            } catch (e: Exception) { 0 }
            val accent = eventAccentColor("DAILY")

            EventCardShell(
                onClick = { onEdit(todo) }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompactCheckbox(
                            checked = todo.isCompleted,
                            onCheckedChange = { onToggle(todo) }
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = todo.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = todo.shortDateMeta(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                CountdownChip(countdownLabel(days), accent)
                Spacer(modifier = Modifier.width(2.dp))
                DeleteAction { onDelete(todo) }
            }
        }
    }
}

private fun formatShortDate(dateIso: String): String = runCatching {
    val date = LocalDate.parse(dateIso)
    val month = date.month.number.toString().padStart(2, '0')
    val day = date.day.toString().padStart(2, '0')
    "$month.$day"
}.getOrDefault(dateIso)

@Composable
private fun UpcomingMakeupsSection(
    today: LocalDate,
    makeups: List<ScheduleEvent>,
    onEdit: (ScheduleEvent) -> Unit,
    onDelete: (ScheduleEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "待上补课",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        makeups.forEach { makeup ->
            val days = try {
                today.daysUntil(LocalDate.parse(makeup.date))
            } catch (e: Exception) { 0 }
            val accent = eventAccentColor("MAKEUP")

            EventCardShell(
                onClick = { onEdit(makeup) }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = makeup.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = makeup.shortDateMeta(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                CountdownChip(countdownLabel(days), accent)
                Spacer(modifier = Modifier.width(2.dp))
                DeleteAction { onDelete(makeup) }
            }
        }
    }
}

@Composable
fun UpcomingExamsSection(
    today: LocalDate,
    exams: List<ScheduleEvent>,
    onEdit: (ScheduleEvent) -> Unit,
    onDelete: (ScheduleEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "考试倒计时",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        exams.forEach { exam ->
            val days = try {
                today.daysUntil(LocalDate.parse(exam.date))
            } catch (e: Exception) { 0 }
            val accent = eventAccentColor("EXAM")

            EventCardShell(
                onClick = { onEdit(exam) }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = exam.shortDateMeta(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                CountdownChip(countdownLabel(days), accent)
                Spacer(modifier = Modifier.width(2.dp))
                DeleteAction { onDelete(exam) }
            }
        }
    }
}

@Composable
fun ScheduleEventItem(
    event: ScheduleEvent,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val isDaily = event.type == "DAILY"
    val meta = event.shortDateMeta()

    EventCardShell(
        onClick = onEdit,
        modifier = Modifier.graphicsLayer(alpha = if (event.isCompleted) 0.5f else 1f)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDaily) {
                    CompactCheckbox(
                        checked = event.isCompleted,
                        onCheckedChange = onToggle
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                    ),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (meta.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScheduleEventTimelineItem(
    event: ScheduleEvent,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val isDaily = event.type == "DAILY"
    val meta = listOfNotNull(
        event.location?.takeIf { it.isNotBlank() },
        event.teacher?.takeIf { it.isNotBlank() }?.let { "教师：$it" },
        event.detail?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (event.isCompleted) 0.5f else 1f),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(65.dp).padding(top = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = event.startTime ?: EMPTY_TIME_PLACEHOLDER,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!event.endTime.isNullOrBlank()) {
                Text(
                    text = event.endTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        EventCardShell(
            onClick = onEdit,
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDaily) {
                        CompactCheckbox(
                            checked = event.isCompleted,
                            onCheckedChange = onToggle
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (meta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private enum class EventTimeField { START, END }

private fun formatTimeOfDay(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private fun parseTimeOfDay(value: String): Pair<Int, Int>? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

/**
 * 将考试导入数据转换为日程事件；课表 ID 与主键由 ViewModel 批量写入时补全。
 */
fun ExamImportItem.toScheduleEvent(courseTableId: String = ""): ScheduleEvent = ScheduleEvent(
    id = "",
    courseTableId = courseTableId,
    title = courseName,
    type = "EXAM",
    date = date,
    startTime = startTime,
    endTime = endTime,
    location = listOfNotNull(campus, location)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { null },
    detail = note?.takeIf { it.isNotBlank() },
    teacher = teacher?.takeIf { it.isNotBlank() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleEventBottomSheet(
    defaultDate: String,
    initialEvent: ScheduleEvent? = null,
    draft: DraftState? = null,
    onDraftChange: (DraftState) -> Unit = {},
    onDismiss: () -> Unit,
    onOpenNjuptSync: () -> Unit,
    onImportIcs: () -> Unit,
    onConfirm: (title: String, type: String, date: String, startTime: String?, endTime: String?, location: String?, detail: String?, teacher: String?) -> Unit,
    onUpdate: (ScheduleEvent) -> Unit = {},
    onDelete: (ScheduleEvent) -> Unit = {},
    semesterStartDate: LocalDate? = null,
    currentWeek: Int? = null
) {
    val isEditing = initialEvent != null
    val initialDate = remember(defaultDate, initialEvent) {
        runCatching {
            LocalDate.parse(initialEvent?.date ?: defaultDate)
        }.getOrElse {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
    }
    var workingDraft by remember(initialEvent) {
        mutableStateOf(
            initialEvent?.toDraftState()
                ?: draft
                ?: DraftState(date = initialDate.toString())
        )
    }
    val selectedDate = remember(workingDraft.date) {
        runCatching { LocalDate.parse(workingDraft.date) }.getOrElse { initialDate }
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<EventTimeField?>(null) }
    val smartSource = workingDraft.smartInput.ifBlank { workingDraft.title }
    val smartResult = remember(smartSource, semesterStartDate, currentWeek) {
        smartSource.takeIf { it.isNotBlank() }?.let {
            SmartScheduleParser.parse(
                it,
                semesterStartDate = semesterStartDate,
                currentWeek = currentWeek
            )
        }
    }

    fun updateDraft(transform: (DraftState) -> DraftState) {
        val updated = transform(workingDraft)
        workingDraft = updated
        if (!isEditing) onDraftChange(updated)
    }

    val weekDays = stringArrayResource(Res.array.week_days_full_names)
    val formattedDate = stringResource(
        Res.string.date_format_year_month_day,
        selectedDate.year.toString(),
        selectedDate.month.number.toString(),
        selectedDate.day.toString()
    )
    val weekDayName = weekDays.getOrNull(selectedDate.dayOfWeek.isoDayNumber - 1).orEmpty()
    val dateDisplay = "$formattedDate $weekDayName"

    val timeRangeInvalid = !workingDraft.isAllDay &&
        workingDraft.startTime.isNotBlank() &&
        workingDraft.endTime.isNotBlank() &&
        workingDraft.endTime <= workingDraft.startTime

    val typeOptions = remember {
        listOf(
            "DAILY" to "每日日程",
            "EXAM" to "考试日程",
            "MAKEUP" to "自定义补课"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "编辑事项" else "添加事项",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!isEditing) {
                    TextButton(
                        onClick = {
                            updateDraft { DraftState(date = initialDate.toString()) }
                        },
                        enabled = workingDraft.hasInput
                    ) {
                        Text("清空重填")
                    }
                }
            }

            OutlinedTextField(
                value = workingDraft.smartInput,
                onValueChange = { value -> updateDraft { it.copy(smartInput = value) } },
                placeholder = { Text("试试：明天下午五点去教三 206 上课") },
                leadingIcon = {
                    Text(text = "✨", fontSize = 20.sp)
                },
                trailingIcon = {
                    if (workingDraft.smartInput.isNotEmpty()) {
                        IconButton(onClick = { updateDraft { it.copy(smartInput = "") } }) {
                            Icon(
                                painter = painterResource(Res.drawable.close_24px),
                                contentDescription = "清除智能输入",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            smartResult?.takeIf { result ->
                result.hasExplicitDate || result.startTime != null || result.location != null
            }?.let { result ->
                SmartScheduleSuggestionCard(
                    result = result,
                    onApply = {
                        val parsedStart = result.startTime
                        updateDraft { current ->
                            // 用户已手动切换到“自定义补课”时，解析出的普通日程不得覆盖其选择。
                            val nextType = if (
                                current.type == SmartScheduleParser.TYPE_MAKEUP &&
                                result.type == SmartScheduleParser.TYPE_DAILY
                            ) {
                                current.type
                            } else {
                                result.type
                            }
                            current.copy(
                                type = nextType,
                                title = result.title,
                                date = result.date.toString(),
                                isAllDay = parsedStart.isNullOrBlank(),
                                startTime = parsedStart.orEmpty(),
                                endTime = if (parsedStart.isNullOrBlank()) "" else result.endTime.orEmpty(),
                                location = result.location?.takeIf { it.isNotBlank() } ?: current.location,
                                teacher = result.teacher?.takeIf { it.isNotBlank() } ?: current.teacher,
                                smartInput = ""
                            )
                        }
                    }
                )
            }

            if (!isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickImportCard(
                        emoji = "⚡",
                        title = "同步南邮考试",
                        subtitle = "StuDev 班级在线",
                        onClick = onOpenNjuptSync,
                        modifier = Modifier.weight(1f)
                    )
                    QuickImportCard(
                        emoji = "📄",
                        title = "导入 .ics 文件",
                        subtitle = "离线日历兜底",
                        onClick = onImportIcs,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeOptions.forEach { (type, label) ->
                    FilterChip(
                        selected = workingDraft.type == type,
                        onClick = { updateDraft { it.copy(type = type) } },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = workingDraft.title,
                onValueChange = { value -> updateDraft { it.copy(title = value) } },
                label = {
                    Text(
                        when (workingDraft.type) {
                            "EXAM" -> "考试科目（如：高等数学期末）"
                            "MAKEUP" -> "补课名称（如：物理实验）"
                            else -> "日程标题（如：交作业/班会）"
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedCard(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.calendar_today_24px),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "日期",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateDisplay,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "选择",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "全天事项",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "开启后不设置具体时间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = workingDraft.isAllDay,
                    onCheckedChange = { checked -> updateDraft { it.copy(isAllDay = checked) } }
                )
            }

            if (!workingDraft.isAllDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeFieldCard(
                        label = "开始时间",
                        value = workingDraft.startTime,
                        onClick = { timePickerTarget = EventTimeField.START },
                        onClear = { updateDraft { it.copy(startTime = "") } },
                        modifier = Modifier.weight(1f)
                    )
                    TimeFieldCard(
                        label = "结束时间",
                        value = workingDraft.endTime,
                        onClick = { timePickerTarget = EventTimeField.END },
                        onClear = { updateDraft { it.copy(endTime = "") } },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (timeRangeInvalid) {
                    Text(
                        text = "结束时间需晚于开始时间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            OutlinedTextField(
                value = workingDraft.location,
                onValueChange = { value -> updateDraft { it.copy(location = value) } },
                label = { Text(if (workingDraft.type == "EXAM") "考场（如：主楼A101）" else "地点/教室") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (workingDraft.type == "MAKEUP") {
                OutlinedTextField(
                    value = workingDraft.teacher,
                    onValueChange = { value -> updateDraft { it.copy(teacher = value) } },
                    label = { Text("任课教师（如：张老师）") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = workingDraft.detail,
                onValueChange = { value -> updateDraft { it.copy(detail = value) } },
                label = { Text(if (workingDraft.type == "EXAM") "座位号 / 注意事项" else "备注 / 详情") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val formStartTime = if (workingDraft.isAllDay) {
                        null
                    } else {
                        workingDraft.startTime.takeIf { it.isNotBlank() }
                    }
                    val formEndTime = if (workingDraft.isAllDay) {
                        null
                    } else {
                        workingDraft.endTime.takeIf { it.isNotBlank() }
                    }
                    val formLocation = workingDraft.location.takeIf { it.isNotBlank() }
                    val formDetail = workingDraft.detail.takeIf { it.isNotBlank() }
                    val formTeacher = workingDraft.teacher.takeIf { it.isNotBlank() }

                    val base = initialEvent
                    if (base != null) {
                        onUpdate(
                            base.copy(
                                title = workingDraft.title.trim(),
                                type = workingDraft.type,
                                date = selectedDate.toString(),
                                startTime = formStartTime,
                                endTime = formEndTime,
                                location = formLocation,
                                detail = formDetail,
                                teacher = formTeacher
                            )
                        )
                    } else {
                        onConfirm(
                            workingDraft.title.trim(),
                            workingDraft.type,
                            selectedDate.toString(),
                            formStartTime,
                            formEndTime,
                            formLocation,
                            formDetail,
                            formTeacher
                        )
                    }
                },
                enabled = workingDraft.title.isNotBlank() && !timeRangeInvalid,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isEditing) "保存修改" else "保存事项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isEditing) {
                OutlinedButton(
                    onClick = { initialEvent?.let(onDelete) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "删除该事项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(selectedDate) {
                selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                            updateDraft { it.copy(date = pickedDate.toString()) }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    val pickerTarget = timePickerTarget
    if (pickerTarget != null) {
        val currentValue = when (pickerTarget) {
            EventTimeField.START -> workingDraft.startTime
            EventTimeField.END -> workingDraft.endTime
        }
        val (initialHour, initialMinute) = remember(pickerTarget, currentValue) {
            parseTimeOfDay(currentValue) ?: when (pickerTarget) {
                EventTimeField.START -> 8 to 0
                EventTimeField.END -> 9 to 0
            }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        TimePickerDialog(
            onDismissRequest = { timePickerTarget = null },
            title = {
                Text(
                    text = if (pickerTarget == EventTimeField.START) "选择开始时间" else "选择结束时间",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = formatTimeOfDay(timePickerState.hour, timePickerState.minute)
                        when (pickerTarget) {
                            EventTimeField.START -> updateDraft { it.copy(startTime = picked) }
                            EventTimeField.END -> updateDraft { it.copy(endTime = picked) }
                        }
                        timePickerTarget = null
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { timePickerTarget = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun SmartScheduleSuggestionCard(
    result: SmartScheduleParser.ParseResult,
    onApply: () -> Unit
) {
    val typeLabel = when (result.type) {
        SmartScheduleParser.TYPE_EXAM -> "考试"
        SmartScheduleParser.TYPE_MAKEUP -> "补课"
        else -> "日程"
    }
    val dateLabel = buildString {
        append(result.date.month.number.toString().padStart(2, '0'))
        append("月")
        append(result.date.day.toString().padStart(2, '0'))
        append("日")
    }
    val parsedStart = result.startTime
    val timeLabel = when {
        parsedStart.isNullOrBlank() -> "全天"
        result.endTime.isNullOrBlank() -> parsedStart
        else -> "$parsedStart-${result.endTime}"
    }
    val detailLine = buildString {
        append(dateLabel)
        append(' ')
        append(timeLabel)
        result.location?.takeIf { it.isNotBlank() }?.let { location ->
            append(" · ")
            append(location)
        }
        result.teacher?.takeIf { it.isNotBlank() }?.let { teacher ->
            append(" · ")
            append(teacher)
        }
        append(" · ")
        append(typeLabel)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "💡 识别结果: ${result.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onApply) {
                Text("一键填入")
            }
        }
    }
}

@Composable
private fun TimeFieldCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value.ifBlank { "未设置" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Medium,
                    color = if (value.isBlank()) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )
            }

            if (value.isNotBlank()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.close_24px),
                        contentDescription = "清除时间",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Icon(
                    painter = painterResource(Res.drawable.update_24px),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun QuickImportCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NjuptExamSyncBottomSheet(
    state: NjuptExamSyncUiState,
    savedClassCode: String,
    onQuery: (String) -> Unit,
    onImport: (List<ExamImportItem>) -> Unit,
    onFallbackToIcs: () -> Unit,
    onDismiss: () -> Unit
) {
    var classCode by remember(savedClassCode) {
        mutableStateOf(savedClassCode.ifBlank { TodayScheduleViewModel.DEFAULT_NJUPT_CLASS_CODE })
    }
    var selectedIndices by remember(state.exams) {
        mutableStateOf(state.exams.indices.toSet())
    }

    LaunchedEffect(savedClassCode) {
        if (classCode.isBlank()) {
            classCode = savedClassCode.ifBlank { TodayScheduleViewModel.DEFAULT_NJUPT_CLASS_CODE }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "⚡ 一键同步南邮考试",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "输入班级代码，从 StuDev 班级在线获取最新考试安排",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            OutlinedTextField(
                value = classCode,
                onValueChange = { newValue ->
                    classCode = newValue.uppercase().filterNot { it.isWhitespace() }
                },
                label = { Text("班级代码") },
                placeholder = { Text(TodayScheduleViewModel.DEFAULT_NJUPT_CLASS_CODE) },
                trailingIcon = {
                    if (classCode.isNotEmpty()) {
                        IconButton(onClick = { classCode = "" }) {
                            Icon(
                                painter = painterResource(Res.drawable.close_24px),
                                contentDescription = "清除班级代码",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                supportingText = { Text("忽略大小写与空格，输入后会自动转为大写") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (classCode.isNotBlank() && !state.isQuerying) onQuery(classCode)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onQuery(classCode) },
                enabled = classCode.isNotBlank() && !state.isQuerying,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (state.isQuerying) "查询中…" else "查询考试安排",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            when {
                state.isQuerying -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "正在获取考试安排…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                state.errorMessage != null -> {
                    ExamSyncErrorCard(
                        message = state.errorMessage,
                        onRetry = { onQuery(classCode) },
                        onFallbackToIcs = onFallbackToIcs
                    )
                }

                state.exams.isNotEmpty() -> {
                    ExamSelectionSection(
                        exams = state.exams,
                        selectedIndices = selectedIndices,
                        onSelectionChange = { selectedIndices = it },
                        title = "找到 ${state.exams.size} 门考试"
                    )

                    Button(
                        onClick = {
                            onImport(
                                state.exams.filterIndexed { index, _ -> index in selectedIndices }
                            )
                        },
                        enabled = selectedIndices.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "导入选中的 ${selectedIndices.size} 门考试",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                state.hasQueried -> {
                    ExamSyncErrorCard(
                        message = "未查询到该班级的考试安排",
                        onRetry = { onQuery(classCode) },
                        onFallbackToIcs = onFallbackToIcs
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcsExamPreviewBottomSheet(
    exams: List<ExamImportItem>,
    onDismiss: () -> Unit,
    onImport: (List<ExamImportItem>) -> Unit
) {
    var selectedIndices by remember(exams) {
        mutableStateOf(exams.indices.toSet())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "📄 导入 .ics 日历文件",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "已解析 ${exams.size} 项日程，确认后作为考试导入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            ExamSelectionSection(
                exams = exams,
                selectedIndices = selectedIndices,
                onSelectionChange = { selectedIndices = it },
                title = "已解析 ${exams.size} 项日程"
            )

            Button(
                onClick = {
                    onImport(exams.filterIndexed { index, _ -> index in selectedIndices })
                },
                enabled = selectedIndices.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "导入选中的 ${selectedIndices.size} 项日程",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ExamSelectionSection(
    exams: List<ExamImportItem>,
    selectedIndices: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit,
    title: String,
    modifier: Modifier = Modifier
) {
    val allSelected = exams.isNotEmpty() && selectedIndices.size == exams.size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "已选择 ${selectedIndices.size} 项",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            TextButton(
                onClick = {
                    onSelectionChange(if (allSelected) emptySet() else exams.indices.toSet())
                }
            ) {
                Text(if (allSelected) "取消全选" else "全选")
            }
        }

        exams.forEachIndexed { index, exam ->
            ExamImportCard(
                exam = exam,
                selected = index in selectedIndices,
                onToggle = { checked ->
                    onSelectionChange(
                        if (checked) selectedIndices + index else selectedIndices - index
                    )
                }
            )
        }
    }
}

@Composable
private fun ExamImportCard(
    exam: ExamImportItem,
    selected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    OutlinedCard(
        onClick = { onToggle(!selected) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = exam.courseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = exam.scheduleLine(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                exam.locationLine()?.let { locationText ->
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                exam.teacher?.takeIf { it.isNotBlank() }?.let { teacher ->
                    Text(
                        text = "教师：$teacher",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                exam.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamSyncErrorCard(
    message: String,
    onRetry: () -> Unit,
    onFallbackToIcs: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ $message",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetry) {
                    Text("重试")
                }
                TextButton(onClick = onFallbackToIcs) {
                    Text("改用 .ics 导入")
                }
            }
        }
    }
}

private fun ExamImportItem.scheduleLine(): String {
    val timeRange = when {
        startTime.isNullOrBlank() -> null
        endTime.isNullOrBlank() -> startTime
        else -> "$startTime - $endTime"
    }
    return listOfNotNull(date.takeIf { it.isNotBlank() }, timeRange).joinToString(" · ")
}

private fun ExamImportItem.locationLine(): String? =
    listOfNotNull(campus, location)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { null }