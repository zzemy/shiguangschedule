package com.xingheyuzhuan.shiguangschedule.widget.agenda

import com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEvent
import com.xingheyuzhuan.shiguangschedule.data.model.WidgetStyle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 日程小组件使用的数据分类。
 * SCHEDULE 对应主数据库中的 MAKEUP，仅作为日程展示，不会写入课程小组件数据库。
 */
internal enum class AgendaItemKind {
    TODO,
    EXAM,
    SCHEDULE
}

/**
 * 日程小组件的单条数据。数据直接来自 ScheduleEvent，不经过 WidgetCourse 快照。
 */
internal data class AgendaItem(
    val id: String,
    val title: String,
    val kind: AgendaItemKind,
    val date: LocalDate,
    val startTime: String,
    val endTime: String,
    val location: String,
    val detail: String,
    val isCompleted: Boolean
) {
    val sortTime: String get() = startTime.ifBlank { "99:99" }

    fun daysLeft(today: LocalDate): Int = ChronoUnit.DAYS.between(today, date).toInt()
}

/**
 * 一次刷新的完整日程小组件数据快照。
 */
internal data class AgendaWidgetData(
    val today: LocalDate,
    val items: List<AgendaItem>,
    val widgetStyle: WidgetStyle
) {
    val nearestExam: AgendaItem?
        get() = items.asSequence()
            .filter { it.kind == AgendaItemKind.EXAM }
            .minWithOrNull(agendaComparator)

    val nearestPending: AgendaItem?
        get() = items.asSequence()
            .filter { it.kind != AgendaItemKind.EXAM }
            .filter { !(it.kind == AgendaItemKind.TODO && it.isCompleted) }
            .minWithOrNull(agendaComparator)

    val pendingTodoCount: Int
        get() = items.count { it.kind == AgendaItemKind.TODO && !it.isCompleted }

    val examCount: Int
        get() = items.count { it.kind == AgendaItemKind.EXAM }

    companion object {
        private val agendaComparator =
            compareBy<AgendaItem>({ it.date }, { it.sortTime }, { it.title })
    }
}

/**
 * 将主数据库的日程事件转换为日程小组件数据。
 * 未来已完成的待办不再展示；今天已完成的待办保留，以便时间线呈现完成状态。
 */
internal object AgendaWidgetDataFactory {

    private const val HORIZON_DAYS = 30L
    private const val MAX_ITEMS = 40

    fun create(
        events: List<ScheduleEvent>,
        today: LocalDate,
        widgetStyle: WidgetStyle
    ): AgendaWidgetData {
        val horizonEnd = today.plusDays(HORIZON_DAYS)

        val items = events.asSequence()
            .mapNotNull { it.toAgendaItem() }
            .filter { !it.date.isBefore(today) && !it.date.isAfter(horizonEnd) }
            .filter { !(it.kind == AgendaItemKind.TODO && it.isCompleted && it.date != today) }
            .sortedWith(compareBy({ it.date }, { it.sortTime }, { it.title }))
            .take(MAX_ITEMS)
            .toList()

        return AgendaWidgetData(
            today = today,
            items = items,
            widgetStyle = widgetStyle
        )
    }

    private fun ScheduleEvent.toAgendaItem(): AgendaItem? {
        val kind = when (type.uppercase()) {
            "DAILY" -> AgendaItemKind.TODO
            "EXAM" -> AgendaItemKind.EXAM
            "MAKEUP" -> AgendaItemKind.SCHEDULE
            else -> return null
        }

        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        val safeTitle = title.trim()
        if (safeTitle.isBlank()) return null

        return AgendaItem(
            id = id,
            title = safeTitle,
            kind = kind,
            date = parsedDate,
            startTime = startTime?.trim().orEmpty(),
            endTime = endTime?.trim().orEmpty(),
            location = location?.trim().orEmpty(),
            detail = detail?.trim().orEmpty(),
            isCompleted = isCompleted
        )
    }
}