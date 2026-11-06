package com.xingheyuzhuan.shiguangschedule.widget.agenda

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

/**
 * 日程与考试待办时间线小组件接收器（4x2）。
 * 仅触发日程小组件链路，不触碰课程小组件更新。
 */
internal class AgendaNativeProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        AgendaWidgetUpdateDebouncer.requestUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        AgendaWidgetUpdateDebouncer.requestUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        AgendaWorkManagerHelper.schedulePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        AgendaWorkManagerHelper.cancelPeriodicWork(context)
    }
}