package org.order.logic.impl.commands.display

import org.order.bot.send.button
import org.order.data.entities.Admin
import org.order.data.entities.Client
import org.order.data.entities.Producer
import org.order.data.entities.State.COMMAND
import org.order.data.entities.Student
import org.order.data.tables.Clients
import org.order.logic.commands.triggers.*
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text

private val MONEY_TOTAL_WINDOW_TRIGGER =
        (RoleTrigger(Producer) or RoleTrigger(Admin)) and
                StateTrigger(COMMAND) and
                CommandTrigger(Text["money-total-command"])

private const val WINDOW_MARKER = "money-total-window"

val MONEY_TOTAL_WINDOW = Window(WINDOW_MARKER, MONEY_TOTAL_WINDOW_TRIGGER,
        args = listOf("true")) { _, (showOnlyDebtStr) ->
    val showDebtOnly = showOnlyDebtStr.toBoolean()

    val clientsToShow = (if (showDebtOnly)
        Client.find { Clients.balance less 0f }
    else Client.all())
        .filter { it.user.valid }

    val moneyTotal = buildString {
        val groupedByGrade = clientsToShow
                .groupBy {
                    it.user.linkedOrNull(Student)
                            ?.grade
                            ?.name ?: Text["empty-grade"]
                }
                .toSortedMap()

        for ((grade, byGrade) in groupedByGrade) {
            appendln(Text.get("money-total-grade") { it["grade"] = grade })
            for (client in byGrade.sortedBy { it.balance })
                appendln(Text.get("money-total-user") {
                    it["user"] = client.user.name!!
                    it["balance"] = client.balance.toString()
                })

            appendln()
        }
    }

    val message =
            if (clientsToShow.isNotEmpty())
                Text.get("money-total") {
                    it["money-total-rows"] = moneyTotal
                }
            else Text["money-total-empty"]

    show(message) {
        if (showDebtOnly)
            button(Text["show-only-debt-checked"], "$WINDOW_MARKER:false")
        else
            button(Text["show-only-debt-unchecked"], "$WINDOW_MARKER:true")

        button(Text["update-button"], "$WINDOW_MARKER:$showDebtOnly")
        button(Text["cancel-button"], "remove-message")
    }
}