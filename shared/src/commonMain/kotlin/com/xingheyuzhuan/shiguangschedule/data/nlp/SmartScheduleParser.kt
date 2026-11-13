package com.xingheyuzhuan.shiguangschedule.data.nlp

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * 轻量级自然语言日程解析器。
 *
 * 支持“明天下午五点去教三 206 上课”“下周三上午9:50高数期末考试在教2-410”
 * “今晚8点半交操作系统作业”“第10周 周三 1到2节 计网补课”等口语化输入，
 * 并提取标题、类型、日期、起止时间与地点。
 *
 * 周次（第10周、第8到10周）会结合 [semesterStartDate] 或 [currentWeek] 推算具体日期；
 * 节次（第1到2节）会在未指定具体钟点时映射为标准作息时间。
 */
object SmartScheduleParser {

    const val TYPE_DAILY = "DAILY"
    const val TYPE_EXAM = "EXAM"
    const val TYPE_MAKEUP = "MAKEUP"

    data class ParseResult(
        val title: String,
        val type: String,
        val date: LocalDate,
        val startTime: String?,
        val endTime: String?,
        val location: String?,
        val rawInput: String,
        val hasExplicitDate: Boolean = false,
        val teacher: String? = null
    ) {
        val isAllDay: Boolean get() = startTime.isNullOrBlank()
    }

    private enum class TimePeriod { MORNING, NOON, AFTERNOON, EVENING }

    private data class TimeToken(
        val range: IntRange,
        val hour: Int,
        val minute: Int,
        val period: TimePeriod?
    )

    private data class LocationMatch(
        val value: String,
        val removalRange: IntRange?
    )

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MAX_WEEK = 60

    /** 口语化数字：阿拉伯数字或中文数字（十、十二、二十、两等）。 */
    private const val NUMBER_TOKEN =
        """(?:\d{1,2}|[零〇一二三四五六七八九十两]{1,3})"""
    private const val RANGE_CONNECTOR =
        """(?:-+|－+|—+|–+|~+|～+|至|到)"""

    /** 南邮标准节次作息（分钟），索引 0 对应第 1 节。 */
    private val SECTION_TIME_RANGES = listOf(
        8 * 60 to 8 * 60 + 45,          // 第 1 节 08:00-08:45
        8 * 60 + 50 to 9 * 60 + 35,     // 第 2 节 08:50-09:35
        9 * 60 + 50 to 10 * 60 + 35,    // 第 3 节 09:50-10:35
        10 * 60 + 40 to 11 * 60 + 25,   // 第 4 节 10:40-11:25
        11 * 60 + 30 to 12 * 60 + 15,   // 第 5 节 11:30-12:15
        14 * 60 to 14 * 60 + 45,        // 第 6 节 14:00-14:45
        14 * 60 + 50 to 15 * 60 + 35,   // 第 7 节 14:50-15:35
        15 * 60 + 45 to 16 * 60 + 30,   // 第 8 节 15:45-16:30
        16 * 60 + 35 to 17 * 60 + 20,   // 第 9 节 16:35-17:20
        18 * 60 + 30 to 19 * 60 + 15,   // 第 10 节 18:30-19:15
        19 * 60 + 20 to 20 * 60 + 5,    // 第 11 节 19:20-20:05
        20 * 60 + 10 to 20 * 60 + 55    // 第 12 节 20:10-20:55
    )

