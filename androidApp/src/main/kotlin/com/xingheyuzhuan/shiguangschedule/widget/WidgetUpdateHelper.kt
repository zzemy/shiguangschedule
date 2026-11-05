package com.xingheyuzhuan.shiguangschedule.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEvent
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.ScheduleEventRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.widget.agenda.AgendaCompactNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.agenda.AgendaCompactNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.agenda.AgendaNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.agenda.AgendaNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "WidgetUpdateHelper"

// 1. 定义在顶层，避免 "local type aliases" 实验性特性报错
private typealias RenderFunc = (Context, WidgetSnapshot, Int) -> RemoteViews

/**
 * 小组件更新期间的依赖解析中心。
 * 使用 getKoinOrNull + getOrNull 逐项安全获取，任何一个 bean 缺失都不应导致整条更新链路崩溃。
 */
private object WidgetDependencies {
    inline fun <reified T : Any> getOrNull(): T? =
        runCatching { KoinPlatform.getKoinOrNull()?.getOrNull<T>() }.getOrNull()
}

/**
 * 全局防抖管理中心
 * 用于拦截拖动滑块、频繁数据监听引起的密集 updateAppWidget 调用
 */
object WidgetUpdateDebouncer {
    private var updateJob: Job? = null

    // 使用 SupervisorJob：单次刷新失败绝不能取消整个作用域，否则后续所有更新都会静默失效
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 触发带防抖功能的更新
     * @param delayMillis 触发等待间隔，默认 300ms
     */
    fun requestUpdate(context: Context, delayMillis: Long = 300L) {
        updateJob?.cancel() // 取消未完成的排队任务
        updateJob = scope.launch {
            try {
                delay(delayMillis.milliseconds)
                realUpdateAllWidgets(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "小组件刷新任务异常", e)
            }
        }
    }
}

/**
 * 小组件统一分发中心对外公开函数
 * 保持兼容原本的顶层调用接口，内部引入防抖逻辑
 */
suspend fun updateAllWidgets(context: Context) {
    WidgetUpdateDebouncer.requestUpdate(context)
}

/**
 * 在限定时间内安全读取数据，超时或读取失败均返回 null，交由调用方使用兜底值。
 * 仅透传上层协程取消，不吞掉 CancellationException。
 */
private suspend fun <T> loadOrNull(
    label: String,
    timeout: Duration,
    block: suspend () -> T?
): T? = try {
    withTimeoutOrNull(timeout) { block() }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.w(TAG, "读取${label}失败，使用兜底数据", e)
    null
}

/**
 * 实际执行全量更新的私有核心方法。
 * 任何一类数据（尤其是日程数据）读取失败，都必须保证课程数据与其他小组件继续刷新。
 */
