package org.order.logic.impl.commands.display

import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.property.TextAlignment
import org.joda.time.DateTime
import org.order.data.entities.Admin
import org.order.data.entities.Client
import org.order.data.entities.Producer
import org.order.data.entities.State
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.*
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.*
import kotlin.math.roundToInt

fun createMoneyPDFTotal() = createPDF {
    section(Text["money-pdf-total:title"], bold = true)

    text(DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),
            alignment = TextAlignment.CENTER,
            fontSize = 20f, bold = false)

    text("\n\n")

    table(0.9f, 0.1f) {
        cell(Text["money-pdf-total:name"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["money-pdf-total:balance"],
                border = SolidBorder(1f),
                bold = true)

        val clients = Client.all()
        for (client in clients) {
            cell(client.user.name!!)
            cell(client.balance.toString())
        }

        cell(Text["money-pdf-total:total"],
                border = SolidBorder(1f),
                bold = true)

        val total = clients.sumBy {
            (it.balance * 100).roundToInt()
        } / 100f

        cell(total.toString(),
                border = SolidBorder(1f),
                bold = true)
    }
}

private val MONEY_PDF_TOTAL_TRIGGER =
        (RoleTrigger(Producer) or RoleTrigger(Admin)) and
                StateTrigger(State.COMMAND) and
                CommandTrigger(Text["money-pdf-total-command"])

val MONEY_PDF_TOTAL = TriggerCommand(MONEY_PDF_TOTAL_TRIGGER) { user, _ ->
    val pdfTotal = createMoneyPDFTotal()
    val pdfTotalFileName = Text.get("money-pdf-total:file-name") {
        it["date"] = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
    }
    user.sendFile(pdfTotalFileName, "", pdfTotal)
}
