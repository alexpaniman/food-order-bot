package org.order.logic.impl.commands.display.pdf

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.Color
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
import org.order.logic.impl.commands.BOLD_FONT_FOR_BUILDING_PDF_PATH
import org.order.logic.impl.commands.FONT_FOR_BUILDING_PDF_PATH
import java.io.File

private fun createFont(path: String) = PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H)!!

fun createPDF(fontPath: String = FONT_FOR_BUILDING_PDF_PATH, init: Document.() -> Unit): File {
    val file = createTempFile()

    val document = Document(
            PdfDocument(PdfWriter(file))
    ).apply {
        setFont(createFont(fontPath))
    }

    document.apply(init)

    document.close()
    return file
}

fun Document.text(text: String, bold: Boolean = false, fontSize: Float? = null, alignment: TextAlignment? = null) {
    val paragraph = Paragraph(text).apply {
        if (bold)
            setFont(createFont(BOLD_FONT_FOR_BUILDING_PDF_PATH))
        else
            setFont(createFont(FONT_FOR_BUILDING_PDF_PATH))

        if (fontSize != null)
            setFontSize(fontSize)

        if (alignment != null)
            setTextAlignment(TextAlignment.CENTER)
    }

    this.add(paragraph)
}

fun Document.section(title: String, bold: Boolean = false) =
        text(title, bold = bold, alignment = TextAlignment.CENTER, fontSize = 24f)

fun Document.table(vararg percents: Float, init: Table.() -> Unit) {
    val percentArray = UnitValue.createPercentArray(
            floatArrayOf(*percents)
    )

    val table = Table(percentArray).apply(init)
    this.add(table)
}

fun par(text: String, bold: Boolean = false, color: Color? = null) = Paragraph(text)
    .apply {
        if (bold)
            setFont(createFont(BOLD_FONT_FOR_BUILDING_PDF_PATH))
        else
            setFont(createFont(FONT_FOR_BUILDING_PDF_PATH))
        if (color != null)
            setFontColor(color)
    }

operator fun Paragraph.plus(other: Paragraph) = this.add(other)!!

fun Table.cell(text: String, row: Int = 1, col: Int = 1, border: Border? = null, bold: Boolean = false, padding: Float = 5f) {
    cell(par(text, bold), row, col, border, padding)
}

fun Table.cell(paragraph: Paragraph, row: Int = 1, col: Int = 1, border: Border? = null, padding: Float = 5f) {
    val cell = Cell(row, col).apply {
        if (border != null)
            setBorder(border)

        add(paragraph)

        setTextAlignment(TextAlignment.CENTER)
        setVerticalAlignment(VerticalAlignment.MIDDLE)
        setPadding(padding)
    }

    this.addCell(cell)
}