    private val WEEK_RANGE_REGEX = Regex(
        """(?:第\s*)?($NUMBER_TOKEN)\s*(?:周)?\s*$RANGE_CONNECTOR\s*""" +
            """(?:第\s*)?($NUMBER_TOKEN)\s*周(?:次)?"""
    )
    private val WEEK_SINGLE_REGEX = Regex(
        """(?:第\s*)?($NUMBER_TOKEN)\s*周(?:次)?"""
    )
    private val WEEKDAY_RANGE_REGEX = Regex(
        """(?:周|星期|礼拜)\s*[一二三四五六日天1-7]\s*$RANGE_CONNECTOR\s*""" +
            """(?:周|星期|礼拜)\s*[一二三四五六日天1-7]"""
    )
    private val SECTION_RANGE_REGEX = Regex(
        """(?:第\s*)?($NUMBER_TOKEN)\s*(?:节|小节)?\s*$RANGE_CONNECTOR\s*""" +
            """(?:第\s*)?($NUMBER_TOKEN)\s*(?:节|小节)"""
    )
    private val SECTION_SINGLE_REGEX = Regex(
        """(?:第\s*)?($NUMBER_TOKEN)\s*(?:节|小节)"""
    )

    private val ISO_DATE_REGEX =
        Regex("""(?<!\d)(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})(?!\d)""")
    private val SHORT_DATE_DOT_REGEX =
        Regex("""(?<!\d)(\d{1,2})[./](\d{1,2})(?:[日号])?(?!\d)""")
    private val CN_FULL_DATE_REGEX =
        Regex("""(?<!\d)(\d{4})年(\d{1,2})月(\d{1,2})[日号]?""")
    private val EVENING_DATE_REGEX = Regex("(大后晚|后晚|明晚|今晚)")
    private val RELATIVE_DATE_REGEX = Regex("(大后天|后天|明天|明日|今天|今日)")
    private val MONTH_DAY_REGEX = Regex("""(?<!\d)(\d{1,2})月(\d{1,2})[日号]""")
    private val NEXT_WEEK_REGEX = Regex("下(?:周|星期|礼拜)([一二三四五六日天])")
    private val WEEK_REGEX = Regex("(?:周|星期|礼拜)([一二三四五六日天])")
    private val WEEKDAY_TOKEN_REGEX = Regex("""(?:周|星期|礼拜)\s*[一二三四五六日天1-7]""")

    // 南邮大学特色口语化时段词：早八、下午两节、晚自习
    private val COLLOQUIAL_TIME_REGEX = Regex("(早八|下午两节|晚自习)")

    private val CLOCK_TIME_REGEX = Regex(
        """(凌晨|早上|上午|中午|下午|傍晚|晚上|夜里)?\s*""" +
            """(\d{1,2}|[零〇一二三四五六七八九十两]{1,3})\s*[:：]\s*(\d{1,2})"""
    )
    private val POINT_TIME_REGEX = Regex(
        """(凌晨|早上|上午|中午|下午|傍晚|晚上|夜里)?\s*""" +
            """(\d{1,2}|[零〇一二三四五六七八九十两]{1,3})\s*(?:点|时)""" +
            """(?:\s*(半)|(\d{1,2}|[零〇一二三四五六七八九十两]{1,3})\s*分?)?"""
    )
    private val RANGE_CONNECTOR_REGEX = Regex("""^\s*$RANGE_CONNECTOR\s*$""")

