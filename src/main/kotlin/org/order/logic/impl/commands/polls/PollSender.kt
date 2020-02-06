package org.order.logic.impl.commands.polls

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.bot.send.*
import org.order.data.entities.*
import org.order.data.tables.Orders
import org.order.data.tables.PollAnswers
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.window.WindowContext
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.TIME_TO_SEND_POLL
import java.lang.Thread.sleep
import kotlin.concurrent.thread

private fun WindowContext.sendPoll(order: Order) {
    val message = Text.get("poll") {
        it["order:menu:name"] = order.menu.name
        it["order:order-date"] = order.orderDate.toString()
    }

    val dishes = order.menu.dishes
            .sortedBy { it.id } // Exact same order guarantee

    show(message) {
        var finished = 0

        for (dish in dishes) {
            button(dish.name)

            row {
                val pollAnswer = PollAnswer
                        .find { PollAnswers.order eq order.id and (PollAnswers.dish eq dish.id) }
                        .firstOrNull()

                if (pollAnswer != null)
                    finished ++

                val rate = pollAnswer?.rate ?: 0

                repeat(rate) {
                    if (pollAnswer == null)
                        button(Text["poll-filled-star"], "poll-rate:${order.id}:${dish.id}:${it + 1}")
                    else
                        button(Text["poll-filled-star"])
                }

                repeat(5 - rate) {
                    if (pollAnswer == null)
                        button(Text["poll-unfilled-star"], "poll-rate:${order.id}:${dish.id}:${it + rate + 1}")
                    else
                        button(Text["poll-unfilled-star"])
                }
            }
        }

        if (finished == dishes.size)
            button(Text["confirm-poll"], "remove-message")
    }
}

val RATE_PROCESSOR = CallbackProcessor("poll-rate") { user, src, (orderIdStr, dishIdStr, rateStr) ->
    val rate = rateStr.toInt()

    val orderId = orderIdStr.toInt()
    val order = Order.findById(orderId) ?: error("There's no order with id: $orderId")

    val dishId = dishIdStr.toInt()
    val dish = Dish.findById(dishId) ?: error("There's no dish with id: $dishId")

    PollAnswer.find { PollAnswers.order eq order.id and (PollAnswers.dish eq dish.id) }
            .firstOrNull() ?: PollAnswer.new {
        this.order = order
        this.dish = dish
        this.rate = rate
    }

    WindowContext(this, src, user)
            .sendPoll(order)
}

private fun SenderContext.sendPolls() = transaction {
    val today = LocalDate.now().toString()
    val ordersToday = Order
            .find { Orders.orderDate eq today }
            .filter { !it.canceled }

    for (order in ordersToday) {
        val clientUser = order.client.user
        if (clientUser.state != State.IMAGINE)
            WindowContext(this@sendPolls, null, clientUser)
                    .sendPoll(order)
    }
}

private const val LISTENER_DELAY = 100L

fun SenderContext.launchPollSender() = thread {
    var wasSent = false

    while (true) {
        val timeNow = LocalTime.now()
                .withSecondOfMinute(0)
                .withMillisOfSecond(0)// Remove seconds

        if (!wasSent && timeNow == TIME_TO_SEND_POLL) {
            sendPolls()
            wasSent = true
        }

        if (timeNow != TIME_TO_SEND_POLL)
            wasSent = false

        sleep(LISTENER_DELAY)
    }
}