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
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.RemoteViewsCompat
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import com.xingheyuzhuan.shiguangschedule.widget.WidgetAgendaItemProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 日程与考试看板原生小组件渲染器
 */
object AgendaNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")

    private const val MAX_AGENDA_ITEMS = 50

    private const val COLOR_TODO = 0xFF1A73E8.toInt()
    private const val COLOR_EXAM = 0xFFE5484D.toInt()
    private const val COLOR_SCHEDULE = 0xFF2E7D32.toInt()

    /**
     * 日程节点分类，同时携带徽章文案、背景与文字颜色
     */
    private enum class AgendaKind(
        @StringRes val badgeTextRes: Int,
        @DrawableRes val badgeBackgroundRes: Int,
        @ColorInt val badgeTextColor: Int
    ) {
        TODO(R.string.widget_agenda_badge_todo, R.drawable.widget_agenda_badge_todo, COLOR_TODO),
        EXAM(R.string.widget_agenda_badge_exam, R.drawable.widget_agenda_badge_exam, COLOR_EXAM),
        SCHEDULE(R.string.widget_agenda_badge_schedule, R.drawable.widget_agenda_badge_schedule, COLOR_SCHEDULE)
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
        val secondaryTextColor: Int
    )

    private data class ItemViewIds(
        val root: Int,
        val dateTag: Int,
        val time: Int,
        val endTime: Int,
        val badge: Int,
        val title: Int,
        val countdown: Int,
        val location: Int
    )

    private val LIGHT_ITEM_IDS = ItemViewIds(
        root = R.id.item_root_agenda,
        dateTag = R.id.tv_item_date_tag,
        time = R.id.tv_item_time,
        endTime = R.id.tv_item_end_time,
        badge = R.id.tv_item_badge,
        title = R.id.tv_item_title,
        countdown = R.id.tv_item_countdown,
        location = R.id.tv_item_location
    )

    private val DARK_ITEM_IDS = ItemViewIds(
        root = R.id.item_root_agenda_dark,
        dateTag = R.id.tv_item_date_tag_dark,
        time = R.id.tv_item_time_dark,
        endTime = R.id.tv_item_end_time_dark,
        badge = R.id.tv_item_badge_dark,
        title = R.id.tv_item_title_dark,
        countdown = R.id.tv_item_countdown_dark,
        location = R.id.tv_item_location_dark
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

        val lightColors = resolveContentColors(
            isDark = snapshot.style?.widget_style?.theme_mode == WidgetThemeModeProto.WIDGET_THEME_DARK,
            seedColorLong = snapshot.style?.widget_style?.seed_color
        )
        val darkColors = resolveContentColors(
            isDark = snapshot.style?.widget_style?.theme_mode != WidgetThemeModeProto.WIDGET_THEME_LIGHT,
            seedColorLong = snapshot.style?.widget_style?.seed_color
        )
        applyWidgetStyle(rv, snapshot, lightColors, darkColors)

        val today = LocalDate.now()
        val weekDays = context.resources.getStringArray(R.array.week_days_names)
        val weekDayText = weekDays.getOrElse(today.dayOfWeek.value - 1) { "" }
        val headerText = context.getString(
            R.string.widget_agenda_header_date,
            today.monthValue,
            today.dayOfMonth,
            weekDayText
        )
        rv.setTextViewText(R.id.tv_header_title, headerText)
        rv.setTextViewText(R.id.tv_header_title_dark, headerText)

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
            renderAgendaList(context, rv, appWidgetId, snapshot, agendaItems, lightColors, darkColors)
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

        rv.setViewVisibility(R.id.list_view_agenda_dark, View.VISIBLE)
        rv.setViewVisibility(R.id.container_empty_dark, View.GONE)
        rv.setViewVisibility(R.id.tv_exam_countdown_dark, View.GONE)
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
        rv.setOnClickPendingIntent(R.id.widget_root_dark, pendingIntent)
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

        rv.setViewVisibility(R.id.tv_exam_countdown_dark, View.VISIBLE)
        rv.setTextViewText(R.id.tv_exam_countdown_dark, text)
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
        snapshot: WidgetSnapshot,
        items: List<AgendaItem>,
        lightColors: ContentColors,
        darkColors: ContentColors
    ) {
        rv.setViewVisibility(R.id.list_view_agenda, View.VISIBLE)
        rv.setViewVisibility(R.id.container_empty, View.GONE)
        rv.setViewVisibility(R.id.list_view_agenda_dark, View.VISIBLE)
        rv.setViewVisibility(R.id.container_empty_dark, View.GONE)

        val builder = RemoteViewsCompat.RemoteCollectionItems.Builder()

        items.forEachIndexed { index, item ->
            val itemRv = createAgendaItemView(context, snapshot, item, lightColors, darkColors)

            val fillInIntent = Intent().apply { putExtra("agenda_id", item.id) }
            itemRv.setOnClickFillInIntent(LIGHT_ITEM_IDS.root, fillInIntent)
            itemRv.setOnClickFillInIntent(DARK_ITEM_IDS.root, fillInIntent)

            val itemId = "${item.id}#$index".hashCode().toLong() and 0x7FFFFFFFFFL
            builder.addItem(itemId, itemRv)
        }

        builder.setHasStableIds(true)
        val collectionItems = builder.build()

        RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, R.id.list_view_agenda, collectionItems)
        RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, R.id.list_view_agenda_dark, collectionItems)

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
        rv.setPendingIntentTemplate(R.id.list_view_agenda_dark, pendingIntentTemplate)
    }

    /**
     * 创建单条日程节点视图（同时绑定常规与暗色两套 View 树）
     */
    private fun createAgendaItemView(
        context: Context,
        snapshot: WidgetSnapshot,
        item: AgendaItem,
        lightColors: ContentColors,
        darkColors: ContentColors
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item_agenda_node)

        val rawScale = snapshot.style?.widget_style?.font_scale ?: 1.0f
        val scale = if (rawScale > 0f) rawScale else 1.0f
        val badgeText = context.getString(item.kind.badgeTextRes)
        val timeText = item.startTime.trim().ifBlank { context.getString(R.string.widget_agenda_all_day) }
        val endTimeText = item.endTime.trim()
        val locationText = item.location.trim().let { location ->
            if (location.isBlank()) "" else "📍 $location"
        }
        val dateTag = item.date.format(DATE_FORMATTER)
        val countdownText = if (item.daysLeft <= 0) {
            context.getString(R.string.widget_title_today)
        } else {
            context.getString(R.string.widget_agenda_days_left, item.daysLeft)
        }

        bindItemGroup(
            rv, LIGHT_ITEM_IDS, lightColors, item.kind, item.title, badgeText,
            dateTag, timeText, endTimeText, countdownText, locationText, scale
        )
        bindItemGroup(
            rv, DARK_ITEM_IDS, darkColors, item.kind, item.title, badgeText,
            dateTag, timeText, endTimeText, countdownText, locationText, scale
        )

        return rv
    }

    /**
     * 绑定一整套日程条目视图的文本、颜色与可见性
     */
    private fun bindItemGroup(
        rv: RemoteViews,
        ids: ItemViewIds,
        colors: ContentColors,
        kind: AgendaKind,
        title: String,
        badgeText: String,
        dateTag: String,
        timeText: String,
        endTimeText: String,
        countdownText: String,
        locationText: String,
        scale: Float
    ) {
        rv.setTextViewText(ids.dateTag, dateTag)
        rv.setTextColor(ids.dateTag, colors.secondaryTextColor)
        rv.setTextViewTextSize(ids.dateTag, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)

        rv.setTextViewText(ids.time, timeText)
        rv.setTextColor(ids.time, colors.primaryTextColor)
        rv.setTextViewTextSize(ids.time, TypedValue.COMPLEX_UNIT_DIP, 10.5f * scale)

        if (endTimeText.isNotBlank()) {
            rv.setViewVisibility(ids.endTime, View.VISIBLE)
            rv.setTextViewText(ids.endTime, endTimeText)
            rv.setTextColor(ids.endTime, colors.secondaryTextColor)
            rv.setTextViewTextSize(ids.endTime, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)
        } else {
            rv.setViewVisibility(ids.endTime, View.GONE)
        }

        rv.setTextViewText(ids.badge, badgeText)
        rv.setTextColor(ids.badge, kind.badgeTextColor)
        rv.setTextViewTextSize(ids.badge, TypedValue.COMPLEX_UNIT_DIP, 8f * scale)
        rv.setInt(ids.badge, "setBackgroundResource", kind.badgeBackgroundRes)

        rv.setTextViewText(ids.title, title)
        rv.setTextColor(ids.title, colors.primaryTextColor)
        rv.setTextViewTextSize(ids.title, TypedValue.COMPLEX_UNIT_DIP, 12.5f * scale)

        rv.setTextViewText(ids.countdown, countdownText)
        rv.setTextColor(ids.countdown, kind.badgeTextColor)
        rv.setTextViewTextSize(ids.countdown, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)

        if (locationText.isNotBlank()) {
            rv.setViewVisibility(ids.location, View.VISIBLE)
            rv.setTextViewText(ids.location, locationText)
            rv.setTextColor(ids.location, colors.secondaryTextColor)
            rv.setTextViewTextSize(ids.location, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)
        } else {
            rv.setViewVisibility(ids.location, View.GONE)
        }
    }

    /**
     * 显示无日程空状态
     */
    private fun showEmptyState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.list_view_agenda, View.GONE)
        rv.setViewVisibility(R.id.container_empty, View.VISIBLE)
        rv.setViewVisibility(R.id.list_view_agenda_dark, View.GONE)
        rv.setViewVisibility(R.id.container_empty_dark, View.VISIBLE)
    }

    /**
     * 应用小组件主题样式、背景透明度与字体缩放
     */
    private fun applyWidgetStyle(
        rv: RemoteViews,
        snapshot: WidgetSnapshot,
        lightColors: ContentColors,
        darkColors: ContentColors
    ) {
        val widgetStyle = snapshot.style?.widget_style
        val alphaPercent: Float = widgetStyle?.background_alpha ?: 1.0f
        val fontScale: Float = widgetStyle?.font_scale ?: 1.0f
        val scale = if (fontScale > 0f) fontScale else 1.0f

        applyGroupStyle(
            rv, lightColors, alphaPercent, scale,
            R.id.card_bg_image, R.id.tv_header_title, R.id.tv_exam_countdown,
            R.id.tv_empty_icon, R.id.tv_empty_title, R.id.tv_empty_hint
        )
        applyGroupStyle(
            rv, darkColors, alphaPercent, scale,
            R.id.card_bg_image_dark, R.id.tv_header_title_dark, R.id.tv_exam_countdown_dark,
            R.id.tv_empty_icon_dark, R.id.tv_empty_title_dark, R.id.tv_empty_hint_dark
        )
    }

    /**
     * 设置一组视图的背景色、文字颜色与字体缩放
     */
    private fun applyGroupStyle(
        rv: RemoteViews,
        colors: ContentColors,
        alphaPercent: Float,
        scale: Float,
        cardBgId: Int,
        headerTitleId: Int,
        examCountdownId: Int,
        emptyIconId: Int,
        emptyTitleId: Int,
        emptyHintId: Int
    ) {
        rv.setImageViewBackground(cardBgId, colors.backgroundColor, alphaPercent)

        rv.setTextColor(headerTitleId, colors.primaryTextColor)
        rv.setTextViewTextSize(headerTitleId, TypedValue.COMPLEX_UNIT_DIP, 15f * scale)

        rv.setTextColor(examCountdownId, COLOR_EXAM)
        rv.setTextViewTextSize(examCountdownId, TypedValue.COMPLEX_UNIT_DIP, 10f * scale)

        rv.setTextColor(emptyIconId, colors.secondaryTextColor)
        rv.setTextViewTextSize(emptyIconId, TypedValue.COMPLEX_UNIT_DIP, 24f * scale)

        rv.setTextColor(emptyTitleId, colors.primaryTextColor)
        rv.setTextViewTextSize(emptyTitleId, TypedValue.COMPLEX_UNIT_DIP, 14f * scale)

        rv.setTextColor(emptyHintId, colors.secondaryTextColor)
        rv.setTextViewTextSize(emptyHintId, TypedValue.COMPLEX_UNIT_DIP, 10f * scale)
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
                secondaryTextColor = scheme.onSurfaceVariant.toArgb()
            )
        } else {
            val contentColor = scheme.onPrimaryContainer.toArgb()
            ContentColors(
                backgroundColor = scheme.primaryContainer.toArgb(),
                primaryTextColor = contentColor,
                secondaryTextColor = contentColor
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