    private val ZAI_LOCATION_REGEX = Regex(
        """(?:在|于)\s*([^\s，,。；;、！？!?]{1,20}?)""" +
            """(?=\s*(?:上课|考试|开会|自习|参加|做|看|吃|集合|见|拿|取|办理|$|[，,。；;！？!?]))"""
    )
    private val QU_LOCATION_REGEX = Regex(
        """去\s*([^，,。；;、！？!?]{1,20}?)""" +
            """(?=\s*(?:上课|考试|开会|自习|参加|做|看|吃|集合|见|拿|取|办理|$|[，,。；;！？!?]))"""
    )
    // 南邮院系学科楼与常用场馆；长词前置，避免“计算机学科楼”被截成“学科楼”。
    private const val NJUPT_BUILDING =
        """(?:计算机|电子|光电|物联网|自动化|软件)?学科楼|自动化楼|软件楼|鼎山楼|中兴报告厅|大活|""" +
            """教[1-4一二三四]|教学[1-4一二三四]楼|实验[楼室]|通达馆|圆楼|文科楼|理科楼|安恒大厦|安恒|""" +
            """图书馆|体育馆|礼堂|食堂|会议室"""
    // 房间号支持多间连写：405、407 / B201/B202 / 405, 407。
    private const val ROOM_NUMBER = """[A-Za-z]?\d{2,4}"""
    private val ROOM_LOCATION_REGEX = Regex(
        """(?:$NJUPT_BUILDING)\s*[-－—]?\s*$ROOM_NUMBER""" +
            """(?:\s*[、,，/]\s*$ROOM_NUMBER)*\s*(?:教室|室)?"""
    )
    private val PLAIN_LOCATION_REGEX = Regex(
        "$NJUPT_BUILDING|通达|鼎山|战邮广场|南操|风雨操场|学一食堂|学二食堂|学三食堂"
    )
    // 教师：支持“张三老师”等称谓，以及“宋和平、李博文、研究生”这类人员列表。
    private val TEACHER_SUFFIX_REGEX = Regex(
        """([一-鿿]{1,3})\s*(?:老师|教师|教授)"""
    )
    private val TEACHER_LIST_REGEX = Regex(
        """([一-鿿]{2,4}(?:\s*[、,，/]\s*[一-鿿]{2,4})+)"""
    )
    private val TEACHER_STOP_WORDS = listOf(
        "上午", "下午", "中午", "晚上", "凌晨", "今天", "明天", "后天",
        "周一", "周二", "周三", "周四", "周五", "周六", "周日",
        "星期", "礼拜", "补课", "考试", "期末", "期中", "实验",
        "课程", "教室", "地点", "时间", "备注", "宿舍", "食堂", "图书馆"
    )
    private val LOCATION_VERB_REGEX = Regex(
        "上课|考试|开会|自习|参加|想|说|做|看|吃|写|复习|学习|工作|睡|玩"
    )
    private val PERIOD_WORD_REGEX = Regex("凌晨|早上|上午|中午|下午|傍晚|晚上|夜里")
    private val WHITESPACE_REGEX = Regex("""\s+""")

