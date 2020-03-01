package org.order.logic.impl.commands.display

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import com.itextpdf.layout.property.VerticalAlignment
import org.joda.time.LocalDate
import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.bot.send.removeReply
import org.order.bot.send.reply
import org.order.data.entities.*
import org.order.data.entities.State.COMMAND
import org.order.data.tables.Orders
import org.order.data.tables.PollAnswers
import org.order.data.tables.PollComments
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.*
import org.order.logic.corpus.Text
import org.order.logic.impl.commands.BOLD_FONT_FOR_BUILDING_PDF_PATH
import org.order.logic.impl.commands.FONT_FOR_BUILDING_PDF_PATH
import org.order.logic.impl.utils.dayOfWeekAsLongText
import org.order.logic.impl.utils.dayOfWeekAsShortText
import org.order.logic.impl.utils.grade
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File
import org.order.data.entities.State.READ_DATE_FOR_PDF_POLLS_TOTAL
import org.order.logic.impl.utils.appendMainKeyboard

private fun Table.cell(text: String, row: Int = 1, col: Int = 1, border: Boolean = true, font: PdfFont? = null, padding: Float = 5f) {
    val paragraph = Paragraph(text)
    if (font != null)
        paragraph.setFont(font)

    val cell = Cell(row, col).apply {
        if (!border)
            setBorder(Border.NO_BORDER)

        add(paragraph)

        setTextAlignment(TextAlignment.CENTER)
        setVerticalAlignment(VerticalAlignment.MIDDLE)
        setPadding(padding)
    }

    this.addCell(cell)
}

private object AskPollsTotalDate: Question(READ_DATE_FOR_PDF_POLLS_TOTAL) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["polls-pdf-total:ask-date"]) {
                reply {
                    button(LocalDate.now().toString())
                }
            }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val text = update.message?.text
        val date = if (text != null)
            try {
                LocalDate.parse(text)
            } catch (exc: IllegalArgumentException) { null }
        else null

        if (date == null) {
            user.send(Text["polls-pdf-total:wrong-date"]) {
                removeReply()
            }
            return false
        }

        sendPollsPdfTotal(user, date)
        user.state = COMMAND
        return true
    }
}

private val POLLS_PDF_TOTAL_TRIGGER =
        (RoleTrigger(Producer) or RoleTrigger(Admin)) and
                StateTrigger(COMMAND) and
                CommandTrigger(Text["polls-pdf-total-command"])

val POLLS_PDF_TOTAL = QuestionSet(AskPollsTotalDate, trigger = POLLS_PDF_TOTAL_TRIGGER)

private data class Poll(val client: Client, val menu: Menu, val answers: List<PollAnswer>, val comment: PollComment?)

private fun SenderContext.sendPoll(user: User, date: LocalDate, file: File) {
    val fileName = Text.get("polls-pdf-total:file-name") {
        it["date"] = date.toString()
    }

    val caption = Text.get("polls-pdf-total:caption") {
        it["date"] = date.toString()
        it["day"] = date.dayOfWeekAsLongText.toLowerCase()
    }

    user.sendFile(fileName, caption, file) {
        appendMainKeyboard(user)
    }
}

private fun SenderContext.sendPollsPdfTotal(user: User, date: LocalDate) {
    val tempFile = createTempFile()

    val document = Document(
            PdfDocument(PdfWriter(tempFile))
    )
    val font = PdfFontFactory.createFont(
            FONT_FOR_BUILDING_PDF_PATH,
            PdfEncodings.IDENTITY_H
    )
    document.setFont(font)

    val boldFont = PdfFontFactory.createFont(
            BOLD_FONT_FOR_BUILDING_PDF_PATH,
            PdfEncodings.IDENTITY_H
    )

    val title = Text.get("polls-pdf-total") {
        it["date"] = date.toString()
        it["day"] = date.dayOfWeekAsShortText
    }

    document.add(Paragraph(title)
            .setFont(boldFont)
            .setFontSize(24f)
            .setTextAlignment(TextAlignment.CENTER)
    )
    document.add(Paragraph("\n\n"))

    val orders = Order
            .find { Orders.orderDate eq date.toString() }

    val polls = orders.map { order ->
        val client = order.client

        val answers = PollAnswer
                .find { PollAnswers.order eq order.id }
                .toList()

        val comments = PollComment
                .find { PollComments.order eq order.id }
                .firstOrNull()

        Poll(client, order.menu, answers, comments)
    }.sortedBy { it.client.user.grade }

    if (polls.isEmpty()) {
        document.add(Paragraph(Text["polls-pdf-total:empty"]))
        document.close()

        sendPoll(user, date, tempFile)
        return
    }

    document.add(Paragraph(Text["polls-pdf-total:sum-up"])
            .setFont(boldFont)
            .setFontSize(15f)
    )

    val sumUp = polls
            .flatMap { it.answers }
            .groupBy { it.dish }
            .mapValues { (_, answers) ->
                val rates = answers.map { it.rate }
                rates.sum() / rates.size.toFloat()
            }
            .toList()
            .groupBy { (dish, _) -> dish.menu }

    val sumUpTable = Table(
            UnitValue.createPercentArray(floatArrayOf(0.3f, 0.55f, 0.15f))
    )

    for ((menu, rates) in sumUp) sumUpTable.apply {
        val menuName = Text.get("polls-pdf-total:menu") {
            it["name"] = menu.name
        }

        cell(menuName, row = rates.size)

        for ((dish, rate) in rates) {
            cell(dish.name)
            cell("%.2f".format(rate))
        }
    }

    document.add(sumUpTable)
    document.add(Paragraph("\n"))

    document.add(Paragraph(Text["polls-pdf-total:full-list"])
            .setFont(boldFont)
            .setFontSize(15f)
    )

    val mainTable = Table(
            UnitValue.createPercentArray(floatArrayOf(0.2f, 0.15f, 0.5f, 0.15f))
    )
    for (poll in polls) mainTable.apply {
        val client = poll.client.user

        if (poll.answers.isNotEmpty()) {
            cell("", 1, 2, border = false)

            val menuName = Text.get("polls-pdf-total:menu") {
                it["name"] = poll.answers.first().dish.menu.name
            }
            cell(menuName, 1, 2)
        }

        cell(client.name!!, 3, 1)
        cell(client.grade, 3, 1)

        for (dishRate in poll.answers) {
            cell(dishRate.dish.name)
            cell(dishRate.rate.toString())
        }
        if (poll.comment?.text != null) {
            cell("", 1, 2, border = false)
            cell(poll.comment.text!!, 1, 2)
        }

        cell("", 1, 4, border = false)
    }
    document.add(mainTable)
    document.close()

    sendPoll(user, date, tempFile)
}