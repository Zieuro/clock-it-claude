package com.clockit.app.data

import java.time.LocalDateTime
import java.time.LocalTime

/** A single daily alarm in the schedule. */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val title: String = "",
    val enabled: Boolean = true,
) {
    val minuteOfDay: Int get() = hour * 60 + minute
    val time: LocalTime get() = LocalTime.of(hour, minute)

    /** First occurrence strictly after [ref]. */
    fun nextAfter(ref: LocalDateTime): LocalDateTime {
        val today = ref.toLocalDate().atTime(time)
        return if (today.isAfter(ref)) today else today.plusDays(1)
    }

    /** Latest occurrence at or before [ref]. */
    fun prevAtOrBefore(ref: LocalDateTime): LocalDateTime {
        val today = ref.toLocalDate().atTime(time)
        return if (!today.isAfter(ref)) today else today.minusDays(1)
    }
}

data class ScheduleState(
    val alarms: List<Alarm> = emptyList(),
    val armed: Boolean = false,
) {
    val activeAlarms: List<Alarm> get() = alarms.filter { it.enabled }

    /** The next time any active alarm fires after [ref], with every alarm sharing that minute. */
    fun nextFire(ref: LocalDateTime): Pair<LocalDateTime, List<Alarm>>? {
        val active = activeAlarms
        if (active.isEmpty()) return null
        val grouped = active.groupBy { it.nextAfter(ref) }
        val first = grouped.keys.min()
        return first to grouped.getValue(first)
    }

    /** The most recent time any active alarm fired at or before [ref]. */
    fun previousFire(ref: LocalDateTime): LocalDateTime? =
        activeAlarms.maxOfOrNull { it.prevAtOrBefore(ref) }

    /** Upcoming fire times in order, up to [count]. */
    fun upcoming(ref: LocalDateTime, count: Int): List<Pair<LocalDateTime, Alarm>> =
        activeAlarms.map { it.nextAfter(ref) to it }.sortedBy { it.first }.take(count)
}
