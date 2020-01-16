package org.order.logic.impl.utils

import org.joda.time.LocalDate
import org.order.logic.impl.commands.LOCALE

val LocalDate.dayOfWeekAsText: String
    get() = dayOfWeek().getAsShortText(LOCALE)