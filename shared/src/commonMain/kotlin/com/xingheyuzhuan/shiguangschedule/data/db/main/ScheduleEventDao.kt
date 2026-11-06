package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作日程事件 (ScheduleEvent) 数据表。
 */
@Dao
interface ScheduleEventDao {

    /**
     * 获取指定课表、指定日期的事件，按开始时间排序。
     */
    @Query(
        "SELECT * FROM schedule_events " +
            "WHERE courseTableId = :courseTableId AND date = :date " +
            "ORDER BY startTime ASC"
    )
    fun getEventsForDateFlow(courseTableId: String, date: String): Flow<List<ScheduleEvent>>

    /**
     * 按主键获取单个日程事件，用于区分周课表中的伪课程与真实课程。
     */
    @Query("SELECT * FROM schedule_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): ScheduleEvent?

    /**
     * 获取指定课表在指定日期之后的补课事件，按日期和开始时间排序。
     */
    @Query(
        "SELECT * FROM schedule_events " +
            "WHERE courseTableId = :courseTableId AND type = 'MAKEUP' AND date > :fromDate " +
            "ORDER BY date ASC, startTime ASC"
    )
    fun getUpcomingMakeupsFlow(courseTableId: String, fromDate: String): Flow<List<ScheduleEvent>>

    /**
     * 获取指定课表在指定日期之后的考试日程，按日期和开始时间排序。
     */
    @Query(
        "SELECT * FROM schedule_events " +
            "WHERE courseTableId = :courseTableId AND type = 'EXAM' AND date >= :fromDate " +
            "ORDER BY date ASC, startTime ASC"
    )
    fun getUpcomingExamsFlow(courseTableId: String, fromDate: String): Flow<List<ScheduleEvent>>

    /**
     * 获取指定课表从指定日期开始的日程看板事件（考试、补课与每日待办）。
     * 仅供日程小组件与相关刷新链路使用，不得写入课程小组件数据库。
     */
    @Query(
        "SELECT * FROM schedule_events " +
            "WHERE courseTableId = :courseTableId AND date >= :fromDate " +
            "AND (type IN ('EXAM', 'MAKEUP') OR (type = 'DAILY' AND isCompleted = 0)) " +
            "ORDER BY date ASC, startTime ASC"
    )
    fun getUpcomingAgendaEventsFlow(courseTableId: String, fromDate: String): Flow<List<ScheduleEvent>>

    /**
     * 获取指定课表在日期范围 [startDate, endDateExclusive) 内的补课与考试事件，
     * 仅返回同时具有起止时间的事件。按日期和开始时间排序。
     */
    @Query(
        "SELECT * FROM schedule_events " +
            "WHERE courseTableId = :courseTableId AND type IN ('MAKEUP', 'EXAM') " +
            "AND date >= :startDate AND date < :endDateExclusive " +
            "AND startTime IS NOT NULL AND endTime IS NOT NULL " +
            "ORDER BY date ASC, startTime ASC"
    )
    fun getEventsInRangeFlow(
        courseTableId: String,
        startDate: String,
        endDateExclusive: String
    ): Flow<List<ScheduleEvent>>

    /**
     * 插入一个日程事件。如果发生主键冲突，则替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ScheduleEvent)

    /**
     * 批量插入日程事件。主键冲突时替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ScheduleEvent>)

    /**
     * 更新一个现有日程事件。
     */
    @Update
    suspend fun updateEvent(event: ScheduleEvent)

    /**
     * 删除一个日程事件。
     */
    @Delete
    suspend fun deleteEvent(event: ScheduleEvent)
}