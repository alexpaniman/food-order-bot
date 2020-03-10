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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.lang.Thread.sleep
import kotlin.concurrent.thread

const val LISTENER_DELAY = 10000L

private fun SenderContext.notifyClient(client: Client, clientsWhoOrdered: Set<Client>) {
    if (client.user.state == IMAGINE)
        return

    if (client in clientsWhoOrdered)
        return

    client.user.send(Text["notification"])
}

private fun SenderContext.notifyClientSafe(client: Client, clientsWhoOrdered: Set<Client>) =
        try {
            notifyClient(client, clientsWhoOrdered)
            sleep(35)
        } catch (exc: TelegramApiException) {
            exc.printStackTrace()
        }

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

            for (client in Client.all())
                notifyClientSafe(client, clientsWhoOrdered)

            LAST_NOTIFICATION = now
        }

        sleep(LISTENER_DELAY)
    }
}