private suspend fun realUpdateAllWidgets(context: Context) {
    val repository = WidgetDependencies.getOrNull<WidgetRepository>()
    if (repository == null) {
        Log.e(TAG, "WidgetRepository 不可用（Koin 尚未初始化），跳过本次小组件刷新")
        return
    }

    // 1. 清理历史版本遗留的虚拟考试倒计时脏数据
    try {
        repository.deleteCountdownCourses()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "清理倒计时脏数据失败，将在读取时继续过滤", e)
    }

    // 2. 准备基础数据
    val today = LocalDate.now()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val widgetWindowEnd = today.plusDays(7)

    val dbCourses = (
        loadOrNull("小组件课程", 3.seconds) {
            repository.getWidgetCoursesByDateRange(weekStart.toString(), widgetWindowEnd.toString()).first()
        } ?: emptyList()
        ).filterNot { it.id.startsWith("countdown-") }

    val currentWeek = loadOrNull("当前周数", 2.seconds) {
        repository.getCurrentWeekFlow().first()
    } ?: 0

    // 获取小组件设置（包含开学日期和周起始日）
    val appSettings = loadOrNull("小组件设置", 2.seconds) {
        repository.getAppSettingsFlow().first()
    }

    val styleSettingsRepository = WidgetDependencies.getOrNull<StyleSettingsRepository>()
    val currentStyle = if (styleSettingsRepository == null) {
        Log.w(TAG, "StyleSettingsRepository 不可用，使用默认样式")
        null
    } else {
        loadOrNull("小组件样式", 2.seconds) { styleSettingsRepository.getStyleOnce() }
    }

    val finalStyleToSync = currentStyle?.toProto() ?: ScheduleGridStyle.DEFAULT.toProto()

    // 3. 独立提取日程看板所需事件：与课程数据、样式数据彻底隔离，失败时回退到 Widget 本地库
    val agendaItems = loadAgendaItems(today, dbCourses)

    // 4. 计算开学倒计时天数
    val semesterStartDateStr = appSettings?.semesterStartDate
    val daysUntilTermStart = if (!semesterStartDateStr.isNullOrEmpty()) {
        try {
            val startDate = LocalDate.parse(semesterStartDateStr)
            ChronoUnit.DAYS.between(today, startDate).toInt()
        } catch (_: Exception) {
            0
        }
    } else {
        0
    }

    // 获取周起始日（若拿不到则兜底为 1，即周一）
    val firstDayOfWeek = appSettings?.firstDayOfWeek ?: 1

    // 5. 构造数据快照 (Protobuf)
    val courseProtoList = dbCourses.map { course ->
        WidgetCourseProto(
            id = course.id,
            name = course.name,
            teacher = course.teacher,
            position = course.position,
            start_time = course.startTime,
            end_time = course.endTime,
            color_int = course.colorInt,
            is_skipped = course.isSkipped,
            date = course.date
        )
    }

    val snapshot = WidgetSnapshot(
        current_week = currentWeek,
        style = finalStyleToSync,
        courses = courseProtoList,
        days_until_term_start = daysUntilTermStart,
        first_day_of_week = firstDayOfWeek,
        total_weeks = appSettings?.semesterTotalWeeks ?: 0,
        agenda_items = agendaItems
    )

    // 6. 定义所有原生尺寸的映射列表
    val appWidgetManager = AppWidgetManager.getInstance(context)

    val nativeConfigs: List<Pair<Class<*>, RenderFunc>> = listOf(
        TinyNativeProvider::class.java to { ctx, snap, id -> TinyNativeRenderer.render(ctx, snap, id) },
        CompactNativeProvider::class.java to { ctx, snap, id -> CompactNativeRenderer.render(ctx, snap, id) },
        DoubleDaysNativeProvider::class.java to { ctx, snap, id -> DoubleDaysNativeRenderer.render(ctx, snap, id) },
        ListVerticalNativeProvider::class.java to { ctx, snap, id -> ListVerticalNativeRenderer.render(ctx, snap, id) },
        AgendaNativeProvider::class.java to { ctx, snap, id -> AgendaNativeRenderer.render(ctx, snap, id) },
        AgendaCompactNativeProvider::class.java to { ctx, snap, id -> AgendaCompactNativeRenderer.render(ctx, snap, id) }
    )

    // 7. 统一分发更新：每个规格、每个实例之间互不阻塞
    nativeConfigs.forEachIndexed { index, (providerClass, renderFunc) ->
        try {
            val componentName = ComponentName(context, providerClass)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isEmpty()) return@forEachIndexed

            if (index > 0) {
                delay(300.milliseconds)
            }

            ids.forEach { appWidgetId ->
                try {
                    val remoteViews = renderFunc(context, snapshot, appWidgetId)
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "规格 ${providerClass.simpleName} 实例 $appWidgetId 渲染失败", e)
                }
            }

            Log.d(TAG, "成功刷新规格 ${providerClass.simpleName}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "规格 ${providerClass.simpleName} 刷新流程异常", e)
        }
    }
}

