package org.order.logic.impl.utils

import org.joda.time.DateTimeConstants.*
import org.joda.time.Days
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.data.entities.Menu
import org.order.logic.impl.commands.LAST_ORDER_TIME

class Schedule(private val start: LocalDate, private val step: Int) {

    companion object {
        @JvmStatic
        fun parse(input: String): Schedule {
            val (dateStr, stepStr) = input.split(':')
            return Schedule(
                    LocalDate.parse(dateStr),
                    stepStr.toInt()
            )
        }
    }

    fun isAvailable(date: LocalDate) =
            Days.daysBetween(start, date).days % step == 0

    override fun toString() = "$start:$step"
}

fun Menu.availableList(): List<LocalDate> {
    val dateNow = LocalDate.now()
    val timeNow = LocalTime.now()

    val lastMonday = dateNow.minusDays(dateNow.dayOfWeek - MONDAY)

    val bound = if (dateNow.dayOfWeek in SATURDAY..SUNDAY)
        lastMonday.plusWeeks(2)
    else
        lastMonday.plusWeeks(1)

    val activeDays = mutableListOf<LocalDate>()

    var date =
            if (LAST_ORDER_TIME.isAfter(timeNow)) dateNow
            else dateNow.plusDays(1)

    while (date.isBefore(bound)) {
        if (schedule.isAvailable(date))
            activeDays += date
        date = date.plusDays(1)
    }

    return activeDays
}