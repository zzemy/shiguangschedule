package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                    IconButton(onClick = { showAddSheet = true }) {
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
                        onDeleteEvent = { viewModel.deleteEvent(it) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddScheduleEventBottomSheet(
            defaultDate = defaultDate,
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
            onConfirm = { title, type, date, startTime, endTime, location, detail ->
                viewModel.addEvent(title, type, date, startTime, endTime, location, detail)
                showAddSheet = false
            }
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

@Composable
fun TodayContent(
    state: TodayUiState.Success,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean,
    onToggleEvent: (ScheduleEvent) -> Unit,
    onDeleteEvent: (ScheduleEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTime = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }

    val targetScrollIndex = remember(state.courses, currentTime) {
        val firstActiveIndex = state.courses.indexOfFirst { model ->
            try {
                LocalTime.parse(model.endTime ?: DEFAULT_TIME_ZERO) >= currentTime
            } catch (e: Exception) {
                true
            }
        }

        if (firstActiveIndex == -1) {
            (state.courses.size - 1).coerceAtLeast(0)
        } else {
            firstActiveIndex
        }
    }

    val initialFirstVisibleItemIndex = remember(
        targetScrollIndex,
        state.courses.size,
        state.events.size,
        state.upcomingExams.size
    ) {
        if (state.courses.isEmpty()) {
            0
        } else {
            val examItemCount = if (state.upcomingExams.isNotEmpty()) 1 else 0
            val eventItemCount = if (state.events.isNotEmpty()) 1 + state.events.size else 0
            val courseHeaderCount = if (state.events.isNotEmpty()) 1 else 0
            examItemCount + eventItemCount + courseHeaderCount + targetScrollIndex
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

        val hasContent = state.upcomingExams.isNotEmpty() ||
            state.events.isNotEmpty() ||
            state.courses.isNotEmpty()

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
                if (state.upcomingExams.isNotEmpty()) {
                    item {
                        UpcomingExamsSection(
                            today = state.today,
                            exams = state.upcomingExams,
                            onDelete = onDeleteEvent
                        )
                    }
                }

                if (state.events.isNotEmpty()) {
                    item {
                        Text(
                            text = "📌 今日日程与待办",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    itemsIndexed(state.events) { _, event ->
                        ScheduleEventItem(
                            event = event,
                            onToggle = { onToggleEvent(event) },
                            onDelete = { onDeleteEvent(event) }
                        )
                    }
                }

                if (state.courses.isNotEmpty()) {
                    if (state.events.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📚 今日课程",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    itemsIndexed(state.courses) { _, model ->
                        CourseTimelineItem(model, gridStyle, isDark)
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
fun UpcomingExamsSection(
    today: LocalDate,
    exams: List<ScheduleEvent>,
    onDelete: (ScheduleEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "📅 考试倒计时",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        exams.forEach { exam ->
            val days = try {
                today.daysUntil(LocalDate.parse(exam.date))
            } catch (e: Exception) { 0 }

            val countdownText = when {
                days == 0 -> "今天考试"
                days > 0 -> "还有 $days 天"
                else -> "已结束"
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = exam.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        val timeStr = listOfNotNull(exam.startTime, exam.endTime?.let { "- $it" }).joinToString(" ")
                        val locStr = listOfNotNull(
                            exam.location?.takeIf { it.isNotBlank() },
                            exam.detail?.takeIf { it.isNotBlank() }
                        ).joinToString(" | ")

                        val subInfo = listOfNotNull(exam.date, timeStr.takeIf { it.isNotBlank() }, locStr.takeIf { it.isNotBlank() }).joinToString(" · ")
                        if (subInfo.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { onDelete(exam) }) {
                        Icon(
                            painter = painterResource(Res.drawable.delete_24px),
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleEventItem(
    event: ScheduleEvent,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isDaily = event.type == "DAILY"
    val isExam = event.type == "EXAM"

    val badgeLabel = when (event.type) {
        "EXAM" -> "考试"
        "MAKEUP" -> "补课"
        else -> "日程"
    }

    val containerColor = when (event.type) {
        "EXAM" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        "MAKEUP" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (event.isCompleted) 0.5f else 1f),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDaily) {
                Checkbox(
                    checked = event.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isExam) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val timeStr = if (!event.startTime.isNullOrBlank()) {
                    "${event.startTime}${if (!event.endTime.isNullOrBlank()) " - ${event.endTime}" else ""}"
                } else null

                val subInfo = listOfNotNull(
                    timeStr,
                    event.location?.takeIf { it.isNotBlank() },
                    event.detail?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")

                if (subInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(Res.drawable.delete_24px),
                    contentDescription = "删除事件",
                    tint = MaterialTheme.colorScheme.outline
                )
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
    detail = listOfNotNull(teacher?.let { "教师: $it" }, note)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { null }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleEventBottomSheet(
    defaultDate: String,
    onDismiss: () -> Unit,
    onOpenNjuptSync: () -> Unit,
    onImportIcs: () -> Unit,
    onConfirm: (title: String, type: String, date: String, startTime: String?, endTime: String?, location: String?, detail: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf("DAILY") }
    var title by remember { mutableStateOf("") }
    val initialDate = remember(defaultDate) {
        runCatching { LocalDate.parse(defaultDate) }.getOrElse {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
    }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var isAllDay by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<EventTimeField?>(null) }

    val weekDays = stringArrayResource(Res.array.week_days_full_names)
    val formattedDate = stringResource(
        Res.string.date_format_year_month_day,
        selectedDate.year.toString(),
        selectedDate.month.number.toString(),
        selectedDate.day.toString()
    )
    val weekDayName = weekDays.getOrNull(selectedDate.dayOfWeek.isoDayNumber - 1).orEmpty()
    val dateDisplay = "$formattedDate $weekDayName"

    val timeRangeInvalid = !isAllDay &&
        startTime.isNotBlank() &&
        endTime.isNotBlank() &&
        endTime <= startTime

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
            Text(
                text = "添加事项",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeOptions.forEach { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
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
                value = title,
                onValueChange = { title = it },
                label = {
                    Text(
                        when (selectedType) {
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
                    checked = isAllDay,
                    onCheckedChange = { isAllDay = it }
                )
            }

            if (!isAllDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeFieldCard(
                        label = "开始时间",
                        value = startTime,
                        onClick = { timePickerTarget = EventTimeField.START },
                        onClear = { startTime = "" },
                        modifier = Modifier.weight(1f)
                    )
                    TimeFieldCard(
                        label = "结束时间",
                        value = endTime,
                        onClick = { timePickerTarget = EventTimeField.END },
                        onClear = { endTime = "" },
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
                value = location,
                onValueChange = { location = it },
                label = { Text(if (selectedType == "EXAM") "考场（如：主楼A101）" else "地点/教室") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it },
                label = { Text(if (selectedType == "EXAM") "座位号 / 注意事项" else "备注 / 详情") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onConfirm(
                        title.trim(),
                        selectedType,
                        selectedDate.toString(),
                        if (isAllDay) null else startTime.takeIf { it.isNotBlank() },
                        if (isAllDay) null else endTime.takeIf { it.isNotBlank() },
                        location.takeIf { it.isNotBlank() },
                        detail.takeIf { it.isNotBlank() }
                    )
                },
                enabled = title.isNotBlank() && !timeRangeInvalid,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "保存事项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
                            selectedDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
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
            EventTimeField.START -> startTime
            EventTimeField.END -> endTime
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
                            EventTimeField.START -> startTime = picked
                            EventTimeField.END -> endTime = picked
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