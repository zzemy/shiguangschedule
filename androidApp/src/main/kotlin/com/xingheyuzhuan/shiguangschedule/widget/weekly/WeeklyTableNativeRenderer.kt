package com.xingheyuzhuan.shiguangschedule.widget.weekly

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 全周课表大卡片原生小组件渲染器
 * 以周一至周日七列网格展示当前自然周的课程，并高亮今天所在列
 */
object WeeklyTableNativeRenderer {

    private const val DAY_COUNT = 7
    private const val MAX_COURSES_PER_DAY = 6
    private const val DARK_TEXT_COLOR = 0xFF1B1B1F.toInt()
    private const val LIGHT_TEXT_COLOR = 0xFFFFFFFF.toInt()
    private const val FALLBACK_LIGHT_COLOR = 0xFFE8EAED.toInt()
    private const val FALLBACK_DARK_COLOR = 0xFF3A3A3C.toInt()

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")

    private val COLUMN_IDS = intArrayOf(
        R.id.weekly_column_1, R.id.weekly_column_2, R.id.weekly_column_3,
        R.id.weekly_column_4, R.id.weekly_column_5, R.id.weekly_column_6,
        R.id.weekly_column_7
    )

    private val DAY_NAME_IDS = intArrayOf(
        R.id.weekly_day_name_1, R.id.weekly_day_name_2, R.id.weekly_day_name_3,
        R.id.weekly_day_name_4, R.id.weekly_day_name_5, R.id.weekly_day_name_6,
        R.id.weekly_day_name_7
    )

    private val DAY_DATE_IDS = intArrayOf(
        R.id.weekly_day_date_1, R.id.weekly_day_date_2, R.id.weekly_day_date_3,
        R.id.weekly_day_date_4, R.id.weekly_day_date_5, R.id.weekly_day_date_6,
        R.id.weekly_day_date_7
    )

    private val MORE_IDS = intArrayOf(
        R.id.weekly_more_1, R.id.weekly_more_2, R.id.weekly_more_3,
        R.id.weekly_more_4, R.id.weekly_more_5, R.id.weekly_more_6,
        R.id.weekly_more_7
    )

    // 按 天（7）× 课块（6）的顺序排列，索引通过 blockIndex() 计算
    private val BLOCK_IDS = intArrayOf(
        R.id.weekly_block_1_1, R.id.weekly_block_1_2, R.id.weekly_block_1_3,
        R.id.weekly_block_1_4, R.id.weekly_block_1_5, R.id.weekly_block_1_6,
        R.id.weekly_block_2_1, R.id.weekly_block_2_2, R.id.weekly_block_2_3,
        R.id.weekly_block_2_4, R.id.weekly_block_2_5, R.id.weekly_block_2_6,
        R.id.weekly_block_3_1, R.id.weekly_block_3_2, R.id.weekly_block_3_3,
        R.id.weekly_block_3_4, R.id.weekly_block_3_5, R.id.weekly_block_3_6,
        R.id.weekly_block_4_1, R.id.weekly_block_4_2, R.id.weekly_block_4_3,
        R.id.weekly_block_4_4, R.id.weekly_block_4_5, R.id.weekly_block_4_6,
        R.id.weekly_block_5_1, R.id.weekly_block_5_2, R.id.weekly_block_5_3,
        R.id.weekly_block_5_4, R.id.weekly_block_5_5, R.id.weekly_block_5_6,
        R.id.weekly_block_6_1, R.id.weekly_block_6_2, R.id.weekly_block_6_3,
        R.id.weekly_block_6_4, R.id.weekly_block_6_5, R.id.weekly_block_6_6,
        R.id.weekly_block_7_1, R.id.weekly_block_7_2, R.id.weekly_block_7_3,
        R.id.weekly_block_7_4, R.id.weekly_block_7_5, R.id.weekly_block_7_6
    )

