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
import androidx.core.widget.RemoteViewsCompat
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.WidgetStyle
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 日程与考试待办时间线小组件渲染器（4x2）。
 *
 * 数据来自 AgendaWidgetData（由 ScheduleEventRepository 构建），不读取 WidgetCourse 数据库。
 * layout/ 与 layout-night/ 必须保持同一套 View ID。
 */
internal object AgendaNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")
    private const val MAX_AGENDA_ITEMS = 40

    private enum class AgendaKind(val accent: AgendaAccent) {
        TODO(AgendaPalette.todo),
        EXAM(AgendaPalette.exam),
        SCHEDULE(AgendaPalette.schedule)
    }

    private val BADGE_VIEW_IDS = intArrayOf(
        R.id.tv_item_badge_todo,
        R.id.tv_item_badge_exam,
        R.id.tv_item_badge_schedule
    )

    private val COUNTDOWN_VIEW_IDS = intArrayOf(
        R.id.tv_item_countdown_todo,
        R.id.tv_item_countdown_exam,
        R.id.tv_item_countdown_schedule
    )

    private fun AgendaKind.badgeViewId(): Int = when (this) {
        AgendaKind.TODO -> R.id.tv_item_badge_todo
        AgendaKind.EXAM -> R.id.tv_item_badge_exam
        AgendaKind.SCHEDULE -> R.id.tv_item_badge_schedule
    }

    private fun AgendaKind.countdownViewId(): Int = when (this) {
        AgendaKind.TODO -> R.id.tv_item_countdown_todo
        AgendaKind.EXAM -> R.id.tv_item_countdown_exam
        AgendaKind.SCHEDULE -> R.id.tv_item_countdown_schedule
    }

    private fun AgendaItemKind.toAgendaKind(): AgendaKind = when (this) {
        AgendaItemKind.TODO -> AgendaKind.TODO
        AgendaItemKind.EXAM -> AgendaKind.EXAM
        AgendaItemKind.SCHEDULE -> AgendaKind.SCHEDULE
    }

    /** 仅显示选中的分类视图，其余同组视图隐藏（背景为静态 drawable，避免非法 setBackgroundResource）。 */
    private fun RemoteViews.selectKindView(selectedId: Int, candidateIds: IntArray) {
        candidateIds.forEach { id ->
            setViewVisibility(id, if (id == selectedId) View.VISIBLE else View.GONE)
        }
    }

    private data class ContentColors(
        val backgroundColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int
    )

    fun render(
        context: Context,
        data: AgendaWidgetData,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_agenda_native)

        resetWidgetState(rv)
        bindRootClick(context, rv)

        val style = data.widgetStyle
        val isDark = resolveWidgetIsDark(context, style.themeMode)
        val colors = resolveContentColors(isDark, style.seedColor)
        val alphaPercent = style.backgroundAlpha.coerceIn(0.0f, 1.0f)
        val scale = if (style.fontScale > 0f) style.fontScale else 1.0f

        applyWidgetStyle(rv, isDark, colors, alphaPercent, scale)
        renderHeader(context, rv, data.today)
        renderNearestExamCountdown(context, rv, data.nearestExam, data.today)

        val items = data.items.take(MAX_AGENDA_ITEMS)
        if (items.isEmpty()) {
            showEmptyState(rv)
        } else {
            renderAgendaList(context, rv, appWidgetId, items, data.today, isDark, colors, scale)
        }

        return rv
    }

    private fun resetWidgetState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.list_view_agenda, View.VISIBLE)
        rv.setViewVisibility(R.id.container_empty, View.GONE)
        rv.setViewVisibility(R.id.tv_exam_countdown, View.GONE)
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

    private fun renderHeader(context: Context, rv: RemoteViews, today: LocalDate) {
        val weekDays = context.resources.getStringArray(R.array.week_days_names)
        val weekDayText = weekDays.getOrElse(today.dayOfWeek.value - 1) { "" }
        val headerText = context.getString(
            R.string.widget_agenda_header_date,
            today.monthValue,
            today.dayOfMonth,
            weekDayText
        )
        rv.setTextViewText(R.id.tv_header_title, headerText)
    }

    private fun renderNearestExamCountdown(
        context: Context,
        rv: RemoteViews,
        exam: AgendaItem?,
        today: LocalDate
    ) {
        if (exam == null) return

        val daysLeft = exam.daysLeft(today)
        val text = if (daysLeft <= 0) {
            context.getString(R.string.widget_agenda_exam_today)
        } else {
            context.getString(R.string.widget_agenda_exam_countdown, daysLeft)
        }

        rv.setViewVisibility(R.id.tv_exam_countdown, View.VISIBLE)
        rv.setTextViewText(R.id.tv_exam_countdown, text)
    }

    private fun renderAgendaList(
        context: Context,
        rv: RemoteViews,
        appWidgetId: Int,
        items: List<AgendaItem>,
        today: LocalDate,
        isDark: Boolean,
        colors: ContentColors,
        scale: Float
    ) {
        rv.setViewVisibility(R.id.list_view_agenda, View.VISIBLE)
        rv.setViewVisibility(R.id.container_empty, View.GONE)

        val builder = RemoteViewsCompat.RemoteCollectionItems.Builder()

        items.forEachIndexed { index, item ->
            val itemRv = createAgendaItemView(context, today, item, isDark, colors, scale)

            itemRv.setOnClickFillInIntent(R.id.item_root_agenda, Intent().apply {
                putExtra("agenda_id", item.id)
            })

            val stableId = "${item.id}#$index".hashCode().toLong() and 0x7FFFFFFFL
            builder.addItem(stableId, itemRv)
        }

        builder.setHasStableIds(true)
        val collectionItems = builder.build()

        RemoteViewsCompat.setRemoteAdapter(
            context,
            rv,
            appWidgetId,
            R.id.list_view_agenda,
            collectionItems
        )

        val clickIntentTemplate = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentTemplate = PendingIntent.getActivity(
            context,
            appWidgetId,
            clickIntentTemplate,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        rv.setPendingIntentTemplate(R.id.list_view_agenda, pendingIntentTemplate)
    }

    private fun createAgendaItemView(
        context: Context,
        today: LocalDate,
        item: AgendaItem,
        isDark: Boolean,
        colors: ContentColors,
        scale: Float
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item_agenda_node)

        val kind = item.kind.toAgendaKind()
        val accentTextColor = kind.accent.textColor(isDark)
        val badgeViewId = kind.badgeViewId()
        val countdownViewId = kind.countdownViewId()

        rv.selectKindView(badgeViewId, BADGE_VIEW_IDS)
        rv.selectKindView(countdownViewId, COUNTDOWN_VIEW_IDS)

        val dateTag = when (item.date) {
            today -> context.getString(R.string.widget_title_today)
            today.plusDays(1) -> context.getString(R.string.widget_title_tomorrow)
            else -> item.date.format(DATE_FORMATTER)
        }
        val timeText = item.startTime.take(5)
            .ifBlank { context.getString(R.string.widget_agenda_all_day) }
        val locationText = listOf(item.location, item.detail)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val daysLeft = item.daysLeft(today)
        val countdownText = when {
            item.kind == AgendaItemKind.TODO && item.isCompleted ->
                context.getString(R.string.widget_agenda_completed)
            daysLeft <= 0 -> context.getString(R.string.widget_title_today)
            else -> context.getString(R.string.widget_agenda_days_left, daysLeft)
        }

        rv.setTextViewText(R.id.tv_item_date_tag, dateTag)
        rv.setTextColor(R.id.tv_item_date_tag, colors.secondaryTextColor)
        rv.setTextViewTextSize(R.id.tv_item_date_tag, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)

        rv.setTextViewText(R.id.tv_item_time, timeText)
        rv.setTextColor(R.id.tv_item_time, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.tv_item_time, TypedValue.COMPLEX_UNIT_DIP, 11.5f * scale)

        rv.setTextColor(badgeViewId, accentTextColor)
        rv.setTextViewTextSize(badgeViewId, TypedValue.COMPLEX_UNIT_DIP, 9f * scale)

        rv.setTextViewText(R.id.tv_item_title, item.title)
        rv.setTextColor(
            R.id.tv_item_title,
            if (item.kind == AgendaItemKind.TODO && item.isCompleted) {
                colors.secondaryTextColor
            } else {
                colors.primaryTextColor
            }
        )
        rv.setTextViewTextSize(R.id.tv_item_title, TypedValue.COMPLEX_UNIT_DIP, 13.5f * scale)

        rv.setTextViewText(countdownViewId, countdownText)
        rv.setTextColor(countdownViewId, accentTextColor)
        rv.setTextViewTextSize(countdownViewId, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)

        if (locationText.isBlank()) {
            rv.setViewVisibility(R.id.tv_item_location, View.GONE)
        } else {
            rv.setViewVisibility(R.id.tv_item_location, View.VISIBLE)
            rv.setTextViewText(R.id.tv_item_location, locationText)
            rv.setTextColor(R.id.tv_item_location, colors.secondaryTextColor)
            rv.setTextViewTextSize(R.id.tv_item_location, TypedValue.COMPLEX_UNIT_DIP, 11f * scale)
        }

        return rv
    }

    private fun showEmptyState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.list_view_agenda, View.GONE)
        rv.setViewVisibility(R.id.container_empty, View.VISIBLE)
        rv.setViewVisibility(R.id.tv_exam_countdown, View.GONE)
    }

    private fun applyWidgetStyle(
        rv: RemoteViews,
        isDark: Boolean,
        colors: ContentColors,
        alphaPercent: Float,
        scale: Float
    ) {
        rv.setImageViewBackground(R.id.card_bg_image, colors.backgroundColor, alphaPercent)

        rv.setTextColor(R.id.tv_header_title, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.tv_header_title, TypedValue.COMPLEX_UNIT_DIP, 15f * scale)

        rv.setTextColor(R.id.tv_exam_countdown, AgendaPalette.exam.textColor(isDark))
        rv.setTextViewTextSize(R.id.tv_exam_countdown, TypedValue.COMPLEX_UNIT_DIP, 10f * scale)

        rv.setTextColor(R.id.tv_empty_title, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.tv_empty_title, TypedValue.COMPLEX_UNIT_DIP, 13f * scale)

        rv.setTextColor(R.id.tv_empty_hint, colors.secondaryTextColor)
        rv.setTextViewTextSize(R.id.tv_empty_hint, TypedValue.COMPLEX_UNIT_DIP, 10f * scale)
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
                secondaryTextColor = scheme.onSurfaceVariant.toArgb()
            )
        } else {
            val contentColor = scheme.onPrimaryContainer
            ContentColors(
                backgroundColor = scheme.primaryContainer.toArgb(),
                primaryTextColor = contentColor.toArgb(),
                secondaryTextColor = contentColor.copy(alpha = 0.72f).toArgb()
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