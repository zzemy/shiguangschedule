package com.xingheyuzhuan.shiguangschedule.widget.agenda

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 日程小组件独立的周期刷新调度器。
 * 不取消也不复用课程小组件的 WorkManager 任务。
 */
internal object AgendaWorkManagerHelper {

    private const val AGENDA_UPDATE_WORK_NAME = "AgendaWidgetUpdateWorker_Periodic"

    fun schedulePeriodicWork(context: Context) {
        val request = PeriodicWorkRequestBuilder<AgendaWidgetUpdateWorker>(
            15,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AGENDA_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelPeriodicWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(AGENDA_UPDATE_WORK_NAME)
    }
}