    private val BLOCK_NAME_IDS = intArrayOf(
        R.id.weekly_block_name_1_1, R.id.weekly_block_name_1_2, R.id.weekly_block_name_1_3,
        R.id.weekly_block_name_1_4, R.id.weekly_block_name_1_5, R.id.weekly_block_name_1_6,
        R.id.weekly_block_name_2_1, R.id.weekly_block_name_2_2, R.id.weekly_block_name_2_3,
        R.id.weekly_block_name_2_4, R.id.weekly_block_name_2_5, R.id.weekly_block_name_2_6,
        R.id.weekly_block_name_3_1, R.id.weekly_block_name_3_2, R.id.weekly_block_name_3_3,
        R.id.weekly_block_name_3_4, R.id.weekly_block_name_3_5, R.id.weekly_block_name_3_6,
        R.id.weekly_block_name_4_1, R.id.weekly_block_name_4_2, R.id.weekly_block_name_4_3,
        R.id.weekly_block_name_4_4, R.id.weekly_block_name_4_5, R.id.weekly_block_name_4_6,
        R.id.weekly_block_name_5_1, R.id.weekly_block_name_5_2, R.id.weekly_block_name_5_3,
        R.id.weekly_block_name_5_4, R.id.weekly_block_name_5_5, R.id.weekly_block_name_5_6,
        R.id.weekly_block_name_6_1, R.id.weekly_block_name_6_2, R.id.weekly_block_name_6_3,
        R.id.weekly_block_name_6_4, R.id.weekly_block_name_6_5, R.id.weekly_block_name_6_6,
        R.id.weekly_block_name_7_1, R.id.weekly_block_name_7_2, R.id.weekly_block_name_7_3,
        R.id.weekly_block_name_7_4, R.id.weekly_block_name_7_5, R.id.weekly_block_name_7_6
    )

    private val BLOCK_ROOM_IDS = intArrayOf(
        R.id.weekly_block_room_1_1, R.id.weekly_block_room_1_2, R.id.weekly_block_room_1_3,
        R.id.weekly_block_room_1_4, R.id.weekly_block_room_1_5, R.id.weekly_block_room_1_6,
        R.id.weekly_block_room_2_1, R.id.weekly_block_room_2_2, R.id.weekly_block_room_2_3,
        R.id.weekly_block_room_2_4, R.id.weekly_block_room_2_5, R.id.weekly_block_room_2_6,
        R.id.weekly_block_room_3_1, R.id.weekly_block_room_3_2, R.id.weekly_block_room_3_3,
        R.id.weekly_block_room_3_4, R.id.weekly_block_room_3_5, R.id.weekly_block_room_3_6,
        R.id.weekly_block_room_4_1, R.id.weekly_block_room_4_2, R.id.weekly_block_room_4_3,
        R.id.weekly_block_room_4_4, R.id.weekly_block_room_4_5, R.id.weekly_block_room_4_6,
        R.id.weekly_block_room_5_1, R.id.weekly_block_room_5_2, R.id.weekly_block_room_5_3,
        R.id.weekly_block_room_5_4, R.id.weekly_block_room_5_5, R.id.weekly_block_room_5_6,
        R.id.weekly_block_room_6_1, R.id.weekly_block_room_6_2, R.id.weekly_block_room_6_3,
        R.id.weekly_block_room_6_4, R.id.weekly_block_room_6_5, R.id.weekly_block_room_6_6,
        R.id.weekly_block_room_7_1, R.id.weekly_block_room_7_2, R.id.weekly_block_room_7_3,
        R.id.weekly_block_room_7_4, R.id.weekly_block_room_7_5, R.id.weekly_block_room_7_6
    )

    private data class ContentColors(
        val backgroundColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int,
        val hintTextColor: Int,
        val accentColor: Int,
        val todayHighlightColor: Int
    )

