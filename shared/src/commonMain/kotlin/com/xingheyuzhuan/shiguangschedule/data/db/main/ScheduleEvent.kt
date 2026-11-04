package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room 实体类，代表“日程事件”数据表。
 * 统一存储每日日程、补课（自定义加课）与考试日程，并与 `CourseTable` 关联。
 */
@Entity(
    tableName = "schedule_events",
    foreignKeys = [ForeignKey(
        entity = CourseTable::class,
        parentColumns = ["id"],
        childColumns = ["courseTableId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["courseTableId"])]
)
data class ScheduleEvent(
    @PrimaryKey
    val id: String, // 日程事件的唯一标识符 UUID
    val courseTableId: String, // 所属课表 ID
    val title: String, // 标题，例如 "数学期末考" / "交微积分作业"
    val type: String, // 事件类型："DAILY"、"EXAM"、"MAKEUP"
    val date: String, // 日期，格式为 "YYYY-MM-DD"
    val startTime: String? = null, // 开始时间，格式为 "HH:MM"
    val endTime: String? = null, // 结束时间，格式为 "HH:MM"
    val location: String? = null, // 地点（教室 / 考场）
    val detail: String? = null, // 详情（座位号 / 备注）
    val isCompleted: Boolean = false, // 每日待办是否已完成
    val colorInt: Int? = null // 事件颜色索引
)