    /**
     * 解析一句话。无法得到任何有效信息时返回 null；仅输入标题时返回“今天 + 全天”的结果。
     *
     * @param semesterStartDate 学期第 1 周的开学日期；提供后可精确推算“第 N 周 周几”。
     * @param currentWeek 当前所在周次；未提供开学日期时，用它作为周次推算基准。
     */
    fun parse(
        input: String,
        today: LocalDate = currentDate(),
        semesterStartDate: LocalDate? = null,
        currentWeek: Int? = null
    ): ParseResult? {
        val text = input.trim()
        if (text.isBlank()) return null

        val removals = mutableListOf<IntRange>()
        var periodHint: TimePeriod? = null

        val colloquialMatch = COLLOQUIAL_TIME_REGEX.find(text)
        val sectionMatch = findSectionMatch(text, colloquialMatch?.range)
        val weekInfo = findWeekInfo(text)
        weekInfo?.removalRanges?.forEach { removals += it }

        var date = today
        var hasExplicitDate = false
        var matchedWeekday: Int? = null

        val eveningDate = EVENING_DATE_REGEX.find(text)
        if (eveningDate != null) {
            val offset = when (eveningDate.groupValues[1]) {
                "今晚" -> 0
                "明晚" -> 1
                "后晚" -> 2
                else -> 3
            }
            date = today.plusDays(offset)
            hasExplicitDate = true
            periodHint = TimePeriod.EVENING
            removals += eveningDate.range
        } else {
            val dateMatch = findDateMatch(text, today)
            if (dateMatch != null) {
                dateMatch.parsedDate?.let {
                    date = it
                    hasExplicitDate = true
                }
                matchedWeekday = dateMatch.weekday
                removals += dateMatch.range
            }
        }

        if (weekInfo != null && matchedWeekday != null) {
            resolveWeekDate(
                targetWeek = weekInfo.targetWeek,
                targetWeekday = matchedWeekday,
                today = today,
                semesterStartDate = semesterStartDate,
                currentWeek = currentWeek
            )?.let {
                date = it
                hasExplicitDate = true
            }
        }

        val timeTokens = findTimeTokens(text)
        var startMinutes: Int? = null
        var endMinutes: Int? = null

        val firstToken = timeTokens.getOrNull(0)
        if (firstToken != null) {
            val secondToken = timeTokens.getOrNull(1)
            val connectorRange = if (secondToken != null) {
                rangeConnectorRange(text, firstToken, secondToken)
            } else {
                null
            }
            val isRange = connectorRange != null

            val start = firstToken.toMinutesOfDay(periodHint)
            var end = if (isRange && secondToken != null) {
                secondToken.toMinutesOfDay(secondToken.period ?: firstToken.period ?: periodHint)
            } else {
                start + 60
            }

            if (isRange && secondToken != null && end <= start) {
                val period = firstToken.period ?: periodHint
                if ((period == TimePeriod.AFTERNOON || period == TimePeriod.EVENING) &&
                    secondToken.period == null &&
                    secondToken.hour in 1..11
                ) {
                    end += 12 * 60
                }
            }
            if (end <= start) end = start + 60

            startMinutes = start.coerceIn(0, MINUTES_PER_DAY - 1)
            endMinutes = end.coerceIn(0, MINUTES_PER_DAY - 1)

            timeTokens.forEach { removals += it.range }
            connectorRange?.let { removals += it }
        }

        // 未指定具体钟点时，节次按标准作息映射；“第1到2节”取第 1 节开始到第 2 节结束。
        if (startMinutes == null && sectionMatch != null) {
            sectionTimeRange(sectionMatch.startSection, sectionMatch.endSection)?.let { (start, end) ->
                startMinutes = start
                endMinutes = end
            }
        }
        sectionMatch?.range?.let { removals += it }

        if (colloquialMatch != null && startMinutes == null) {
            when (colloquialMatch.value) {
                "早八" -> {
                    startMinutes = 8 * 60
                    endMinutes = 9 * 60 + 35
                }
                "下午两节" -> {
                    startMinutes = 13 * 60 + 45
                    endMinutes = 15 * 60 + 20
                }
                "晚自习" -> {
                    startMinutes = 19 * 60
                    endMinutes = 21 * 60
                }
            }
            removals += colloquialMatch.range
        }
        val locationMatch = findLocation(text)
        locationMatch?.removalRange?.let { removals += it }

        val teacherMatch = findTeacher(text, removals)
        teacherMatch?.removalRange?.let { removals += it }

        // 含节次或周次、且不是考试时，优先视为补课。
        val type = detectType(
            text = text,
            hasSchedulingHint = sectionMatch != null || weekInfo != null
        )
        val title = cleanTitle(text, removals, type)

        return ParseResult(
            title = title,
            type = type,
            date = date,
            startTime = startMinutes?.let(::formatMinutes),
            endTime = endMinutes?.let(::formatMinutes),
            location = locationMatch?.value,
            rawInput = text,
            hasExplicitDate = hasExplicitDate,
            teacher = teacherMatch?.value
        )
    }

    private data class DateMatch(
        val parsedDate: LocalDate?,
        val range: IntRange,
        val weekday: Int? = null
    )

