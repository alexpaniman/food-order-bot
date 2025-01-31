package org.order.logic.impl.commands

import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.logic.impl.utils.getproperty
import org.order.logic.impl.utils.setproperty
import java.util.*

val LOCALE = Locale("ru")
const val REGION_ID = "Europe/Kiev"

val JDBC_DATABASE_URL = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:h2:mem:testdb;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
val DATABASE_DRIVER = System.getenv("DATABASE_DRIVER") ?: "org.postgresql.Driver"

val BOT_TOKEN = System.getenv("BOT_TOKEN") ?: "EMPTY_TOKEN"
val BOT_USERNAME = System.getenv("BOT_USERNAME") ?: "EMPTY_USERNAME"

val PAYMENTS_TOKEN = System.getenv("PAYMENTS_TOKEN") ?: "EMPTY_PAYMENTS_TOKEN"

val LAST_ORDER_TIME
    get() = LocalTime(getproperty("LAST_ORDER_TIME"))

val TIME_TO_SEND_POLL
    get() = LocalTime(getproperty("TIME_TO_SEND_POLL"))

val MAX_DEBT
    get() = getproperty("MAX_DEBT").toFloat()


val TIME_TO_NOTIFY_PARENT
    get() = LocalTime(getproperty("TIME_TO_NOTIFY_PARENT"))

val TIME_TO_NOTIFY_STUDENT
    get() = LocalTime(getproperty("TIME_TO_NOTIFY_STUDENT"))

var LAST_PARENT_NOTIFICATION: LocalDate
    get() = LocalDate.parse(getproperty("LAST_PARENT_NOTIFICATION"))
    set(value) = setproperty("LAST_PARENT_NOTIFICATION", value.toString())

var LAST_STUDENT_NOTIFICATION: LocalDate
    get() = LocalDate.parse(getproperty("LAST_STUDENT_NOTIFICATION"))
    set(value) = setproperty("LAST_STUDENT_NOTIFICATION", value.toString())


val COMMISSION
    get() = getproperty("COMMISSION").toFloat()

const val INVOICE_START_PARAMETER = "0"
const val CURRENCY = "UAH"


val RES_HOME = System.getenv("RES_HOME") ?: "src/main/resources/"
val TEXT_CORPUS_PATH = "$RES_HOME/text-corpus.txt"

val FONT_FOR_BUILDING_PDF_PATH = "$RES_HOME/font.ttf"
val BOLD_FONT_FOR_BUILDING_PDF_PATH = "$RES_HOME/bold-font.ttf"
