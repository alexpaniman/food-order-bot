package org.order.logic.impl.commands

import org.joda.time.LocalTime
import java.util.*

val LOCALE = Locale("ru")

val JDBC_DATABASE_URL = System.getenv("JDBC_DATABASE_URL")!!
val DATABASE_DRIVER = System.getenv("DATABASE_DRIVER")!!

val BOT_TOKEN = System.getenv("BOT_TOKEN")!!
val BOT_USERNAME = System.getenv("BOT_USERNAME")!!

val LAST_ORDER_TIME = LocalTime(System.getenv("LAST_ORDER_TIME"))

val PAYMENTS_TOKEN = System.getenv("PAYMENTS_TOKEN")!!
const val INVOICE_START_PARAMETER = "0"
const val CURRENCY = "UAH"