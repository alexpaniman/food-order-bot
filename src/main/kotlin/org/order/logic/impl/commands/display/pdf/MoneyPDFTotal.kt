package org.order.logic.impl.commands.display.pdf

import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.property.TextAlignment
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.data.entities.*
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.*
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.LAST_ORDER_TIME
import org.order.logic.impl.utils.GRADE_COMPARATOR
import org.order.logic.impl.utils.grade
import java.lang.Exception
import kotlin.math.roundToInt

fun createMoneyPDFTotal() = createPDF {
    section(Text["money-pdf-total:title"], bold = true)

    val currentDate = DateTime.now()
            .toString("yyyy-MM-dd HH:mm:ss")

    text(currentDate,
            alignment = TextAlignment.CENTER,
            fontSize = 20f, bold = false)

    text("\n\n")

    table(0.2f, 0.5f, 0.1f, 0.1f, 0.1f) {
        cell(Text["money-pdf-total:grade"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["money-pdf-total:name"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["money-pdf-total:real-balance"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["money-pdf-total:future-orders"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["money-pdf-total:virtual-balance"],
                border = SolidBorder(1f),
                bold = true)

        var lastFinishedDay = LocalDate.now()
        if (LocalTime.now().isBefore(LAST_ORDER_TIME))
            lastFinishedDay = lastFinishedDay.minusDays(1)

        val groupedByGrade = Client.all()
                .filter { it.user.valid }
                .groupBy { it.user.grade }
                .toSortedMap(GRADE_COMPARATOR)

        var totalRealBalance = 0f
        var totalVirtualBalance = 0f
        var totalFutureOrders = 0f

        for ((grade, clients) in groupedByGrade) {
            cell(grade, row = clients.size)

            for (client in clients) {
                val ordered = client.orders
                        .filter { !it.canceled }
                        .filter { !lastFinishedDay.isBefore(it.orderDate) }
                        .sumBy { (- it.menu.cost * 100).roundToInt() } / 100f

                val paid = client.payments
                        .filter { it.registered != null }
                        .sumBy { (it.amount!! * 100).roundToInt() } / 100f

                val currentRealBalance = ordered + paid
                totalRealBalance += currentRealBalance

                val currentVirtualBalance = client.balance
                totalVirtualBalance += currentVirtualBalance

                val futureOrders = currentRealBalance - currentVirtualBalance
                totalFutureOrders += futureOrders

                cell(client.user.name!!)

                cell("$currentRealBalance")
                cell("$futureOrders")
                cell("$currentVirtualBalance")
            }
        }

        cell(Text["money-pdf-total:total"], col = 2,
                border = SolidBorder(1f),
                bold = true)

        cell("$totalRealBalance",
                border = SolidBorder(1f),
                bold = true)

        cell("$totalFutureOrders",
                border = SolidBorder(1f),
                bold = true)

        cell("$totalVirtualBalance",
                border = SolidBorder(1f),
                bold = true)

    }
}

private val MONEY_PDF_TOTAL_TRIGGER =
        (RoleTrigger(Producer) or RoleTrigger(Admin)) and
                StateTrigger(State.COMMAND) and
                CommandTrigger(Text["money-pdf-total-command"])

val MONEY_PDF_TOTAL = TriggerCommand(MONEY_PDF_TOTAL_TRIGGER) { user, _ ->
    PDFQueue.schedule {
        try {
            val pdfTotal = transaction {
                createMoneyPDFTotal()
            }

            val pdfTotalFileName = Text["money-pdf-total:file-name"]
            user.sendFile(pdfTotalFileName, "", pdfTotal)
        } catch(ignore: Exception) {}
    }
}
