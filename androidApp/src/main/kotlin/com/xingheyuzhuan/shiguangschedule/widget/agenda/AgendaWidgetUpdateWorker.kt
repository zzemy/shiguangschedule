package com.xingheyuzhuan.shiguangschedule.widget.agenda

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.android.annotation.KoinWorker

/**
 * 定时刷新日程小组件的 Worker。
 * 与课程小组件的 WidgetUiUpdateWorker 完全独立，只读取日程事件数据。
 */
@KoinWorker
class AgendaWidgetUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AgendaWidgetUpdate", "AgendaWidgetUpdateWorker 开始执行")
        return try {
            updateAllAgendaWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("AgendaWidgetUpdate", "AgendaWidgetUpdateWorker 执行失败", e)
            Result.retry()
        }
    }
}