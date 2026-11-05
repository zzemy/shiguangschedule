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
     * 获取指定课表在指定日期之后的考试日程，按日期和开始时间排序。
     */
    @Query(
        "SELECT * FROM schedule_events " +
            "WHERE courseTableId = :courseTableId AND type = 'EXAM' AND date >= :fromDate " +
            "ORDER BY date ASC, startTime ASC"
    )
    fun getUpcomingExamsFlow(courseTableId: String, fromDate: String): Flow<List<ScheduleEvent>>

    /**
     * 获取指定课表在指定日期之后的日程看板事件：
     * 包含全部考试、补课/日程，以及未完成的每日待办。按日期和开始时间排序。
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