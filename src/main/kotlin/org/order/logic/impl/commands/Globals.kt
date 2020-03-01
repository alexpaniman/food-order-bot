package org.order.logic.impl.commands

import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.logic.impl.utils.getproperty
import org.order.logic.impl.utils.setproperty
import java.util.*

val LOCALE = Locale("ru")
const val REGION_ID = "Europe/Kiev"

val JDBC_DATABASE_URL = System.getenv("JDBC_DATABASE_URL")!!
val DATABASE_DRIVER = System.getenv("DATABASE_DRIVER")!!

val BOT_TOKEN = System.getenv("BOT_TOKEN")!!
val BOT_USERNAME = System.getenv("BOT_USERNAME")!!

val PAYMENTS_TOKEN = System.getenv("PAYMENTS_TOKEN")!!

val LAST_ORDER_TIME get() = LocalTime(getproperty("LAST_ORDER_TIME"))
val TIME_TO_SEND_POLL get() = LocalTime(getproperty("TIME_TO_SEND_POLL"))
val TIME_TO_NOTIFY get() = LocalTime(getproperty("TIME_TO_NOTIFY"))
val MAX_DEBT get() = getproperty("MAX_DEBT").toFloat()

var LAST_NOTIFICATION: LocalDate
    get() = LocalDate.parse(getproperty("LAST_NOTIFICATION"))
    set(value) = setproperty("LAST_NOTIFICATION", value.toString())

const val INVOICE_START_PARAMETER = "0"
const val CURRENCY = "UAH"

const val TEXT_CORPUS_PATH = "src/main/resources/text-corpus.txt"

const val FONT_FOR_BUILDING_PDF_PATH = "src/main/resources/font.ttf"
const val BOLD_FONT_FOR_BUILDING_PDF_PATH = "src/main/resources/bold-font.ttf"