/**
 * 读取日程看板数据。
 * 优先读取主数据库；主数据库仓库不可用或查询失败时，回退到已同步到 Widget 本地库的考试/待办/补课记录，
 * 确保日程小组件在任何情况下都有可渲染的数据（至少是空状态，而不是停留在 initialLayout）。
 */
private suspend fun loadAgendaItems(
    today: LocalDate,
    fallbackCourses: List<WidgetCourse>
): List<WidgetAgendaItemProto> {
    // 返回 null 表示主数据库不可用，需要回退到 Widget 本地数据；
    // 返回空列表表示主数据库正常但当前确实没有日程，不应再使用旧的本地缓存。
    val mainDatabaseItems: List<WidgetAgendaItemProto>? = try {
        val appSettingsRepository = WidgetDependencies.getOrNull<AppSettingsRepository>()
        val scheduleEventRepository = WidgetDependencies.getOrNull<ScheduleEventRepository>()

        if (appSettingsRepository == null || scheduleEventRepository == null) {
            Log.w(TAG, "日程数据仓库不可用，改用 Widget 本地数据")
            null
        } else {
            val tableId = loadOrNull("当前课表 ID", 2.seconds) {
                appSettingsRepository.getAppSettingsOnce().currentCourseTableId
            }.orEmpty()

            if (tableId.isBlank()) {
                emptyList()
            } else {
                loadOrNull("日程看板事件", 3.seconds) {
                    scheduleEventRepository
                        .getUpcomingAgendaEventsFlow(tableId, today.toString())
                        .first()
                }?.mapNotNull { event -> event.toAgendaItemProto(today) }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "读取主数据库日程失败，改用 Widget 本地数据", e)
        null
    }

    val items = mainDatabaseItems ?: fallbackCourses.mapNotNull { course -> course.toAgendaItemProto(today) }

    return items.sortedWith(
        compareBy<WidgetAgendaItemProto> { it.date }.thenBy { it.start_time.ifBlank { "99:99" } }
    )
}

private fun ScheduleEvent.toAgendaItemProto(today: LocalDate): WidgetAgendaItemProto? {
    val eventDate = parseDate(date) ?: return null
    val daysLeft = ChronoUnit.DAYS.between(today, eventDate).toInt()
    if (daysLeft < 0) return null

    return WidgetAgendaItemProto(
        id = id,
        title = title,
        type = when (type) {
            "EXAM" -> "EXAM"
            "DAILY" -> "DAILY"
            else -> "CUSTOM"
        },
        date = date,
        start_time = startTime.orEmpty(),
        end_time = endTime.orEmpty(),
        location = listOfNotNull(location, detail)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" · "),
        days_left = daysLeft
    )
}

/**
 * 将 Widget 本地库中的考试/补课/待办记录转换为日程看板条目。
 * 普通课程不会被纳入日程看板。
 */
private fun WidgetCourse.toAgendaItemProto(today: LocalDate): WidgetAgendaItemProto? {
    val type = when {
        id.startsWith("exam-") -> "EXAM"
        id.startsWith("daily-") -> "DAILY"
        id.startsWith("makeup-") -> "CUSTOM"
        else -> return null
    }

    val eventDate = parseDate(date) ?: return null
    val daysLeft = ChronoUnit.DAYS.between(today, eventDate).toInt()
    if (daysLeft < 0) return null

    val cleanTitle = name
        .removePrefix("📝 [考试] ")
        .removePrefix("🔄 [补课] ")
        .removePrefix("📌 [待办] ")
        .trim()
    if (cleanTitle.isBlank()) return null

    return WidgetAgendaItemProto(
        id = id,
        title = cleanTitle,
        type = type,
        date = date,
        start_time = startTime,
        end_time = endTime,
        location = position,
        days_left = daysLeft
    )
}

private fun parseDate(value: String): LocalDate? = try {
    LocalDate.parse(value)
} catch (_: Exception) {
    null
}