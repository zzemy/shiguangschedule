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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEvent
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import com.xingheyuzhuan.shiguangschedule.ui.theme.rememberCourseTextColors
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.add_24px
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
import shiguangschedule.shared.generated.resources.week_days_full_names
import kotlin.time.Clock

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
    val isDark = LocalIsDarkTheme.current
    var showAddDialog by remember { mutableStateOf(false) }

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
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    painter = painterResource(Res.drawable.add_24px),
                    contentDescription = "添加日程"
                )
            }
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

                    if (showAddDialog) {
                        AddScheduleEventDialog(
                            defaultDate = state.today.toString(),
                            onDismiss = { showAddDialog = false },
                            onConfirm = { title, type, date, startTime, endTime, location, detail ->
                                viewModel.addEvent(title, type, date, startTime, endTime, location, detail)
                                showAddDialog = false
                            }
                        )
                    }
                }
            }
        }
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

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = targetScrollIndex
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

        if (state.upcomingExams.isNotEmpty()) {
            UpcomingExamsSection(
                today = state.today,
                exams = state.upcomingExams,
                onDelete = onDeleteEvent
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.courses.isEmpty() && state.events.isEmpty()) {
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
                            exam.detail?.takeIf { it.isNotBlank() }?.let { "座位: $it" }
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

@Composable
fun AddScheduleEventDialog(
    defaultDate: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, type: String, date: String, startTime: String?, endTime: String?, location: String?, detail: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf("DAILY") }
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(defaultDate) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "添加事项",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "DAILY",
                        onClick = { selectedType = "DAILY" },
                        label = { Text("每日日程") }
                    )
                    FilterChip(
                        selected = selectedType == "EXAM",
                        onClick = { selectedType = "EXAM" },
                        label = { Text("考试日程") }
                    )
                    FilterChip(
                        selected = selectedType == "MAKEUP",
                        onClick = { selectedType = "MAKEUP" },
                        label = { Text("自定义补课") }
                    )
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
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("日期 (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("开始 (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("结束 (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(if (selectedType == "EXAM") "考场（如：主楼A101）" else "地点/教室") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text(if (selectedType == "EXAM") "座位号 / 注意事项" else "备注 / 详情") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, selectedType, date, startTime, endTime, location, detail)
                },
                enabled = title.isNotBlank() && date.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}