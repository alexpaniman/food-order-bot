package org.order.logic.impl.commands.notifications

import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.bot.send.SenderContext
import org.order.data.entities.*
import org.order.data.entities.State.IMAGINE
import org.order.data.tables.Orders
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LAST_PARENT_NOTIFICATION
import org.order.logic.impl.commands.LAST_STUDENT_NOTIFICATION
import org.order.logic.impl.commands.TIME_TO_NOTIFY_PARENT
import org.order.logic.impl.commands.TIME_TO_NOTIFY_STUDENT
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.lang.Thread.sleep
import kotlin.concurrent.thread

const val LISTENER_DELAY = 10000L

private fun SenderContext.notifyUser(user: User) =
        try {
            user.send(Text["notification"])
            sleep(35)
        } catch (exc: TelegramApiException) {
            exc.printStackTrace()
        }

private fun clientCanOrderTodayForTomorrow(): Boolean {
    val tomorrow = LocalDate.now()
            .plusDays(1)

    return Menu.all().any { it.schedule.isAvailable(tomorrow) }
}

fun SenderContext.launchClientsNotifier() = thread {
    while (true) {
        val timeNow = LocalTime.now()
                .withSecondOfMinute(0)
                .withMillisOfSecond(0)

        val isTimeToNotifyParent = timeNow >= TIME_TO_NOTIFY_PARENT
                && LAST_PARENT_NOTIFICATION < LocalDate.now()

        val isTimeToNotifyStudent = timeNow >= TIME_TO_NOTIFY_STUDENT
                && LAST_STUDENT_NOTIFICATION < LocalDate.now()

        if (isTimeToNotifyParent || isTimeToNotifyStudent) transaction {
            if (clientCanOrderTodayForTomorrow()) {
                val now = LocalDate.now()

                val clientsWhoOrdered = Order
                        .find { Orders.orderDate eq now.plusDays(1).toString() }
                        .map { it.client }
                        .toSet()

                val notified = Client.all()
                        .filter { it !in clientsWhoOrdered }
                        .map { it.user }
                        .flatMap {
                            if (isTimeToNotifyParent)
                                it.linkedOrNull(Student)?.parents
                                        ?.map { parent ->
                                            parent.user
                                        } ?: listOf()
                            else listOf(it)
                        }
                        .distinct()
                        .filter { it.state != IMAGINE }
                        .onEach { notifyUser(it) } // <== Notify

                if (notified.isNotEmpty()) {
                    val message = Text.get("notification-report") {
                        it["count"] = "${notified.size}"
                        it["type"] = if (isTimeToNotifyParent)
                            Text["notification-report:parent"]
                        else
                            Text["notification-report:student"]

                        it["people"] = notified.joinToString("\n") { user -> "\t${user.name}" }
                    }

                    Admin.all()
                            .map { it.user }
                            .forEach { it.send(message) }

                    if (isTimeToNotifyParent)
                        LAST_PARENT_NOTIFICATION = now
                    else
                        LAST_STUDENT_NOTIFICATION = now
                }
            }
        }

        sleep(LISTENER_DELAY)
    }
}