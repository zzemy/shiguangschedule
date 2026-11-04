package com.xingheyuzhuan.shiguangschedule.data.api.exam

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * 零依赖的轻量 ICS 日历解析器，用于离线导入考试日程。
 *
 * 仅提取 VEVENT 中的 SUMMARY / DTSTART / DTEND / LOCATION / DESCRIPTION，
 * 支持 RFC 5545 的行折叠、属性参数、UTC 时间与文本转义。
 */
object IcsExamParser {

    private val DATE_TIME_REGEX =
        Regex("""^(\d{4})(\d{2})(\d{2})(?:T(\d{2})(\d{2})(\d{2})?)?(Z)?$""")

    private val SUPPORTED_PROPERTIES = setOf(
        "SUMMARY", "DTSTART", "DTEND", "LOCATION", "DESCRIPTION"
    )

    /**
     * 解析 .ics 文本内容，返回按日期与开始时间排序的考试事件列表。
     * 无法识别日期或标题的事件会被忽略。
     */
    fun parse(content: String): List<ExamImportItem> {
        if (content.isBlank()) return emptyList()

        val events = mutableListOf<ExamImportItem>()
        var properties: MutableMap<String, String>? = null
        var inAlarm = false

        unfoldLines(content).forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            when (line.uppercase()) {
                "BEGIN:VEVENT" -> {
                    properties = linkedMapOf()
                    inAlarm = false
                    return@forEach
                }

                "END:VEVENT" -> {
                    properties?.let(::toExamItem)?.let(events::add)
                    properties = null
                    inAlarm = false
                    return@forEach
                }

                "BEGIN:VALARM" -> {
                    inAlarm = true
                    return@forEach
                }

                "END:VALARM" -> {
                    inAlarm = false
                    return@forEach
                }
            }

            val currentProperties = properties ?: return@forEach
            if (inAlarm) return@forEach

            val separator = line.indexOf(':')
            if (separator <= 0) return@forEach

            val header = line.substring(0, separator)
            val propertyName = header.substringBefore(';').uppercase()
            if (propertyName !in SUPPORTED_PROPERTIES) return@forEach

            if (propertyName !in currentProperties) {
                currentProperties[propertyName] = line.substring(separator + 1)
            }
        }

        return events.sortedWith(
            compareBy<ExamImportItem> { it.date }.thenBy { it.startTime ?: "99:99" }
        )
    }

    /**
     * 处理 RFC 5545 的行折叠：以空格或制表符开头的物理行属于上一行。
     */
    private fun unfoldLines(content: String): List<String> {
        val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
        val result = ArrayList<String>()

        normalized.split('\n').forEach { line ->
            if (line.isNotEmpty() && (line[0] == ' ' || line[0] == '\t') && result.isNotEmpty()) {
                result[result.lastIndex] = result.last() + line.substring(1)
            } else {
                result.add(line)
            }
        }

        return result
    }

    private fun toExamItem(properties: Map<String, String>): ExamImportItem? {
        val start = properties["DTSTART"]?.let(::parseDateTime) ?: return null
        val end = properties["DTEND"]?.let(::parseDateTime)

        val title = properties["SUMMARY"]
            ?.let(::unescape)
            ?.trim()
            .orEmpty()
            .ifBlank { "未命名考试" }

        return ExamImportItem(
            courseName = title,
            date = start.date,
            startTime = start.time,
            endTime = end?.time,
            location = properties["LOCATION"]
                ?.let(::unescape)
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            note = properties["DESCRIPTION"]
                ?.let(::unescape)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        )
    }

    private fun parseDateTime(raw: String): IcsDateTime? {
        val match = DATE_TIME_REGEX.matchEntire(raw.trim()) ?: return null

        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        val date = runCatching { LocalDate(year, month, day) }.getOrNull() ?: return null

        val hourText = match.groupValues[4]
        if (hourText.isEmpty()) {
            return IcsDateTime(date.toString(), null)
        }

        val hour = hourText.toIntOrNull() ?: return null
        val minute = match.groupValues[5].toIntOrNull() ?: 0
        val second = match.groupValues[6].toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null

        val isUtc = match.groupValues[7].isNotEmpty()
        if (isUtc) {
            val utcDateTime = LocalDateTime(date, LocalTime(hour, minute, second))
            val localDateTime = utcDateTime
                .toInstant(TimeZone.UTC)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            return IcsDateTime(
                date = localDateTime.date.toString(),
                time = "${pad2(localDateTime.hour)}:${pad2(localDateTime.minute)}"
            )
        }

        return IcsDateTime(
            date = date.toString(),
            time = "${pad2(hour)}:${pad2(minute)}"
        )
    }

    /**
     * 处理 ICS 文本转义：`\\n`、`\\,`、`\\;`、`\\\\`。
     */
    private fun unescape(value: String): String {
        val builder = StringBuilder(value.length)
        var index = 0

        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    'n', 'N' -> builder.append('\n')
                    else -> builder.append(next)
                }
                index += 2
            } else {
                builder.append(char)
                index++
            }
        }

        return builder.toString()
    }

    private fun pad2(value: Int): String = value.toString().padStart(2, '0')

    private data class IcsDateTime(
        val date: String,
        val time: String?
    )
}