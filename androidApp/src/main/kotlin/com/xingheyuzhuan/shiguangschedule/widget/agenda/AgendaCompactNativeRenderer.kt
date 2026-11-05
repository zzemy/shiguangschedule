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
import com.xingheyuzhuan.shiguangschedule.widget.WidgetAgendaItemProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 日程与考试近期焦点 2x2 小组件渲染器
 * 上半部展示最近考试倒计时，下半部展示最近一条待办/日程。
 *
 * 布局的 layout/ 与 layout-night/ 必须使用同一套 View ID，
 * 这里只绑定这一套 ID，避免宿主 inflate 后找不到另一套 ID 触发 ActionException。
 */
object AgendaCompactNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")

    private enum class AgendaKind(val accent: AgendaAccent) {
        EXAM(AgendaPalette.exam),
        TODO(AgendaPalette.todo),
        SCHEDULE(AgendaPalette.schedule)
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
        val secondaryTextColor: Int,
        val dividerColor: Int
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

        // 优先展示最近的待办/日程；没有时显示空状态
        val nearestTodo = entries.asSequence()
            .filter { it.kind != AgendaKind.EXAM }
            .minWithOrNull(compareBy<AgendaEntry> { it.date }.thenBy { it.sortTime })

        val widgetStyle = snapshot.style?.widget_style
        val isDark = resolveWidgetIsDark(context, widgetStyle?.theme_mode)
        val colors = resolveContentColors(isDark, widgetStyle?.seed_color)
        val alphaPercent = (widgetStyle?.background_alpha ?: 1.0f).coerceIn(0.0f, 1.0f)
        val rawScale = widgetStyle?.font_scale ?: 1.0f
        val scale = if (rawScale > 0f) rawScale else 1.0f

        bindWidget(context, rv, today, isDark, colors, alphaPercent, scale, nextExam, nearestTodo)

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
        exam: AgendaEntry?,
        nearestTodo: AgendaEntry?
    ) {
        rv.setImageViewBackground(R.id.card_bg_image, colors.backgroundColor, alphaPercent)
        // divider 颜色已在 XML 布局中通过主题资源静态指定，无需通过非法反射方法动态设置

        // 顶部：分区标题与倒计时胶囊
        val examAccent = AgendaPalette.exam.textColor(isDark)
        rv.setTextColor(R.id.tv_exam_label, examAccent)
        rv.setTextViewTextSize(R.id.tv_exam_label, TypedValue.COMPLEX_UNIT_DIP, 10.5f * scale)

        if (exam == null) {
            rv.setViewVisibility(R.id.tv_exam_countdown, View.GONE)
            rv.setTextViewText(R.id.tv_exam_title, context.getString(R.string.widget_agenda_no_exam))
            rv.setTextColor(R.id.tv_exam_title, colors.primaryTextColor)
            rv.setTextViewTextSize(R.id.tv_exam_title, TypedValue.COMPLEX_UNIT_DIP, 14f * scale)
            rv.setViewVisibility(R.id.tv_exam_meta, View.GONE)
        } else {
            rv.setViewVisibility(R.id.tv_exam_countdown, View.VISIBLE)
            rv.setInt(
                R.id.tv_exam_countdown,
                "setBackgroundResource",
                AgendaPalette.pillBackgroundRes(isDark)
            )
            rv.setTextViewText(R.id.tv_exam_countdown, countdownText(context, exam.daysLeft))
            rv.setTextColor(R.id.tv_exam_countdown, examAccent)
            rv.setTextViewTextSize(R.id.tv_exam_countdown, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)

            rv.setTextViewText(R.id.tv_exam_title, exam.title)
            rv.setTextColor(R.id.tv_exam_title, colors.primaryTextColor)
            rv.setTextViewTextSize(R.id.tv_exam_title, TypedValue.COMPLEX_UNIT_DIP, 16f * scale)

            val examMeta = buildExamMeta(exam)
            if (examMeta.isBlank()) {
                rv.setViewVisibility(R.id.tv_exam_meta, View.GONE)
            } else {
                rv.setViewVisibility(R.id.tv_exam_meta, View.VISIBLE)
                rv.setTextViewText(R.id.tv_exam_meta, examMeta)
                rv.setTextColor(R.id.tv_exam_meta, colors.secondaryTextColor)
                rv.setTextViewTextSize(R.id.tv_exam_meta, TypedValue.COMPLEX_UNIT_DIP, 10.5f * scale)
            }
        }

        // 底部：最近待办 / 日程，无数据时保留结构并显示弱化占位
        val todo = nearestTodo
        if (todo == null) {
            rv.setViewVisibility(R.id.container_todo_footer, View.VISIBLE)
            rv.setInt(
                R.id.tv_todo_label,
                "setBackgroundResource",
                AgendaPalette.todo.backgroundRes(isDark)
            )
            rv.setTextViewText(R.id.tv_todo_label, context.getString(R.string.widget_agenda_badge_todo))
            rv.setTextColor(R.id.tv_todo_label, AgendaPalette.todo.textColor(isDark))
            rv.setTextViewTextSize(R.id.tv_todo_label, TypedValue.COMPLEX_UNIT_DIP, 9f * scale)

            rv.setTextViewText(R.id.tv_todo_title, context.getString(R.string.widget_agenda_focus_no_todo))
            rv.setTextColor(R.id.tv_todo_title, colors.secondaryTextColor)
            rv.setTextViewTextSize(R.id.tv_todo_title, TypedValue.COMPLEX_UNIT_DIP, 11f * scale)
            rv.setViewVisibility(R.id.tv_todo_meta, View.GONE)
        } else {
            val accent = todo.kind.accent
            rv.setViewVisibility(R.id.container_todo_footer, View.VISIBLE)
            rv.setInt(R.id.tv_todo_label, "setBackgroundResource", accent.backgroundRes(isDark))
            rv.setTextViewText(R.id.tv_todo_label, context.getString(badgeTextRes(todo.kind)))
            rv.setTextColor(R.id.tv_todo_label, accent.textColor(isDark))
            rv.setTextViewTextSize(R.id.tv_todo_label, TypedValue.COMPLEX_UNIT_DIP, 9f * scale)

            rv.setTextViewText(R.id.tv_todo_title, todo.title)
            rv.setTextColor(R.id.tv_todo_title, colors.primaryTextColor)
            rv.setTextViewTextSize(R.id.tv_todo_title, TypedValue.COMPLEX_UNIT_DIP, 11.5f * scale)

            val todoMeta = buildTodoMeta(context, todo, today)
            if (todoMeta.isBlank()) {
                rv.setViewVisibility(R.id.tv_todo_meta, View.GONE)
            } else {
                rv.setViewVisibility(R.id.tv_todo_meta, View.VISIBLE)
                rv.setTextViewText(R.id.tv_todo_meta, todoMeta)
                rv.setTextColor(R.id.tv_todo_meta, colors.secondaryTextColor)
                rv.setTextViewTextSize(R.id.tv_todo_meta, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)
            }
        }

        // 空状态：考试与待办都没有时整卡居中展示
        val isEmpty = exam == null && todo == null
        rv.setViewVisibility(R.id.container_content, if (isEmpty) View.GONE else View.VISIBLE)
        rv.setViewVisibility(R.id.container_empty, if (isEmpty) View.VISIBLE else View.GONE)
        rv.setTextViewText(R.id.tv_empty_title, context.getString(R.string.widget_agenda_focus_empty))
        rv.setTextColor(R.id.tv_empty_title, colors.primaryTextColor)
        rv.setTextViewTextSize(R.id.tv_empty_title, TypedValue.COMPLEX_UNIT_DIP, 12.5f * scale)
        rv.setTextViewText(R.id.tv_empty_hint, context.getString(R.string.widget_agenda_empty_hint))
        rv.setTextColor(R.id.tv_empty_hint, colors.secondaryTextColor)
        rv.setTextViewTextSize(R.id.tv_empty_hint, TypedValue.COMPLEX_UNIT_DIP, 9.5f * scale)
    }

    private fun badgeTextRes(kind: AgendaKind): Int = when (kind) {
        AgendaKind.EXAM -> R.string.widget_agenda_badge_exam
        AgendaKind.TODO -> R.string.widget_agenda_badge_todo
        AgendaKind.SCHEDULE -> R.string.widget_agenda_badge_schedule
    }

    private fun countdownText(context: Context, daysLeft: Int): String =
        if (daysLeft <= 0) {
            context.getString(R.string.widget_title_today)
        } else {
            context.getString(R.string.widget_agenda_days_left, daysLeft)
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
        if (entry.location.isNotBlank()) parts += entry.location.trim()
        return parts.joinToString(" · ")
    }

    private fun buildTodoMeta(context: Context, entry: AgendaEntry, today: LocalDate): String =
        if (entry.date == today) {
            entry.startTime.trim().ifBlank { context.getString(R.string.widget_title_today) }
        } else {
            entry.date.format(DATE_FORMATTER)
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