    private fun findDateMatch(text: String, today: LocalDate): DateMatch? {
        ISO_DATE_REGEX.find(text)?.let { match ->
            val year = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            val day = match.groupValues[3].toIntOrNull()
            return DateMatch(
                parsedDate = if (year != null && month != null && day != null) {
                    buildDate(year, month, day)
                } else null,
                range = match.range
            )
        }

        CN_FULL_DATE_REGEX.find(text)?.let { match ->
            val year = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            val day = match.groupValues[3].toIntOrNull()
            return DateMatch(
                parsedDate = if (year != null && month != null && day != null) {
                    buildDate(year, month, day)
                } else null,
                range = match.range
            )
        }

        MONTH_DAY_REGEX.find(text)?.let { match ->
            val month = parseNumber(match.groupValues[1])
            val day = parseNumber(match.groupValues[2])
            if (month != null && day != null) {
                var candidate = buildDate(today.year, month, day)
                if (candidate != null && candidate < today) {
                    candidate = buildDate(today.year + 1, month, day)
                }
                return DateMatch(candidate, match.range)
            }
            return DateMatch(null, match.range)
        }

        SHORT_DATE_DOT_REGEX.find(text)?.let { match ->
            val month = parseNumber(match.groupValues[1])
            val day = parseNumber(match.groupValues[2])
            if (month != null && day != null && month in 1..12 && day in 1..31) {
                var candidate = buildDate(today.year, month, day)
                if (candidate != null && candidate < today) {
                    candidate = buildDate(today.year + 1, month, day)
                }
                return DateMatch(candidate, match.range)
            }
        }

        NEXT_WEEK_REGEX.find(text)?.let { match ->
            val target = weekdayNumber(match.groupValues[1])
            if (target != null) {
                val daysToNextMonday = 8 - today.dayOfWeek.isoDayNumber
                val candidate = today.plusDays(daysToNextMonday + target - 1)
                return DateMatch(candidate, match.range, target)
            }
            return DateMatch(null, match.range)
        }

        WEEK_REGEX.find(text)?.let { match ->
            val target = weekdayNumber(match.groupValues[1])
            if (target != null) {
                val delta = (target - today.dayOfWeek.isoDayNumber + 7) % 7
                return DateMatch(today.plusDays(delta), match.range, target)
            }
            return DateMatch(null, match.range)
        }

        RELATIVE_DATE_REGEX.find(text)?.let { match ->
            val offset = when (match.groupValues[1]) {
                "今天", "今日" -> 0
                "明天", "明日" -> 1
                "后天" -> 2
                else -> 3
            }
            return DateMatch(today.plusDays(offset), match.range)
        }

        return null
    }

    private data class WeekInfo(
        val targetWeek: Int,
        val removalRanges: List<IntRange>
    )

    private data class SectionMatch(
        val startSection: Int,
        val endSection: Int,
        val range: IntRange
    )

    /**
     * 提取“第10周”“第8到10周”等周次表达。区间取起始周作为事件日期。
     */
    private fun findWeekInfo(text: String): WeekInfo? {
        val weekdayRanges = (
            WEEKDAY_RANGE_REGEX.findAll(text).map { it.range } +
                WEEKDAY_TOKEN_REGEX.findAll(text).map { it.range }
            ).toList()
        val validRanges = WEEK_RANGE_REGEX.findAll(text)
            .filterNot { match -> weekdayRanges.any { overlaps(match.range, it) } }
            .mapNotNull { match ->
                val start = parseNumber(match.groupValues[1]) ?: return@mapNotNull null
                val end = parseNumber(match.groupValues[2]) ?: return@mapNotNull null
                val low = minOf(start, end)
                val high = maxOf(start, end)
                if (low !in 1..MAX_WEEK || high !in 1..MAX_WEEK) return@mapNotNull null
                low to match.range
            }
            .toList()

        if (validRanges.isNotEmpty()) {
            return WeekInfo(
                targetWeek = validRanges.first().first,
                removalRanges = validRanges.map { it.second }
            )
        }

        val validSingles = WEEK_SINGLE_REGEX.findAll(text)
            .mapNotNull { match ->
                val week = parseNumber(match.groupValues[1]) ?: return@mapNotNull null
                if (week !in 1..MAX_WEEK) return@mapNotNull null
                week to match.range
            }
            .toList()

        if (validSingles.isNotEmpty()) {
            return WeekInfo(
                targetWeek = validSingles.first().first,
                removalRanges = validSingles.map { it.second }
            )
        }
        return null
    }

