package com.xingheyuzhuan.shiguangschedule.data.api.exam

/**
 * 通用考试导入数据模型。
 *
 * 供南邮 StuDev 在线同步与离线 .ics 解析共用，
 * UI 层可将其转换为 [com.xingheyuzhuan.shiguangschedule.data.db.main.ScheduleEvent] 后落库。
 */
data class ExamImportItem(
    val courseName: String,
    val date: String, // YYYY-MM-DD
    val startTime: String? = null, // HH:mm
    val endTime: String? = null, // HH:mm
    val location: String? = null,
    val teacher: String? = null,
    val note: String? = null,
    val campus: String? = null
)