    /**
     * 渲染并返回小组件的 RemoteViews 视图
     */
    fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_weekly_table_native)

        val today = LocalDate.now()
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekDates = (0 until DAY_COUNT).map { weekStart.plusDays(it.toLong()) }

        val isDark = resolveDarkMode(context, snapshot)
        val colors = resolveContentColors(isDark, snapshot.style?.widget_style?.seed_color)
        val coursesByDate = collectWeekCourses(snapshot.courses, weekDates)
        val totalCourses = coursesByDate.values.sumOf { it.size }

        applyWidgetStyle(rv, snapshot, colors)
        bindHeader(context, rv, snapshot, totalCourses)
        bindHeaderClick(context, rv)
        renderColumns(context, rv, snapshot, colors, isDark, weekDates, today, coursesByDate)

        rv.setViewVisibility(
            R.id.weekly_empty,
            if (totalCourses == 0) View.VISIBLE else View.GONE
        )

        return rv
    }

    /**
     * 绑定顶部标题、学期周数与本周课程数
     */
    private fun bindHeader(
        context: Context,
        rv: RemoteViews,
        snapshot: WidgetSnapshot,
        totalCourses: Int
    ) {
        val currentWeek = snapshot.current_week
        val title = if (currentWeek > 0) {
            context.getString(R.string.widget_weekly_table_title, currentWeek)
        } else {
            context.getString(R.string.widget_weekly_table_vacation_title)
        }
        rv.setTextViewText(R.id.weekly_header_title, title)

        val subtitle = when {
            currentWeek <= 0 -> {
                val daysUntilStart = snapshot.days_until_term_start
                if (daysUntilStart > 0) {
                    context.getString(R.string.widget_days_until_term_start, daysUntilStart)
                } else {
                    context.getString(R.string.widget_vacation_expecting)
                }
            }

            snapshot.total_weeks > 0 -> {
                context.getString(
                    R.string.widget_weekly_table_subtitle,
                    snapshot.total_weeks,
                    totalCourses
                )
            }

            else -> context.getString(R.string.widget_weekly_table_course_count, totalCourses)
        }
        rv.setTextViewText(R.id.weekly_header_subtitle, subtitle)
    }

    /**
     * 绑定顶部点击事件（点击进入 App 主界面）
     */
    private fun bindHeaderClick(context: Context, rv: RemoteViews) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.weekly_header, pendingIntent)
    }

    /**
     * 渲染周一至周日七列课程网格
     */
    private fun renderColumns(
        context: Context,
        rv: RemoteViews,
        snapshot: WidgetSnapshot,
        colors: ContentColors,
        isDark: Boolean,
        weekDates: List<LocalDate>,
        today: LocalDate,
        coursesByDate: Map<LocalDate, List<WidgetCourseProto>>
    ) {
        val dayNames = context.resources.getStringArray(R.array.week_days_names)

        weekDates.forEachIndexed { dayIndex, date ->
            val isToday = date == today
            val dayNameColor = if (isToday) colors.accentColor else colors.primaryTextColor
            val dayDateColor = if (isToday) colors.accentColor else colors.secondaryTextColor

            rv.setTextViewText(DAY_NAME_IDS[dayIndex], dayNames.getOrElse(dayIndex) { "" })
            rv.setTextColor(DAY_NAME_IDS[dayIndex], dayNameColor)
            rv.setTextViewText(DAY_DATE_IDS[dayIndex], date.format(DATE_FORMATTER))
            rv.setTextColor(DAY_DATE_IDS[dayIndex], dayDateColor)

            if (isToday) {
                setViewBackgroundTint(rv, COLUMN_IDS[dayIndex], colors.todayHighlightColor)
            } else {
                setViewBackgroundTint(rv, COLUMN_IDS[dayIndex], android.graphics.Color.TRANSPARENT)
            }

            val dayCourses = coursesByDate[date].orEmpty()
            val visibleCount = minOf(dayCourses.size, MAX_COURSES_PER_DAY)

            for (slot in 0 until visibleCount) {
                bindCourseBlock(
                    rv, snapshot, isDark, dayIndex, slot, dayCourses[slot]
                )
            }
            for (slot in visibleCount until MAX_COURSES_PER_DAY) {
                rv.setViewVisibility(BLOCK_IDS[blockIndex(dayIndex, slot)], View.GONE)
            }

            val extraCount = dayCourses.size - MAX_COURSES_PER_DAY
            if (extraCount > 0) {
                rv.setViewVisibility(MORE_IDS[dayIndex], View.VISIBLE)
                rv.setTextViewText(
                    MORE_IDS[dayIndex],
                    context.getString(R.string.widget_weekly_table_more, extraCount)
                )
            } else {
                rv.setViewVisibility(MORE_IDS[dayIndex], View.GONE)
            }
        }
    }

    /**
     * 绑定单列中的单个课程色块
     */
    private fun bindCourseBlock(
        rv: RemoteViews,
        snapshot: WidgetSnapshot,
        isDark: Boolean,
        dayIndex: Int,
        slot: Int,
        course: WidgetCourseProto
    ) {
        val index = blockIndex(dayIndex, slot)
        val blockId = BLOCK_IDS[index]
        val nameId = BLOCK_NAME_IDS[index]
        val roomId = BLOCK_ROOM_IDS[index]

        val background = resolveCourseColor(snapshot, course.color_int, isDark)
        val contentColor = contentColorFor(background)

        rv.setViewVisibility(blockId, View.VISIBLE)
        setViewBackgroundTint(rv, blockId, background)

        rv.setTextViewText(nameId, abbreviateName(course.name))
        rv.setTextColor(nameId, contentColor)

        val room = course.position.trim()
        if (room.isEmpty()) {
            rv.setViewVisibility(roomId, View.GONE)
        } else {
            rv.setViewVisibility(roomId, View.VISIBLE)
            rv.setTextViewText(roomId, room)
            rv.setTextColor(roomId, contentColor)
        }
    }

    /**
     * 筛选当前自然周内需要展示的课程（排除倒计时与每日待办）
     */
    private fun collectWeekCourses(
        courses: List<WidgetCourseProto>,
        weekDates: List<LocalDate>
    ): Map<LocalDate, List<WidgetCourseProto>> {
        val weekDateSet = weekDates.toHashSet()
        val grouped = LinkedHashMap<LocalDate, MutableList<WidgetCourseProto>>()

        courses.forEach { course ->
            if (course.is_skipped) return@forEach
            if (course.id.startsWith("countdown-") || course.id.startsWith("daily-")) {
                return@forEach
            }

            val date = parseDate(course.date) ?: return@forEach
            if (date !in weekDateSet) return@forEach

            grouped.getOrPut(date) { mutableListOf() }.add(course)
        }

        return grouped.mapValues { (_, items) ->
            items.sortedWith(compareBy { it.start_time.ifBlank { "99:99" } })
        }
    }

    /**
     * 课程名缩略，适配窄列展示
     */
    private fun abbreviateName(name: String): String {
        val cleaned = name.substringAfter("] ", name).filterNot { it.isWhitespace() }
        if (cleaned.isEmpty()) return ""
        return if (cleaned.length <= 4) cleaned else cleaned.take(4) + "…"
    }

    private fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (_: Exception) {
        null
    }

    private fun blockIndex(dayIndex: Int, slot: Int): Int =
        dayIndex * MAX_COURSES_PER_DAY + slot

    /**
     * 根据课程颜色索引解析当前主题下的色块颜色
     */
    private fun resolveCourseColor(
        snapshot: WidgetSnapshot,
        colorIndex: Int,
        isDark: Boolean
    ): Int {
        val colorPair = snapshot.style?.course_color_maps?.getOrNull(colorIndex)
            ?: return if (isDark) FALLBACK_DARK_COLOR else FALLBACK_LIGHT_COLOR
        return (if (isDark) colorPair.dark_color else colorPair.light_color).toInt()
    }

    /**
     * 根据色块亮度选择高对比文字颜色
     */
    @ColorInt
    private fun contentColorFor(@ColorInt background: Int): Int =
        if (ColorUtils.calculateLuminance(background) > 0.5) DARK_TEXT_COLOR else LIGHT_TEXT_COLOR

    /**
     * 判断小组件应使用深色还是浅色内容配色
     */
    private fun resolveDarkMode(context: Context, snapshot: WidgetSnapshot): Boolean {
        return when (snapshot.style?.widget_style?.theme_mode) {
            WidgetThemeModeProto.WIDGET_THEME_DARK -> true
            WidgetThemeModeProto.WIDGET_THEME_LIGHT -> false
            else -> (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * 根据主题模式与种子色解析背景、文字与强调色
     */
    private fun resolveContentColors(isDark: Boolean, seedColorLong: Long?): ContentColors {
        val scheme = if (seedColorLong != null) {
            createMaterialKolorScheme(darkTheme = isDark, seedColor = Color(seedColorLong))
        } else {
            createMaterialKolorScheme(darkTheme = isDark)
        }

        val backgroundColor: Int
        val primaryTextColor: Int
        val secondaryTextColor: Int
        val hintTextColor: Int

        if (seedColorLong == null) {
            backgroundColor = scheme.surfaceContainer.toArgb()
            primaryTextColor = scheme.onSurface.toArgb()
            secondaryTextColor = scheme.onSurfaceVariant.toArgb()
            hintTextColor = scheme.outline.toArgb()
        } else {
            backgroundColor = scheme.primaryContainer.toArgb()
            val contentColor = scheme.onPrimaryContainer.toArgb()
            primaryTextColor = contentColor
            secondaryTextColor = contentColor
            hintTextColor = contentColor
        }

        val accentColor = scheme.primary.toArgb()
        val highlightAlpha = if (isDark) 0x33 else 0x1F

        return ContentColors(
            backgroundColor = backgroundColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            hintTextColor = hintTextColor,
            accentColor = accentColor,
            todayHighlightColor = ColorUtils.setAlphaComponent(accentColor, highlightAlpha)
        )
    }

    /**
     * 应用卡片背景、文字颜色与字体缩放
     */
    private fun applyWidgetStyle(
        rv: RemoteViews,
        snapshot: WidgetSnapshot,
        colors: ContentColors
    ) {
        val widgetStyle = snapshot.style?.widget_style
        val alphaPercent: Float = widgetStyle?.background_alpha ?: 1.0f
        val rawScale = widgetStyle?.font_scale ?: 1.0f
        val scale = if (rawScale > 0f) rawScale else 1.0f

        applyImageViewBackground(rv, R.id.weekly_card_bg, colors.backgroundColor, alphaPercent)

        rv.setTextColor(R.id.weekly_header_title, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.weekly_header_title, TypedValue.COMPLEX_UNIT_DIP, 15f * scale)

        rv.setTextColor(R.id.weekly_header_subtitle, colors.secondaryTextColor)
        rv.setTextViewTextSize(R.id.weekly_header_subtitle, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)

        rv.setTextColor(R.id.weekly_header_action, colors.accentColor)
        rv.setTextViewTextSize(R.id.weekly_header_action, TypedValue.COMPLEX_UNIT_DIP, 10f * scale)

        rv.setTextColor(R.id.weekly_empty, colors.hintTextColor)
        rv.setTextViewTextSize(R.id.weekly_empty, TypedValue.COMPLEX_UNIT_DIP, 11f * scale)

        for (dayIndex in 0 until DAY_COUNT) {
            rv.setTextViewTextSize(
                DAY_NAME_IDS[dayIndex], TypedValue.COMPLEX_UNIT_DIP, 10f * scale
            )
            rv.setTextViewTextSize(
                DAY_DATE_IDS[dayIndex], TypedValue.COMPLEX_UNIT_DIP, 8f * scale
            )
            rv.setTextColor(MORE_IDS[dayIndex], colors.hintTextColor)
            rv.setTextViewTextSize(
                MORE_IDS[dayIndex], TypedValue.COMPLEX_UNIT_DIP, 7f * scale
            )
        }

        BLOCK_NAME_IDS.forEach { id ->
            rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_DIP, 8f * scale)
        }
        BLOCK_ROOM_IDS.forEach { id ->
            rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_DIP, 6.5f * scale)
        }
    }

    /**
     * 设置 View 背景色（优先保留圆角 drawable）
     */
    private fun setViewBackgroundTint(
        rv: RemoteViews,
        viewId: Int,
        @ColorInt color: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color))
        } else {
            rv.setInt(viewId, "setBackgroundColor", color)
        }
    }

    /**
     * 设置 ImageView 背景色与透明度（兼容不同 Android 版本）
     */
    private fun applyImageViewBackground(
        rv: RemoteViews,
        bgViewId: Int,
        @ColorInt baseColor: Int,
        @FloatRange(from = 0.0, to = 1.0) alphaPercent: Float
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setColorStateList(bgViewId, "setImageTintList", ColorStateList.valueOf(baseColor))
        } else {
            rv.setInt(bgViewId, "setColorFilter", baseColor)
        }

        val alphaInt = (alphaPercent * 255).toInt().coerceIn(0, 255)
        rv.setInt(bgViewId, "setImageAlpha", alphaInt)
    }
}