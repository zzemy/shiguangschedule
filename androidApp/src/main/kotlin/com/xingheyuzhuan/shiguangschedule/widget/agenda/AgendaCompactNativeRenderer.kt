package com.xingheyuzhuan.shiguangschedule.widget.agenda

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 日程与考试临近日程小组件渲染器（2x2）。
 *
 * 最多展示三条按临近紧迫度统一排序的日程/考试/待办悬浮卡片，
 * 不再区分“最近考试”和“最近待办”两个区块。
 * 数据来自 AgendaWidgetData，不读取课程小组件数据库。
 */
internal object AgendaCompactNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")
    private const val MAX_VISIBLE_ITEMS = 3

    /** 考试倒计时：醒目红橙色 */
    private const val EXAM_COUNTDOWN_LIGHT = 0xFFD9432F.toInt()
    private const val EXAM_COUNTDOWN_DARK = 0xFFFF8A7A.toInt()

    private const val TODO_BADGE_TEXT_LIGHT = 0xFF5B6472.toInt()
    private const val TODO_BADGE_TEXT_DARK = 0xFFC2C7D0.toInt()
    private const val SCHEDULE_BADGE_TEXT_LIGHT = 0xFF2F6BC4.toInt()
    private const val SCHEDULE_BADGE_TEXT_DARK = 0xFF8FB4FF.toInt()

    private data class ContentColors(
        val backgroundColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int,
        val scheduleCountdownColor: Int
    )

    private data class RowBinding(
        val containerId: Int,
        val badgeTodoId: Int,
        val badgeExamId: Int,
        val badgeScheduleId: Int,
        val titleId: Int,
        val metaId: Int,
        val countdownId: Int
    ) {
        val badgeIds: IntArray
            get() = intArrayOf(badgeTodoId, badgeExamId, badgeScheduleId)

        fun badgeIdFor(kind: AgendaItemKind): Int = when (kind) {
            AgendaItemKind.TODO -> badgeTodoId
            AgendaItemKind.EXAM -> badgeExamId
            AgendaItemKind.SCHEDULE -> badgeScheduleId
        }
    }

    private val ROWS = arrayOf(
        RowBinding(
            containerId = R.id.container_row_1,
            badgeTodoId = R.id.tv_row_1_badge_todo,
            badgeExamId = R.id.tv_row_1_badge_exam,
            badgeScheduleId = R.id.tv_row_1_badge_schedule,
            titleId = R.id.tv_row_1_title,
            metaId = R.id.tv_row_1_meta,
            countdownId = R.id.tv_row_1_countdown
        ),
        RowBinding(
            containerId = R.id.container_row_2,
            badgeTodoId = R.id.tv_row_2_badge_todo,
            badgeExamId = R.id.tv_row_2_badge_exam,
            badgeScheduleId = R.id.tv_row_2_badge_schedule,
            titleId = R.id.tv_row_2_title,
            metaId = R.id.tv_row_2_meta,
            countdownId = R.id.tv_row_2_countdown
        ),
        RowBinding(
            containerId = R.id.container_row_3,
            badgeTodoId = R.id.tv_row_3_badge_todo,
            badgeExamId = R.id.tv_row_3_badge_exam,
            badgeScheduleId = R.id.tv_row_3_badge_schedule,
            titleId = R.id.tv_row_3_title,
            metaId = R.id.tv_row_3_meta,
            countdownId = R.id.tv_row_3_countdown
        )
    )

    private val AGENDA_COMPARATOR =
        compareBy<AgendaItem>({ it.date }, { it.sortTime }, { it.title })

    fun render(
        context: Context,
        data: AgendaWidgetData,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_agenda_compact_native)

        bindRootClick(context, rv)

        val style = data.widgetStyle
        val isDark = resolveWidgetIsDark(context, style.themeMode)
        val colors = resolveContentColors(isDark, style.seedColor)
        val alphaPercent = style.backgroundAlpha.coerceIn(0.0f, 1.0f)
        val scale = if (style.fontScale > 0f) style.fontScale else 1.0f

        val items = data.items.asSequence()
            .filterNot { it.kind == AgendaItemKind.TODO && it.isCompleted }
            .sortedWith(AGENDA_COMPARATOR)
            .take(MAX_VISIBLE_ITEMS)
            .toList()

        bindWidget(
            context = context,
            rv = rv,
            today = data.today,
            isDark = isDark,
            colors = colors,
            alphaPercent = alphaPercent,
            scale = scale,
            items = items
        )

        return rv
    }

    private fun bindWidget(
        context: Context,
        rv: RemoteViews,
        today: LocalDate,
        isDark: Boolean,
        colors: ContentColors,
        alphaPercent: Float,
        scale: Float,
        items: List<AgendaItem>
    ) {
        rv.setImageViewBackground(R.id.card_bg_image, colors.backgroundColor, alphaPercent)

        val isEmpty = items.isEmpty()
        rv.setViewVisibility(R.id.container_content, if (isEmpty) View.GONE else View.VISIBLE)
        rv.setViewVisibility(R.id.container_empty, if (isEmpty) View.VISIBLE else View.GONE)

        rv.setTextViewText(
            R.id.tv_empty_title,
            context.getString(R.string.widget_agenda_compact_empty)
        )
        rv.setTextColor(R.id.tv_empty_title, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.tv_empty_title, TypedValue.COMPLEX_UNIT_DIP, 12f * scale)

        if (isEmpty) return

        items.forEachIndexed { index, item ->
            bindRow(context, rv, ROWS[index], item, today, isDark, colors, scale)
        }

        for (index in items.size until MAX_VISIBLE_ITEMS) {
            rv.setViewVisibility(ROWS[index].containerId, View.GONE)
        }
    }

    private fun bindRow(
        context: Context,
        rv: RemoteViews,
        row: RowBinding,
        item: AgendaItem,
        today: LocalDate,
        isDark: Boolean,
        colors: ContentColors,
        scale: Float
    ) {
        rv.setViewVisibility(row.containerId, View.VISIBLE)

        val badgeId = row.badgeIdFor(item.kind)
        row.badgeIds.forEach { id ->
            rv.setViewVisibility(id, if (id == badgeId) View.VISIBLE else View.GONE)
        }
        rv.setTextColor(badgeId, badgeTextColor(item.kind, isDark))
        rv.setTextViewTextSize(badgeId, TypedValue.COMPLEX_UNIT_DIP, 8f * scale)

        rv.setTextViewText(row.titleId, item.title)
        rv.setTextColor(row.titleId, colors.primaryTextColor)
        rv.setTextViewTextSize(row.titleId, TypedValue.COMPLEX_UNIT_DIP, 13f * scale)

        rv.setTextViewText(row.metaId, buildMeta(context, item, today))
        rv.setTextColor(row.metaId, colors.secondaryTextColor)
        rv.setTextViewTextSize(row.metaId, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)

        val daysLeft = item.daysLeft(today)
        rv.setTextViewText(row.countdownId, countdownText(context, daysLeft))
        rv.setTextColor(row.countdownId, countdownTextColor(item.kind, isDark, colors))
        rv.setTextViewTextSize(
            row.countdownId,
            TypedValue.COMPLEX_UNIT_DIP,
            13.5f * scale
        )
    }

    private fun badgeTextColor(kind: AgendaItemKind, isDark: Boolean): Int = when (kind) {
        AgendaItemKind.TODO ->
            if (isDark) TODO_BADGE_TEXT_DARK else TODO_BADGE_TEXT_LIGHT
        AgendaItemKind.EXAM ->
            if (isDark) EXAM_COUNTDOWN_DARK else EXAM_COUNTDOWN_LIGHT
        AgendaItemKind.SCHEDULE ->
            if (isDark) SCHEDULE_BADGE_TEXT_DARK else SCHEDULE_BADGE_TEXT_LIGHT
    }

    private fun countdownTextColor(
        kind: AgendaItemKind,
        isDark: Boolean,
        colors: ContentColors
    ): Int = when (kind) {
        AgendaItemKind.TODO -> colors.primaryTextColor
        AgendaItemKind.EXAM ->
            if (isDark) EXAM_COUNTDOWN_DARK else EXAM_COUNTDOWN_LIGHT
        AgendaItemKind.SCHEDULE -> colors.scheduleCountdownColor
    }

    private fun countdownText(context: Context, daysLeft: Int): String = when {
        daysLeft <= 0 -> context.getString(R.string.widget_title_today)
        daysLeft == 1 -> context.getString(R.string.widget_title_tomorrow)
        else -> context.getString(R.string.widget_agenda_compact_days_left, daysLeft)
    }

    /** 日期/时间 · 地点 */
    private fun buildMeta(context: Context, entry: AgendaItem, today: LocalDate): String {
        val dateText = dateLabel(context, entry.date, today)
        val timeText = entry.startTime.take(5)
            .ifBlank { context.getString(R.string.widget_agenda_all_day) }
        return listOf("$dateText $timeText", entry.location)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }

    private fun dateLabel(context: Context, date: LocalDate, today: LocalDate): String = when (date) {
        today -> context.getString(R.string.widget_title_today)
        today.plusDays(1) -> context.getString(R.string.widget_title_tomorrow)
        else -> date.format(DATE_FORMATTER)
    }

    private fun bindRootClick(context: Context, rv: RemoteViews) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
    }

    private fun resolveContentColors(isDark: Boolean, seedColorLong: Long?): ContentColors {
        val scheme = if (seedColorLong != null) {
            createMaterialKolorScheme(darkTheme = isDark, seedColor = Color(seedColorLong))
        } else {
            createMaterialKolorScheme(darkTheme = isDark)
        }

        return if (seedColorLong == null) {
            ContentColors(
                backgroundColor = scheme.surfaceContainer.toArgb(),
                primaryTextColor = scheme.onSurface.toArgb(),
                secondaryTextColor = scheme.onSurfaceVariant.toArgb(),
                scheduleCountdownColor = scheme.primary.toArgb()
            )
        } else {
            val contentColor = scheme.onPrimaryContainer
            ContentColors(
                backgroundColor = scheme.primaryContainer.toArgb(),
                primaryTextColor = contentColor.toArgb(),
                secondaryTextColor = contentColor.copy(alpha = 0.72f).toArgb(),
                scheduleCountdownColor = scheme.primary.toArgb()
            )
        }
    }

    private fun RemoteViews.setImageViewBackground(
        bgViewId: Int,
        @ColorInt baseColor: Int,
        @FloatRange(from = 0.0, to = 1.0) alphaPercent: Float
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.setColorStateList(bgViewId, "setImageTintList", ColorStateList.valueOf(baseColor))
        } else {
            this.setInt(bgViewId, "setColorFilter", baseColor)
        }

        val alphaInt = (alphaPercent * 255).toInt().coerceIn(0, 255)
        this.setInt(bgViewId, "setImageAlpha", alphaInt)
    }
}