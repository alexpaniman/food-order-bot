package org.order.logic.impl.commands.display.pdf

import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.layout.borders.SolidBorder
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.order.data.entities.*
import org.order.data.tables.Clients
import org.order.data.tables.OrdersCancellations
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.processors.CallbackProcessor
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.RoleTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.triggers.or
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.modules.searchUsers
import org.order.logic.impl.utils.clients

private data class HistoryAction(val time: DateTime, val balanceChange: Float, val description: String)

private fun fetchOrdersHistory(client: Client) = client.orders
    .map { order ->
        val description = Text.get("history-pdf:order") {
            it["ordered-by"] = order.madeBy.name!!
            it["menu:name"] = order.menu.name
            it["order-date"] = order.orderDate.toString()
        }

        HistoryAction(order.registered, -order.menu.cost, description)
    }

private fun fetchPaymentsHistory(client: Client) = client.payments
    .map { payment ->
        val description = Text.get("history-pdf:payment") {
            it["paid-by"] = payment.madeBy.name!!
            it["amount"] = payment.amount.toString()
            it["client:name"] = payment.client.user.name!!
        }

        HistoryAction(payment.registered!!, payment.amount!!, description)
    }

private fun fetchCancellationsHistory(client: Client) = client.orders
    .filter { it.canceled }.map { order ->
        val cancellation = OrderCancellation
            .find { OrdersCancellations.order eq order.id }
            .first()

        val description = Text.get("history-pdf:order-cancellation") {
            it["canceled-by"] = cancellation.canceledBy.name!!
            it["order:menu:name"] = order.menu.name
            it["order:order-date"] = order.orderDate.toString()
        }

        HistoryAction(cancellation.canceled, cancellation.order.menu.cost, description)
    }

fun createHistoryPDF(user: User) = createPDF {
    section(Text.get("history-pdf:title") {
        it["name"] = user.name!! // TODO Make safe
        it["date"] = DateTime.now().toString("dd-MM-yyyy HH:mm")
    }, bold = true)

    text("\n\n")

    val clients = user.clients()

    for (client in clients) {
        if (clients.size > 1)
            section(Text.get("history-pdf:balance-change-history") {
                it["client:name"] = client.user.name!! // Should be safe
            })

        val payments = fetchPaymentsHistory(client)
        val orders = fetchOrdersHistory(client)
        val cancellations = fetchCancellationsHistory(client)

        val allActions = (orders + payments + cancellations)
            .groupBy { it.time.toLocalDate() }
            .mapValues { (_, action) ->
                action.sortedBy { it.time }
            }

        table(.16f, .1f, .2f, .32f, .22f) {
            cell(Text["history-pdf:date"       ], border = SolidBorder(1f), bold = true)
            cell(Text["history-pdf:time"       ], border = SolidBorder(1f), bold = true)
            cell(Text["history-pdf:made-by"    ], border = SolidBorder(1f), bold = true)
            cell(Text["history-pdf:description"], border = SolidBorder(1f), bold = true)
            cell(Text["history-pdf:balance"    ], border = SolidBorder(1f), bold = true)

            repeat(4) {
                cell(Text["history-pdf:empty"])
            }

            cell("0")

            var balance = 0f
            for ((date, actions) in allActions) {
                cell("$date", row = actions.size)
                for (action in actions) {
                    val time = action.time.toString("HH:mm")
                    cell(time)
                    cell("Александр Паниман")
                    cell(action.description)

                    balance += action.balanceChange
                    val diff = action.balanceChange

                    var balanceView = par("$balance (")
                    balanceView += if (diff < 0)
                        par("$diff", color = DeviceRgb(0xad, 0x0e, 0x3c))
                    else
                        par("+$diff", color = DeviceRgb(0x6c, 0xad, 0x0e))
                    balanceView += par(")")

                    cell(balanceView)
                }
            }

            cell(Text["history-pdf:total"], col = 4, border = SolidBorder(1f), bold = true)
            cell("$balance",                   border = SolidBorder(1f), bold = true)
        }

        if (clients.size > 1)
            text("\n")
    }
}

val HISTORY_PDF_TOTAL_TRIGGER =
    CommandTrigger(Text["history-command"]) and (RoleTrigger(Client) or RoleTrigger(Parent))

val HISTORY_PDF_TOTAL = TriggerCommand(HISTORY_PDF_TOTAL_TRIGGER) { user, _ ->
    PDFQueue.schedule {
        val pdfTotal = transaction {
            createHistoryPDF(user)
        }

        val pdfTotalFileName = Text["history-pdf:file-name"]
        user.sendFile(pdfTotalFileName, "", pdfTotal)
    }
}

val HISTORY_PDF_SEARCH_TRIGGER =
    CommandTrigger(Text["history-search-command"]) and (RoleTrigger(Admin) or RoleTrigger(Producer))

private const val HISTORY_SEARCH_CALLBACK = "history-search-window"

val HISTORY_PDF_SEARCH = TriggerCommand(HISTORY_PDF_SEARCH_TRIGGER) { user, _ ->
    searchUsers(user, "$HISTORY_SEARCH_CALLBACK:{}")
}

val HISTORY_PDF_SEARCH_SEND = CallbackProcessor(HISTORY_SEARCH_CALLBACK) { user, _, (clientIdStr) ->
    val clientId = clientIdStr.split(",").first().toInt()
    val client = Client.findById(clientId) ?: error("No such client!")

    PDFQueue.schedule {
        val pdfTotal = transaction {
            createHistoryPDF(client.user)
        }

        val pdfTotalFileName = Text["history-pdf:file-name"]
        user.sendFile(pdfTotalFileName, "", pdfTotal)
    }
}