    /**
     * 提取“第1到2节”“3-4节”“第3节”等节次表达。
     * 与“下午两节”等口语化时段重叠时不视为节次。
     */
    private fun findSectionMatch(text: String, colloquialRange: IntRange?): SectionMatch? {
        SECTION_RANGE_REGEX.findAll(text).forEach { match ->
            if (colloquialRange != null && overlaps(match.range, colloquialRange)) return@forEach
            val start = parseNumber(match.groupValues[1]) ?: return@forEach
            val end = parseNumber(match.groupValues[2]) ?: return@forEach
            val low = minOf(start, end)
            val high = maxOf(start, end)
            if (low in 1..SECTION_TIME_RANGES.size && high in 1..SECTION_TIME_RANGES.size) {
                return SectionMatch(low, high, match.range)
            }
        }

        val single = SECTION_SINGLE_REGEX.find(text) ?: return null
        if (colloquialRange != null && overlaps(single.range, colloquialRange)) return null
        // 避免区间解析失败时把“第1到2节”的尾部误当成单节。
        if (text.getOrNull(single.range.first - 1) in setOf('到', '至', '-', '－', '—', '–', '~', '～')) {
            return null
        }
        val section = parseNumber(single.groupValues[1]) ?: return null
        if (section in 1..SECTION_TIME_RANGES.size) {
            return SectionMatch(section, section, single.range)
        }
        return null
    }

    private fun sectionTimeRange(start: Int, end: Int): Pair<Int, Int>? {
        if (start !in 1..SECTION_TIME_RANGES.size || end !in 1..SECTION_TIME_RANGES.size) {
            return null
        }
        val low = minOf(start, end)
        val high = maxOf(start, end)
        return SECTION_TIME_RANGES[low - 1].first to SECTION_TIME_RANGES[high - 1].second
    }

    private fun resolveWeekDate(
        targetWeek: Int,
        targetWeekday: Int,
        today: LocalDate,
        semesterStartDate: LocalDate?,
        currentWeek: Int?
    ): LocalDate? {
        if (targetWeek < 1 || targetWeekday !in 1..7) return null

        if (semesterStartDate != null) {
            val firstWeekMonday = semesterStartDate.plusDays(
                1 - semesterStartDate.dayOfWeek.isoDayNumber
            )
            return firstWeekMonday.plusDays((targetWeek - 1) * 7 + (targetWeekday - 1))
        }

        if (currentWeek != null && currentWeek > 0) {
            val currentWeekMonday = today.plusDays(1 - today.dayOfWeek.isoDayNumber)
            return currentWeekMonday.plusDays((targetWeek - currentWeek) * 7 + (targetWeekday - 1))
        }

        return null
    }

    private fun overlaps(first: IntRange, second: IntRange): Boolean =
        first.first <= second.last && second.first <= first.last

    private fun findTimeTokens(text: String): List<TimeToken> {
        val tokens = mutableListOf<TimeToken>()

        CLOCK_TIME_REGEX.findAll(text).forEach { match ->
            val hour = parseNumber(match.groupValues[2]) ?: return@forEach
            val minute = parseNumber(match.groupValues[3]) ?: return@forEach
            if (minute in 0..59) {
                tokens += TimeToken(
                    range = match.range,
                    hour = hour,
                    minute = minute,
                    period = periodOf(match.groupValues[1])
                )
            }
        }

        POINT_TIME_REGEX.findAll(text).forEach { match ->
            val hour = parseNumber(match.groupValues[2]) ?: return@forEach
            val minute = when {
                match.groupValues[3].isNotEmpty() -> 30
                match.groupValues[4].isNotEmpty() -> parseNumber(match.groupValues[4]) ?: return@forEach
                else -> 0
            }
            if (minute in 0..59) {
                tokens += TimeToken(
                    range = match.range,
                    hour = hour,
                    minute = minute,
                    period = periodOf(match.groupValues[1])
                )
            }
        }

        return tokens
            .distinctBy { it.range }
            .sortedBy { it.range.first }
    }

