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
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.RemoteViewsCompat
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import com.xingheyuzhuan.shiguangschedule.widget.WidgetAgendaItemProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 日程与考试看板原生小组件渲染器（4x2）
 *
 * 布局的 layout/ 与 layout-night/ 必须使用同一套 View ID，
 * 这里只绑定这一套 ID，避免宿主 inflate 后找不到另一套 ID 触发 ActionException。
 */
object AgendaNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")

    private const val MAX_AGENDA_ITEMS = 50

    /**
     * 日程节点分类，同时携带统一的徽章样式与分类强调色
     */
    private enum class AgendaKind(
        @StringRes val badgeTextRes: Int,
        val accent: AgendaAccent
    ) {
        TODO(R.string.widget_agenda_badge_todo, AgendaPalette.todo),
        EXAM(R.string.widget_agenda_badge_exam, AgendaPalette.exam),
        SCHEDULE(R.string.widget_agenda_badge_schedule, AgendaPalette.schedule)
    }

    private data class AgendaItem(
        val id: String,
        val title: String,
        val kind: AgendaKind,
        val date: LocalDate,
        val startTime: String,
        val endTime: String,
        val location: String,
        val daysLeft: Int
    )

    private data class ContentColors(
        val backgroundColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int,
        val dividerColor: Int
    )

    /**
     * 渲染并返回小组件的 RemoteViews 视图
     */
    fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_agenda_native)

        resetWidgetState(rv)
        bindRootClick(context, rv)

        val today = LocalDate.now()
        val widgetStyle = snapshot.style?.widget_style
        val isDark = resolveWidgetIsDark(context, widgetStyle?.theme_mode)
        val colors = resolveContentColors(isDark, widgetStyle?.seed_color)
        val alphaPercent = (widgetStyle?.background_alpha ?: 1.0f).coerceIn(0.0f, 1.0f)
        val rawScale = widgetStyle?.font_scale ?: 1.0f
        val scale = if (rawScale > 0f) rawScale else 1.0f

        applyWidgetStyle(rv, isDark, colors, alphaPercent, scale)
        renderHeader(context, rv, today)
        renderNearestExamCountdown(context, rv, snapshot.agenda_items, today)

        val agendaItems = snapshot.agenda_items
            .asSequence()
            .mapNotNull { item -> toAgendaItem(item, today) }
            .sortedWith(compareBy<AgendaItem> { it.date }.thenBy { item -> item.startTime.ifBlank { "99:99" } })
            .take(MAX_AGENDA_ITEMS)
            .toList()

        if (agendaItems.isEmpty()) {
            showEmptyState(rv)
        } else {
            renderAgendaList(context, rv, appWidgetId, agendaItems, today, isDark, colors, scale)
        }

        return rv
    }

    /**
     * 重置小组件内部视图的可见性状态
     */
    private fun resetWidgetState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.list_view_agenda, View.VISIBLE)
        rv.setViewVisibility(R.id.container_empty, View.GONE)
        rv.setViewVisibility(R.id.tv_exam_countdown, View.GONE)
    }

    /**
     * 绑定根布局点击事件（点击跳转主界面）
     */
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

    /**
     * 渲染右上角最近考试倒计时胶囊
     */
    private fun renderNearestExamCountdown(
        context: Context,
        rv: RemoteViews,
        items: List<WidgetAgendaItemProto>,
        today: LocalDate
    ) {
        val daysUntilExam = findNearestExam(items, today) ?: return
        val text = if (daysUntilExam <= 0) {
            context.getString(R.string.widget_agenda_exam_today)
        } else {
            context.getString(R.string.widget_agenda_exam_countdown, daysUntilExam)
        }

        rv.setViewVisibility(R.id.tv_exam_countdown, View.VISIBLE)
        rv.setTextViewText(R.id.tv_exam_countdown, text)
    }

    /**
     * 查找距离今天最近的未来考试
     */
    private fun findNearestExam(items: List<WidgetAgendaItemProto>, today: LocalDate): Int? {
        return items.asSequence()
            .filter { it.type.equals("EXAM", ignoreCase = true) }
            .mapNotNull { parseDate(it.date) }
            .map { examDate -> ChronoUnit.DAYS.between(today, examDate).toInt() }
            .filter { days -> days >= 0 }
            .minOrNull()
    }

    /**
     * 将快照中的日程事件转换为可渲染节点
     */
    private fun toAgendaItem(item: WidgetAgendaItemProto, today: LocalDate): AgendaItem? {
        val date = parseDate(item.date) ?: return null
        val daysLeft = ChronoUnit.DAYS.between(today, date).toInt()
        if (daysLeft < 0) return null

        val kind = when {
            item.type.equals("EXAM", ignoreCase = true) -> AgendaKind.EXAM
            item.type.equals("DAILY", ignoreCase = true) -> AgendaKind.TODO
            else -> AgendaKind.SCHEDULE
        }

        val title = item.title.trim()
        if (title.isBlank()) return null

        return AgendaItem(
            id = item.id,
            title = title,
            kind = kind,
            date = date,
            startTime = item.start_time,
            endTime = item.end_time,
            location = item.location,
            daysLeft = daysLeft
        )
    }

    private fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (_: Exception) {
        null
    }

    /**
     * 渲染日程列表视图
     */
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

            val fillInIntent = Intent().apply { putExtra("agenda_id", item.id) }
            itemRv.setOnClickFillInIntent(R.id.item_root_agenda, fillInIntent)

            val itemId = "${item.id}#$index".hashCode().toLong() and 0x7FFFFFFFFFL
            builder.addItem(itemId, itemRv)
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

    /**
     * 创建单条日程节点视图
     */
    private fun createAgendaItemView(
        context: Context,
        today: LocalDate,
        item: AgendaItem,
        isDark: Boolean,
        colors: ContentColors,
        scale: Float
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item_agenda_node)

        val accent = item.kind.accent
        val accentTextColor = accent.textColor(isDark)
        val accentBackgroundRes = accent.backgroundRes(isDark)
        val badgeText = context.getString(item.kind.badgeTextRes)

        val dateTag = if (item.date == today) {
            context.getString(R.string.widget_title_today)
        } else {
            item.date.format(DATE_FORMATTER)
        }
        val timeText = item.startTime.trim().ifBlank { context.getString(R.string.widget_agenda_all_day) }
        val locationText = item.location.trim()
        val countdownText = if (item.daysLeft <= 0) {
            context.getString(R.string.widget_title_today)
        } else {
            context.getString(R.string.widget_agenda_days_left, item.daysLeft)
        }

        rv.setTextViewText(R.id.tv_item_date_tag, dateTag)
        rv.setTextColor(R.id.tv_item_date_tag, colors.secondaryTextColor)
        rv.setTextViewTextSize(R.id.tv_item_date_tag, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)

        rv.setTextViewText(R.id.tv_item_time, timeText)
        rv.setTextColor(R.id.tv_item_time, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.tv_item_time, TypedValue.COMPLEX_UNIT_DIP, 11.5f * scale)

        // 竖向分割线颜色已在 XML 布局中通过 drawable 静态指定，无需通过非法反射方法动态设置

        rv.setTextViewText(R.id.tv_item_badge, badgeText)
        rv.setTextColor(R.id.tv_item_badge, accentTextColor)
        rv.setTextViewTextSize(R.id.tv_item_badge, TypedValue.COMPLEX_UNIT_DIP, 9f * scale)
        rv.setInt(R.id.tv_item_badge, "setBackgroundResource", accentBackgroundRes)

        rv.setTextViewText(R.id.tv_item_title, item.title)
        rv.setTextColor(R.id.tv_item_title, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.tv_item_title, TypedValue.COMPLEX_UNIT_DIP, 13.5f * scale)

        rv.setTextViewText(R.id.tv_item_countdown, countdownText)
        rv.setTextColor(R.id.tv_item_countdown, accentTextColor)
        rv.setTextViewTextSize(R.id.tv_item_countdown, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)
        rv.setInt(R.id.tv_item_countdown, "setBackgroundResource", accentBackgroundRes)

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

    /**
     * 显示无日程空状态
     */
    private fun showEmptyState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.list_view_agenda, View.GONE)
        rv.setViewVisibility(R.id.container_empty, View.VISIBLE)
    }

    /**
     * 应用小组件主题样式、背景透明度与字体缩放
     */
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

        rv.setInt(
            R.id.tv_exam_countdown,
            "setBackgroundResource",
            AgendaPalette.pillBackgroundRes(isDark)
        )
        rv.setTextColor(R.id.tv_exam_countdown, AgendaPalette.exam.textColor(isDark))
        rv.setTextViewTextSize(R.id.tv_exam_countdown, TypedValue.COMPLEX_UNIT_DIP, 10f * scale)

        rv.setTextColor(R.id.tv_empty_title, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.tv_empty_title, TypedValue.COMPLEX_UNIT_DIP, 13f * scale)

        rv.setTextColor(R.id.tv_empty_hint, colors.secondaryTextColor)
        rv.setTextViewTextSize(R.id.tv_empty_hint, TypedValue.COMPLEX_UNIT_DIP, 10f * scale)
    }

    /**
     * 根据主题模式与种子色解析内容配色
     */
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
                dividerColor = scheme.outlineVariant.copy(alpha = 0.55f).toArgb()
            )
        } else {
            val contentColor = scheme.onPrimaryContainer
            ContentColors(
                backgroundColor = scheme.primaryContainer.toArgb(),
                primaryTextColor = contentColor.toArgb(),
                secondaryTextColor = contentColor.copy(alpha = 0.72f).toArgb(),
                dividerColor = contentColor.copy(alpha = 0.18f).toArgb()
            )
        }
    }

    /**
     * 设置 ImageView 背景色与透明度（兼容不同 Android 版本）
     */
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