package org.order.logic.impl.utils

import org.joda.time.LocalDate
import org.order.logic.impl.commands.LOCALE

val LocalDate.dayOfWeekAsShortText: String
    get() = dayOfWeek().getAsShortText(LOCALE)

val LocalDate.dayOfWeekAsLongText: String
    get() {
        val text = dayOfWeek().getAsText(LOCALE)
        return text[0].toUpperCase() + text.drop(1)
    }