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
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import com.xingheyuzhuan.shiguangschedule.widget.WidgetAgendaItemProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 日程与考试近期焦点 2x2 小组件渲染器
 * 上半部展示最近考试倒计时，下半部展示今日最近待办。
 */
object AgendaCompactNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")

    private const val ACCENT_EXAM_DAY = 0xFFE5484D.toInt()
    private const val ACCENT_EXAM_NIGHT = 0xFFFF8A80.toInt()

    private enum class AgendaKind {
        EXAM,
        TODO,
        SCHEDULE
    }

    private data class AgendaEntry(
        val title: String,
        val kind: AgendaKind,
        val date: LocalDate,
        val startTime: String,
        val endTime: String,
        val location: String,
        val daysLeft: Int
    ) {
        val sortTime: String get() = startTime.ifBlank { "99:99" }
    }

    private data class ContentColors(
        val backgroundColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int
    )

    private data class ViewIds(
        val root: Int,
        val cardBg: Int,
        val examLabel: Int,
        val examCountdown: Int,
        val examTitle: Int,
        val examMeta: Int,
        val todoLabel: Int,
        val todoTitle: Int,
        val todoMeta: Int
    )

    private val LIGHT_IDS = ViewIds(
        root = R.id.widget_root,
        cardBg = R.id.card_bg_image,
        examLabel = R.id.tv_exam_label,
        examCountdown = R.id.tv_exam_countdown,
        examTitle = R.id.tv_exam_title,
        examMeta = R.id.tv_exam_meta,
        todoLabel = R.id.tv_todo_label,
        todoTitle = R.id.tv_todo_title,
        todoMeta = R.id.tv_todo_meta
    )

    private val DARK_IDS = ViewIds(
        root = R.id.widget_root_dark,
        cardBg = R.id.card_bg_image_dark,
        examLabel = R.id.tv_exam_label_dark,
        examCountdown = R.id.tv_exam_countdown_dark,
        examTitle = R.id.tv_exam_title_dark,
        examMeta = R.id.tv_exam_meta_dark,
        todoLabel = R.id.tv_todo_label_dark,
        todoTitle = R.id.tv_todo_title_dark,
        todoMeta = R.id.tv_todo_meta_dark
    )

    /**
     * 渲染并返回 2x2 日程焦点小组件的 RemoteViews 视图
     */
    fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_agenda_compact_native)

        bindRootClick(context, rv)

        val today = LocalDate.now()
        val entries = snapshot.agenda_items
            .asSequence()
            .mapNotNull { item -> toAgendaEntry(item, today) }
            .toList()

        val nextExam = entries.asSequence()
            .filter { it.kind == AgendaKind.EXAM }
            .minWithOrNull(compareBy<AgendaEntry> { it.date }.thenBy { it.sortTime })

        val todayTodo = entries.asSequence()
            .filter { it.kind == AgendaKind.TODO && it.date == today }
            .minWithOrNull(compareBy<AgendaEntry> { it.sortTime })

        val widgetStyle = snapshot.style?.widget_style
        val alphaPercent: Float = widgetStyle?.background_alpha ?: 1.0f
        val rawScale = widgetStyle?.font_scale ?: 1.0f
        val scale = if (rawScale > 0f) rawScale else 1.0f

        val lightColors = resolveContentColors(
            isDark = widgetStyle?.theme_mode == WidgetThemeModeProto.WIDGET_THEME_DARK,
            seedColorLong = widgetStyle?.seed_color
        )
        val darkColors = resolveContentColors(
            isDark = widgetStyle?.theme_mode != WidgetThemeModeProto.WIDGET_THEME_LIGHT,
            seedColorLong = widgetStyle?.seed_color
        )

        bindGroup(context, rv, LIGHT_IDS, lightColors, alphaPercent, scale, nextExam, todayTodo, ACCENT_EXAM_DAY)
        bindGroup(context, rv, DARK_IDS, darkColors, alphaPercent, scale, nextExam, todayTodo, ACCENT_EXAM_NIGHT)

        return rv
    }

    private fun bindGroup(
        context: Context,
        rv: RemoteViews,
        ids: ViewIds,
        colors: ContentColors,
        alphaPercent: Float,
        scale: Float,
        exam: AgendaEntry?,
        todayTodo: AgendaEntry?,
        examAccent: Int
    ) {
        rv.setImageViewBackground(ids.cardBg, colors.backgroundColor, alphaPercent)

        rv.setTextColor(ids.examLabel, examAccent)
        rv.setTextViewTextSize(ids.examLabel, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)

        if (exam == null) {
            rv.setViewVisibility(ids.examCountdown, View.GONE)
            rv.setTextViewText(ids.examTitle, context.getString(R.string.widget_agenda_no_exam))
            rv.setTextColor(ids.examTitle, colors.primaryTextColor)
            rv.setTextViewTextSize(ids.examTitle, TypedValue.COMPLEX_UNIT_DIP, 14f * scale)
            rv.setViewVisibility(ids.examMeta, View.GONE)
        } else {
            rv.setViewVisibility(ids.examCountdown, View.VISIBLE)
            rv.setTextViewText(
                ids.examCountdown,
                if (exam.daysLeft <= 0) {
                    context.getString(R.string.widget_title_today)
                } else {
                    context.getString(R.string.widget_agenda_days_left, exam.daysLeft)
                }
            )
            rv.setTextColor(ids.examCountdown, examAccent)
            rv.setTextViewTextSize(ids.examCountdown, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)

            rv.setTextViewText(ids.examTitle, exam.title)
            rv.setTextColor(ids.examTitle, colors.primaryTextColor)
            rv.setTextViewTextSize(ids.examTitle, TypedValue.COMPLEX_UNIT_DIP, 14f * scale)

            val examMeta = buildExamMeta(exam)
            if (examMeta.isBlank()) {
                rv.setViewVisibility(ids.examMeta, View.GONE)
            } else {
                rv.setViewVisibility(ids.examMeta, View.VISIBLE)
                rv.setTextViewText(ids.examMeta, examMeta)
                rv.setTextColor(ids.examMeta, colors.secondaryTextColor)
                rv.setTextViewTextSize(ids.examMeta, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)
            }
        }

        rv.setTextColor(ids.todoLabel, colors.secondaryTextColor)
        rv.setTextViewTextSize(ids.todoLabel, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)

        if (todayTodo == null) {
            rv.setTextViewText(ids.todoTitle, context.getString(R.string.widget_agenda_no_today_todo))
            rv.setTextColor(ids.todoTitle, colors.primaryTextColor)
            rv.setTextViewTextSize(ids.todoTitle, TypedValue.COMPLEX_UNIT_DIP, 11.5f * scale)
            rv.setViewVisibility(ids.todoMeta, View.GONE)
        } else {
            rv.setTextViewText(ids.todoTitle, todayTodo.title)
            rv.setTextColor(ids.todoTitle, colors.primaryTextColor)
            rv.setTextViewTextSize(ids.todoTitle, TypedValue.COMPLEX_UNIT_DIP, 11.5f * scale)

            val todoMeta = buildTodoMeta(todayTodo)
            if (todoMeta.isBlank()) {
                rv.setViewVisibility(ids.todoMeta, View.GONE)
            } else {
                rv.setViewVisibility(ids.todoMeta, View.VISIBLE)
                rv.setTextViewText(ids.todoMeta, todoMeta)
                rv.setTextColor(ids.todoMeta, colors.secondaryTextColor)
                rv.setTextViewTextSize(ids.todoMeta, TypedValue.COMPLEX_UNIT_DIP, 8.5f * scale)
            }
        }
    }

    private fun toAgendaEntry(item: WidgetAgendaItemProto, today: LocalDate): AgendaEntry? {
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

        return AgendaEntry(
            title = title,
            kind = kind,
            date = date,
            startTime = item.start_time,
            endTime = item.end_time,
            location = item.location,
            daysLeft = daysLeft
        )
    }

    private fun buildExamMeta(entry: AgendaEntry): String {
        val parts = mutableListOf(entry.date.format(DATE_FORMATTER))
        val time = formatTimeRange(entry.startTime, entry.endTime)
        if (time.isNotBlank()) parts += time
        if (entry.location.isNotBlank()) parts += entry.location
        return parts.joinToString(" · ")
    }

    private fun buildTodoMeta(entry: AgendaEntry): String {
        val parts = mutableListOf<String>()
        val time = formatTimeRange(entry.startTime, entry.endTime)
        if (time.isNotBlank()) parts += time
        if (entry.location.isNotBlank()) parts += entry.location
        return parts.joinToString(" · ")
    }

    private fun formatTimeRange(startTime: String, endTime: String): String {
        val start = startTime.trim()
        val end = endTime.trim()
        return when {
            start.isNotBlank() && end.isNotBlank() && end != start -> "$start-$end"
            start.isNotBlank() -> start
            else -> ""
        }
    }

    private fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (_: Exception) {
        null
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
        rv.setOnClickPendingIntent(R.id.widget_root_dark, pendingIntent)
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
            val contentColor = scheme.onPrimaryContainer.toArgb()
            ContentColors(
                backgroundColor = scheme.primaryContainer.toArgb(),
                primaryTextColor = contentColor,
                secondaryTextColor = contentColor
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