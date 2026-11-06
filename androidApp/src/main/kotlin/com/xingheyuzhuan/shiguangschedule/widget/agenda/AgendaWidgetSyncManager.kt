package com.xingheyuzhuan.shiguangschedule.widget.agenda

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEvent
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.ScheduleEventRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

/**
 * 日程小组件的独立数据变更监听器。
 *
 * 主数据库中的日程/待办/考试变化会直接触发日程小组件刷新；
 * 不依赖 WidgetDataSynchronizer，也不会触发课程小组件数据库重建。
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Single(createdAtStart = true)
class AgendaWidgetSyncManager(
    appContext: Context,
    appSettingsRepository: AppSettingsRepository,
    scheduleEventRepository: ScheduleEventRepository,
    styleSettingsRepository: StyleSettingsRepository
) {
    private val appContext = appContext.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        appSettingsRepository.getAppSettings()
            .map { it.currentCourseTableId }
            .distinctUntilChanged()
            .flatMapLatest { tableId ->
                if (tableId.isBlank()) {
                    flowOf(emptyList<ScheduleEvent>())
                } else {
                    scheduleEventRepository.getUpcomingAgendaEventsFlow(
                        tableId,
                        LocalDate.now().toString()
                    )
                }
            }
            .debounce(400.milliseconds)
            .onEach {
                AgendaWidgetUpdateDebouncer.requestUpdate(appContext)
            }
            .launchIn(scope)

        styleSettingsRepository.styleFlow
            .debounce(400.milliseconds)
            .onEach {
                AgendaWidgetUpdateDebouncer.requestUpdate(appContext)
            }
            .launchIn(scope)
    }
}