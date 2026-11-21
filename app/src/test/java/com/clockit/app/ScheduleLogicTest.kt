package com.clockit.app

import com.clockit.app.data.Alarm
import com.clockit.app.data.IntervalTemplate
import com.clockit.app.data.ScheduleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ScheduleLogicTest {
    @Test fun intervalInclusiveRange() {
        assertEquals(listOf(420, 435, 450, 465, 480), IntervalTemplate.generate(420, 480, 15))
    }

    @Test fun intervalWrapsMidnight() {
        assertEquals(listOf(1410, 0, 30), IntervalTemplate.generate(1410, 30, 30))
    }

    @Test fun intervalCapped() {
        assertEquals(IntervalTemplate.MAX_ALARMS, IntervalTemplate.generate(0, 1439, 1).size)
    }

    @Test fun nextFireSkipsPastAndDisabled() {
        val s = ScheduleState(
            listOf(Alarm(1, 7, 0), Alarm(2, 8, 0, enabled = false), Alarm(3, 9, 0, "Gym")),
            armed = true,
        )
        val now = LocalDateTime.of(2026, 1, 1, 7, 30)
        val (at, alarms) = s.nextFire(now)!!
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0), at)
        assertEquals("Gym", alarms.single().title)
        assertEquals(LocalDateTime.of(2026, 1, 1, 7, 0), s.previousFire(now))
    }

    @Test fun nextFireRollsToTomorrowAndIsStrict() {
        val s = ScheduleState(listOf(Alarm(1, 7, 0)), armed = true)
        val at = LocalDateTime.of(2026, 1, 1, 7, 0)
        assertEquals(at.plusDays(1), s.nextFire(at)!!.first)
    }

    @Test fun noActiveAlarms() {
        assertNull(ScheduleState(listOf(Alarm(1, 7, 0, enabled = false))).nextFire(LocalDateTime.now()))
    }
}