    private fun rangeConnectorRange(
        text: String,
        first: TimeToken,
        second: TimeToken
    ): IntRange? {
        if (second.range.first <= first.range.last) return null
        val start = first.range.last + 1
        val endExclusive = second.range.first
        val between = text.substring(start, endExclusive)
        return if (RANGE_CONNECTOR_REGEX.matches(between)) {
            start until endExclusive
        } else {
            null
        }
    }

    private fun TimeToken.toMinutesOfDay(periodHint: TimePeriod?): Int {
        var hour = this.hour
        when (this.period ?: periodHint) {
            TimePeriod.MORNING -> if (hour == 12) hour = 0
            TimePeriod.NOON -> if (hour in 1..10) hour += 12
            TimePeriod.AFTERNOON, TimePeriod.EVENING -> if (hour in 1..11) hour += 12
            null -> Unit
        }
        return hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)
    }

    private fun periodOf(raw: String?): TimePeriod? = when (raw) {
        "凌晨", "早上", "上午" -> TimePeriod.MORNING
        "中午" -> TimePeriod.NOON
        "下午" -> TimePeriod.AFTERNOON
        "傍晚", "晚上", "夜里" -> TimePeriod.EVENING
        else -> null
    }

    private fun findLocation(text: String): LocationMatch? {
        ROOM_LOCATION_REGEX.find(text)?.let { match ->
            val value = match.value.trim()
            if (value.isNotBlank()) {
                val removal = if (match.range.first > 0 && text[match.range.first - 1] == '在') {
                    (match.range.first - 1)..match.range.last
                } else {
                    null
                }
                return LocationMatch(value, removal)
            }
        }

        ZAI_LOCATION_REGEX.find(text)?.let { match ->
            val value = match.groupValues[1].trim()
            if (looksLikeLocation(value)) {
                return LocationMatch(value, match.range)
            }
        }

        QU_LOCATION_REGEX.find(text)?.let { match ->
            val value = match.groupValues[1].trim()
            if (looksLikeLocation(value)) {
                return LocationMatch(value, null)
            }
        }

        PLAIN_LOCATION_REGEX.find(text)?.let { match ->
            val removal = if (match.range.first > 0 && text[match.range.first - 1] == '在') {
                (match.range.first - 1)..match.range.last
            } else {
                null
            }
            return LocationMatch(match.value, removal)
        }

        return null
    }

    /**
     * 提取授课教师。支持“张三老师”等称谓，以及“宋和平、李博文、研究生”这类人员列表。
     */
    private fun findTeacher(text: String, occupied: List<IntRange>): LocationMatch? {
        TEACHER_LIST_REGEX.findAll(text).forEach { match ->
            if (occupied.any { overlaps(match.range, it) }) return@forEach
            val value = match.value.trim()
            if (isValidTeacherList(value)) {
                return LocationMatch(value, match.range)
            }
        }

        TEACHER_SUFFIX_REGEX.find(text)?.let { match ->
            if (occupied.any { overlaps(match.range, it) }) return null
            val name = match.groupValues[1].trim()
            if (name.isNotEmpty() && TEACHER_STOP_WORDS.none { name.contains(it) }) {
                return LocationMatch(name, match.range)
            }
        }

        return null
    }

    private fun isValidTeacherList(value: String): Boolean {
        val parts = value.split(Regex("""[、,，/]"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size < 2) return false
        return parts.all { part ->
            part.length in 2..4 && TEACHER_STOP_WORDS.none { part.contains(it) }
        }
    }

    private fun looksLikeLocation(value: String): Boolean {
        if (value.isBlank() || value.length > 20) return false
        return !LOCATION_VERB_REGEX.containsMatchIn(value)
    }

    private fun detectType(text: String, hasSchedulingHint: Boolean): String = when {
        text.contains("考") ||
            text.contains("期末") ||
            text.contains("期中") ||
            text.contains("测试") -> TYPE_EXAM

        text.contains("补课") || text.contains("实验") -> TYPE_MAKEUP

        // 出现节次或周次，通常是在描述一次补课而非普通待办。
        hasSchedulingHint -> TYPE_MAKEUP
        else -> TYPE_DAILY
    }

    private fun cleanTitle(text: String, removals: List<IntRange>, type: String): String {
        var cleaned = removeRanges(text, removals)
        cleaned = cleaned.replace(PERIOD_WORD_REGEX, " ")
        cleaned = cleaned.replace(WHITESPACE_REGEX, " ")
        cleaned = cleaned.trim()
        cleaned = cleaned.trim('，', ',', '。', '.', '；', ';', '：', ':', '、', '-', '—', '~', '～')
        cleaned = cleaned.trimStart('在', '于', '的')
        cleaned = cleaned.trimEnd('在', '于', '的')
        cleaned = cleaned.replace(WHITESPACE_REGEX, " ").trim()

        if (cleaned.isBlank()) {
            return when (type) {
                TYPE_EXAM -> "考试"
                TYPE_MAKEUP -> "补课"
                else -> "新日程"
            }
        }
        return cleaned
    }

    private fun removeRanges(text: String, ranges: List<IntRange>): String {
        if (ranges.isEmpty()) return text
        val builder = StringBuilder(text.length)
        var cursor = 0
        ranges
            .filter { it.first in text.indices && it.last in text.indices }
            .sortedBy { it.first }
            .forEach { range ->
                if (range.first < cursor) return@forEach
                if (range.first > cursor) builder.append(text, cursor, range.first)
                cursor = range.last + 1
            }
        if (cursor < text.length) builder.append(text, cursor, text.length)
        return builder.toString()
    }

    private fun parseNumber(raw: String): Int? {
        raw.toIntOrNull()?.let { return it }
        if (raw.isEmpty()) return null
        if (raw == "十") return 10

        val tenIndex = raw.indexOf('十')
        if (tenIndex >= 0) {
            val tensPart = raw.substring(0, tenIndex)
            val onesPart = raw.substring(tenIndex + 1)
            val tens = if (tensPart.isEmpty()) 1 else CN_DIGITS[tensPart.last()] ?: return null
            val ones = if (onesPart.isEmpty()) 0 else CN_DIGITS[onesPart.last()] ?: return null
            return tens * 10 + ones
        }

        var result = 0
        raw.forEach { char ->
            val digit = CN_DIGITS[char] ?: return null
            result = result * 10 + digit
        }
        return result
    }

    private fun weekdayNumber(char: String): Int? = when (char) {
        "一" -> 1
        "二" -> 2
        "三" -> 3
        "四" -> 4
        "五" -> 5
        "六" -> 6
        "日", "天" -> 7
        else -> null
    }

    private fun buildDate(year: Int, month: Int, day: Int): LocalDate? = runCatching {
        LocalDate.parse(
            "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        )
    }.getOrNull()

    private fun LocalDate.plusDays(days: Int): LocalDate =
        LocalDate.fromEpochDays(toEpochDays() + days)

    private fun formatMinutes(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    private fun currentDate(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private val CN_DIGITS = mapOf(
        '零' to 0,
        '〇' to 0,
        '一' to 1,
        '二' to 2,
        '两' to 2,
        '三' to 3,
        '四' to 4,
        '五' to 5,
        '六' to 6,
        '七' to 7,
        '八' to 8,
        '九' to 9
    )
}