package org.order.logic.impl.commands.display.pdf

import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.property.TextAlignment
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.order.data.entities.Admin
import org.order.data.entities.Order
import org.order.data.entities.Producer
import org.order.data.entities.State
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.*
import org.order.logic.corpus.Text

fun createOrdersPDFTotal() = createPDF {
    section(Text["orders-pdf-total:title"], bold = true)

    val currentDate = DateTime.now()
            .minusMonths(1)
            .toString("yyyy-MM")

    text(currentDate,
            alignment = TextAlignment.CENTER,
            fontSize = 20f, bold = false)

    text("\n\n")

    table(0.3f, 0.15f, 0.2f, 0.2f, 0.15f) {
        cell(Text["orders-pdf-total:date"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["orders-pdf-total:menu"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["orders-pdf-total:menu-cost"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["orders-pdf-total:amount"],
                border = SolidBorder(1f),
                bold = true)

        cell(Text["orders-pdf-total:cost"],
                border = SolidBorder(1f),
                bold = true)

        val now = LocalDate.now()

        val start = now
                .withDayOfMonth(1)
                .minusMonths(1)

        val end = now
                .withDayOfMonth(1)
                .minusDays(1)

        val allOrders = Order.all()
                .filter { it.orderDate >= start && it.orderDate <= end }

        val groupedByDate = allOrders
                .groupBy { it.orderDate }

        for ((date, byDate) in groupedByDate) {
            val groupedByMenu = byDate
                    .groupBy { it.menu }

            cell(date.toString("yyyy-MM-dd"),
                    row = groupedByMenu.size)

            var totalCostForDay = 0f
            for ((menu, byMenu) in groupedByMenu) {
                cell(menu.name)
                cell("${menu.cost}")

                val totalCount = byMenu.size
                val totalCost = totalCount * menu.cost

                totalCostForDay += totalCost

                cell("$totalCount")
                cell("$totalCost")
            }

            cell(Text["orders-pdf-total:sum-for-a-day"], col = 3, border = SolidBorder(1f))
            cell("${byDate.size}", border = SolidBorder(1f))
            cell("$totalCostForDay", border = SolidBorder(1f))
        }

        val groupedByMenu = allOrders
                .groupBy { it.menu }

        cell("$start\n ... \n$end", row = groupedByMenu.size)

        var totalCostForMonth = 0f
        for ((menu, byMenu) in groupedByMenu) {
            cell(menu.name)
            cell("${menu.cost}")

            val totalCount = byMenu.size
            val totalCost = totalCount * menu.cost

            totalCostForMonth += totalCost

            cell("$totalCount")
            cell("$totalCost")
        }


        cell(Text["orders-pdf-total:sum-for-a-month"], col = 3, border = SolidBorder(1f))
        cell("${allOrders.size}", border = SolidBorder(1f))
        cell("$totalCostForMonth", border = SolidBorder(1f))
    }
}

private val ORDERS_PDF_TOTAL_TRIGGER =
        (RoleTrigger(Producer) or RoleTrigger(Admin)) and
                StateTrigger(State.COMMAND) and
                CommandTrigger(Text["orders-pdf-total-command"])

val ORDERS_PDF_TOTAL = TriggerCommand(ORDERS_PDF_TOTAL_TRIGGER) { user, _ ->
    PDFQueue.schedule {
        val pdfTotal = transaction {
            createOrdersPDFTotal()
        }

        val pdfTotalFileName = Text["orders-pdf-total:file-name"]
        user.sendFile(pdfTotalFileName, "", pdfTotal)
    }
}
