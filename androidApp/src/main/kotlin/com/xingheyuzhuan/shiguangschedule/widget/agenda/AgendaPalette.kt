package com.xingheyuzhuan.shiguangschedule.widget.agenda

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ColorInt
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto

/**
 * 日程类小组件共享的分类强调色。
 * 徽章与胶囊的底色由 layout / layout-night 中的静态 drawable 直接指定，
 * 避免在 RemoteViews 中使用非 @RemotableViewMethod 的 setBackgroundResource。
 */
internal data class AgendaAccent(
    @ColorInt val lightTextColor: Int,
    @ColorInt val darkTextColor: Int
) {
    fun textColor(isDark: Boolean): Int = if (isDark) darkTextColor else lightTextColor
}

internal object AgendaPalette {

    /** 待办：低饱和度灰蓝 */
    val todo = AgendaAccent(
        lightTextColor = 0xFF5C6B8A.toInt(),
        darkTextColor = 0xFFAEBBD6.toInt()
    )

    /** 考试：柔和珊瑚红 */
    val exam = AgendaAccent(
        lightTextColor = 0xFFC05B56.toInt(),
        darkTextColor = 0xFFF0A9A3.toInt()
    )

    /** 日程：低饱和度鼠尾草绿 */
    val schedule = AgendaAccent(
        lightTextColor = 0xFF5F7D6B.toInt(),
        darkTextColor = 0xFFA8C6B4.toInt()
    )
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