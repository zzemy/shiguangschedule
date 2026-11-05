package com.xingheyuzhuan.shiguangschedule.widget.agenda

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto

/**
 * 日程类小组件共享的分类视觉资源。
 * 明暗两套底色均放在 drawable/ 中，由渲染器按小组件主题模式显式选择，
 * 不依赖启动器的夜间资源限定符，避免强制深浅色时出现低对比度。
 */
internal data class AgendaAccent(
    @ColorInt val lightTextColor: Int,
    @ColorInt val darkTextColor: Int,
    @DrawableRes val lightBackgroundRes: Int,
    @DrawableRes val darkBackgroundRes: Int
) {
    fun textColor(isDark: Boolean): Int = if (isDark) darkTextColor else lightTextColor
    fun backgroundRes(isDark: Boolean): Int = if (isDark) darkBackgroundRes else lightBackgroundRes
}

internal object AgendaPalette {

    /** 待办：低饱和度灰蓝 */
    val todo = AgendaAccent(
        lightTextColor = 0xFF5C6B8A.toInt(),
        darkTextColor = 0xFFAEBBD6.toInt(),
        lightBackgroundRes = R.drawable.widget_agenda_badge_todo,
        darkBackgroundRes = R.drawable.widget_agenda_badge_todo_dark
    )

    /** 考试：柔和珊瑚红 */
    val exam = AgendaAccent(
        lightTextColor = 0xFFC05B56.toInt(),
        darkTextColor = 0xFFF0A9A3.toInt(),
        lightBackgroundRes = R.drawable.widget_agenda_badge_exam,
        darkBackgroundRes = R.drawable.widget_agenda_badge_exam_dark
    )

    /** 日程：低饱和度鼠尾草绿 */
    val schedule = AgendaAccent(
        lightTextColor = 0xFF5F7D6B.toInt(),
        darkTextColor = 0xFFA8C6B4.toInt(),
        lightBackgroundRes = R.drawable.widget_agenda_badge_schedule,
        darkBackgroundRes = R.drawable.widget_agenda_badge_schedule_dark
    )

    fun pillBackgroundRes(isDark: Boolean): Int =
        if (isDark) R.drawable.widget_agenda_pill_bg_dark else R.drawable.widget_agenda_pill_bg
}

/**
 * 解析小组件最终应使用的深浅色主题。
 * FOLLOW_SYSTEM 时读取系统 uiMode，强制模式时以用户配置为准。
 */
internal fun resolveWidgetIsDark(context: Context, mode: WidgetThemeModeProto?): Boolean =
    when (mode) {
        WidgetThemeModeProto.WIDGET_THEME_DARK -> true
        WidgetThemeModeProto.WIDGET_THEME_LIGHT -> false
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }