package com.xingheyuzhuan.shiguangschedule.data.nlp

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmartScheduleParserTest {

    // 基准日期：2026-11-05，星期四
    private val today = LocalDate(2026, 11, 5)

    @Test
    fun parsesRelativeDateAndChineseTimeWithLocation() {
        val result = SmartScheduleParser.parse("明天下午五点去教三 206 上课", today)

        assertNotNull(result)
        assertEquals("去教三 206 上课", result.title)
        assertEquals(SmartScheduleParser.TYPE_DAILY, result.type)
        assertEquals(LocalDate(2026, 11, 6), result.date)
        assertEquals("17:00", result.startTime)
        assertEquals("18:00", result.endTime)
        assertEquals("教三 206", result.location)
        assertTrue(result.hasExplicitDate)
    }

    @Test
    fun parsesNextWeekExamWithColonTime() {
        val result = SmartScheduleParser.parse("下周三上午9:50高数期末考试在教2-410", today)

        assertNotNull(result)
        assertEquals("高数期末考试", result.title)
        assertEquals(SmartScheduleParser.TYPE_EXAM, result.type)
        assertEquals(LocalDate(2026, 11, 11), result.date)
        assertEquals("09:50", result.startTime)
        assertEquals("10:50", result.endTime)
        assertEquals("教2-410", result.location)
        assertTrue(result.hasExplicitDate)
    }

    @Test
    fun parsesTonightWithHalfHourAndImplicitDate() {
        val result = SmartScheduleParser.parse("今晚8点半交操作系统作业", today)

        assertNotNull(result)
        assertEquals("交操作系统作业", result.title)
        assertEquals(SmartScheduleParser.TYPE_DAILY, result.type)
        assertEquals(today, result.date)
        assertEquals("20:30", result.startTime)
        assertEquals("21:30", result.endTime)
        assertNull(result.location)
        assertTrue(result.hasExplicitDate)
    }

    @Test
    fun parsesExplicitTimeRangeAndMakeupType() {
        val result = SmartScheduleParser.parse("11月6日上午9点到11点补课", today)

        assertNotNull(result)
        assertEquals("补课", result.title)
        assertEquals(SmartScheduleParser.TYPE_MAKEUP, result.type)
        assertEquals(LocalDate(2026, 11, 6), result.date)
        assertEquals("09:00", result.startTime)
        assertEquals("11:00", result.endTime)
    }

    @Test
    fun parsesAfternoonRangeWithPeriodInheritance() {
        val result = SmartScheduleParser.parse("下周三下午3点到5点开会", today)

        assertNotNull(result)
        assertEquals("开会", result.title)
        assertEquals(LocalDate(2026, 11, 11), result.date)
        assertEquals("15:00", result.startTime)
        assertEquals("17:00", result.endTime)
    }

    @Test
    fun parsesIsoDateAndExperimentLocation() {
        val result = SmartScheduleParser.parse("2026-12-01 14:00 数据库实验 在实验楼", today)

        assertNotNull(result)
        assertEquals("数据库实验", result.title)
        assertEquals(SmartScheduleParser.TYPE_MAKEUP, result.type)
        assertEquals(LocalDate(2026, 12, 1), result.date)
        assertEquals("14:00", result.startTime)
        assertEquals("15:00", result.endTime)
        assertEquals("实验楼", result.location)
    }

    @Test
    fun parsesChineseNumeralHourAndWeekday() {
        val result = SmartScheduleParser.parse("周六上午十点图书馆自习", today)

        assertNotNull(result)
        assertEquals("图书馆自习", result.title)
        assertEquals(LocalDate(2026, 11, 7), result.date)
        assertEquals("10:00", result.startTime)
        assertEquals("11:00", result.endTime)
        assertEquals("图书馆", result.location)
    }

    @Test
    fun parsesQuLocationFallback() {
        val result = SmartScheduleParser.parse("明天下午两点去南门取快递", today)

        assertNotNull(result)
        assertEquals("去南门取快递", result.title)
        assertEquals("南门", result.location)
        assertEquals("14:00", result.startTime)
        assertEquals("15:00", result.endTime)
    }

    @Test
    fun keepsTextWithoutDateOrTimeAsTitle() {
        val result = SmartScheduleParser.parse("交操作系统作业", today)

        assertNotNull(result)
        assertEquals("交操作系统作业", result.title)
        assertEquals(today, result.date)
        assertFalse(result.hasExplicitDate)
        assertNull(result.startTime)
        assertNull(result.endTime)
        assertEquals(SmartScheduleParser.TYPE_DAILY, result.type)
    }

    @Test
    fun treatsExplicitMonthAndDayAndExamKeyword() {
        val result = SmartScheduleParser.parse("12月31日期末测试", today)

        assertNotNull(result)
        assertEquals("期末测试", result.title)
        assertEquals(SmartScheduleParser.TYPE_EXAM, result.type)
        assertEquals(LocalDate(2026, 12, 31), result.date)
    }

    @Test
    fun rollsMonthDayToNextYearWhenAlreadyPassed() {
        val result = SmartScheduleParser.parse("1月5日交材料", today)

        assertNotNull(result)
        assertEquals(LocalDate(2027, 1, 5), result.date)
    }

    @Test
    fun returnsNullForBlankInput() {
        assertNull(SmartScheduleParser.parse("   ", today))
    }
}