package com.xingheyuzhuan.shiguangschedule.widget.agenda

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.xingheyuzhuan.shiguangschedule.data.model.WidgetStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.ScheduleEventRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private object AgendaWidgetDependencies : KoinComponent {
    val appSettingsRepository: AppSettingsRepository by inject()
    val scheduleEventRepository: ScheduleEventRepository by inject()
    val styleSettingsRepository: StyleSettingsRepository by inject()
}

/**
 * 日程小组件防抖更新入口，避免日程数据快速变更时密集触发 RemoteViews 刷新。
 */
object AgendaWidgetUpdateDebouncer {
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun requestUpdate(context: Context, delayMillis: Long = 250L) {
        updateJob?.cancel()
        val appContext = context.applicationContext
        updateJob = scope.launch {
            delay(delayMillis.milliseconds)
            updateAllAgendaWidgets(appContext)
        }
    }
}

/**
 * 独立于课程小组件的日程小组件刷新链路。
 *
 * 数据直接来自 ScheduleEventRepository（主数据库 schedule_events），
 * 不读取也不写入 WidgetCourse 数据库，因此不会影响课程小组件的“今日剩余课数”。
 */
suspend fun updateAllAgendaWidgets(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)

    val timelineIds = appWidgetManager.getAppWidgetIds(
        ComponentName(context, AgendaNativeProvider::class.java)
    )
    val compactIds = appWidgetManager.getAppWidgetIds(
        ComponentName(context, AgendaCompactNativeProvider::class.java)
    )

    if (timelineIds.isEmpty() && compactIds.isEmpty()) return

    val today = LocalDate.now()
    val appSettings = withTimeoutOrNull(2.seconds) {
        AgendaWidgetDependencies.appSettingsRepository.getAppSettingsOnce()
    }
    val tableId = appSettings?.currentCourseTableId.orEmpty()

    val events = if (tableId.isBlank()) {
        emptyList()
    } else {
        withTimeoutOrNull(3.seconds) {
            AgendaWidgetDependencies.scheduleEventRepository
                .getUpcomingAgendaEventsFlow(tableId, today.toString())
                .first()
        } ?: emptyList()
    }

    val widgetStyle = withTimeoutOrNull(2.seconds) {
        AgendaWidgetDependencies.styleSettingsRepository.getStyleOnce().widgetStyle
    } ?: WidgetStyle.DEFAULT

    val data = AgendaWidgetDataFactory.create(
        events = events,
        today = today,
        widgetStyle = widgetStyle
    )

    timelineIds.forEach { appWidgetId ->
        runCatching {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                AgendaNativeRenderer.render(context, data, appWidgetId)
            )
        }.onFailure {
            Log.e("AgendaWidgetUpdate", "4x2 日程小组件渲染失败: $appWidgetId", it)
        }
    }

    compactIds.forEach { appWidgetId ->
        runCatching {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                AgendaCompactNativeRenderer.render(context, data, appWidgetId)
            )
        }.onFailure {
            Log.e("AgendaWidgetUpdate", "2x2 日程小组件渲染失败: $appWidgetId", it)
        }
    }
}