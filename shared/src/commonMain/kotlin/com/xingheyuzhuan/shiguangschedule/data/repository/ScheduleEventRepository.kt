package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEvent
import com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEventDao
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

/**
 * 日程事件数据仓库，负责每日日程、补课与考试日程的数据操作。
 */
@Single
class ScheduleEventRepository(
    private val scheduleEventDao: ScheduleEventDao
) {

    /**
     * 获取指定课表、指定日期的日程事件。
     */
    fun getEventsForDateFlow(courseTableId: String, date: String): Flow<List<ScheduleEvent>> {
        return scheduleEventDao.getEventsForDateFlow(courseTableId, date)
    }

    /**
     * 获取指定课表从指定日期开始的考试日程。
     */
    fun getUpcomingExamsFlow(courseTableId: String, fromDate: String): Flow<List<ScheduleEvent>> {
        return scheduleEventDao.getUpcomingExamsFlow(courseTableId, fromDate)
    }

    /**
     * 获取指定课表从指定日期开始的日程看板事件（考试、补课/日程与未完成待办）。
     */
    fun getUpcomingAgendaEventsFlow(courseTableId: String, fromDate: String): Flow<List<ScheduleEvent>> {
        return scheduleEventDao.getUpcomingAgendaEventsFlow(courseTableId, fromDate)
    }

    /**
     * 获取指定课表在日期范围 [startDate, endDateExclusive) 内的补课与考试事件。
     */
    fun getEventsInRangeFlow(
        courseTableId: String,
        startDate: String,
        endDateExclusive: String
    ): Flow<List<ScheduleEvent>> {
        return scheduleEventDao.getEventsInRangeFlow(courseTableId, startDate, endDateExclusive)
    }

    /**
     * 插入一个日程事件。
     */
    suspend fun insertEvent(event: ScheduleEvent) {
        scheduleEventDao.insertEvent(event)
    }

    /**
     * 批量插入日程事件。
     */
    suspend fun insertEvents(events: List<ScheduleEvent>) {
        scheduleEventDao.insertEvents(events)
    }

    /**
     * 更新一个日程事件。
     */
    suspend fun updateEvent(event: ScheduleEvent) {
        scheduleEventDao.updateEvent(event)
    }

    /**
     * 删除一个日程事件。
     */
    suspend fun deleteEvent(event: ScheduleEvent) {
        scheduleEventDao.deleteEvent(event)
    }
}