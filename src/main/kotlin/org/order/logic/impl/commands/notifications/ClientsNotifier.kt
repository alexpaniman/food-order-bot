package org.order.logic.impl.commands.notifications

import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.bot.send.SenderContext
import org.order.data.entities.Client
import org.order.data.entities.Order
import org.order.data.entities.State.IMAGINE
import org.order.data.tables.Orders
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LAST_NOTIFICATION
import org.order.logic.impl.commands.TIME_TO_NOTIFY
import java.lang.Thread.sleep
import kotlin.concurrent.thread

const val LISTENER_DELAY = 10000L

fun SenderContext.launchClientsNotifier() = thread {
    while (true) {
        val timeNow = LocalTime.now()
                .withSecondOfMinute(0)
                .withMillisOfSecond(0)


        if (timeNow >= TIME_TO_NOTIFY && LAST_NOTIFICATION < LocalDate.now()) transaction {
            val now = LocalDate.now()
            val clientsWhoOrdered = Order
                    .find { Orders.orderDate eq now.plusDays(1).toString() }
                    .map { it.client }
                    .toSet()

            for (client in Client.all()) {
                if (client.user.state == IMAGINE)
                    continue

                if (client in clientsWhoOrdered)
                    continue

                client.user.send(Text["notification"])
            }

            LAST_NOTIFICATION = now
        }

        sleep(LISTENER_DELAY)
    }
}