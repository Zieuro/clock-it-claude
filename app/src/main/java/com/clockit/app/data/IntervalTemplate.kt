package com.clockit.app.data

object IntervalTemplate {
    const val MAX_ALARMS = 288

    /**
     * Minute-of-day values from [startMin] to [endMin] inclusive, every [interval] minutes.
     * If end is earlier than start the range wraps past midnight.
     */
    fun generate(startMin: Int, endMin: Int, interval: Int): List<Int> {
        if (interval <= 0) return emptyList()
        val span = if (endMin >= startMin) endMin - startMin else endMin + 1440 - startMin
        val out = ArrayList<Int>()
        var offset = 0
        while (offset <= span && out.size < MAX_ALARMS) {
            out += (startMin + offset) % 1440
            offset += interval
        }
        return out
    }
}
