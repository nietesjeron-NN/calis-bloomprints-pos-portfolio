package com.calisbloomprints.pos.report

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.model.MoneyFormatter
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SalesReportExporter {
    fun buildPdf(report: SalesReport): ByteArray {
        val document = PdfDocument()
        val writer = PdfReportWriter(document)
        writer.line("Cali's Bloomprints and Keepsakes", size = 16f, bold = true)
        writer.line("Sales Report", size = 14f, bold = true)
        writer.line("Generated: ${dateTimeLabel(report.generatedAt)}")
        writer.space()

        report.periods.forEach { period ->
            writer.line(period.label, size = 13f, bold = true)
            writer.line("${dateTimeLabel(period.startAt)} to ${dateTimeLabel(period.endAt)}")
            writer.summary(period)
            writer.space()

            if (period.sales.isEmpty()) {
                writer.line("No sales in this period.")
            } else {
                writer.tableHeader()
                period.sales.forEach { sale ->
                    writer.saleRow(sale)
                }
            }
            writer.space(lines = 2)
        }

        return writer.finish()
    }

    fun buildXlsx(report: SalesReport): ByteArray {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("Cali's Bloomprints and Keepsakes"))
        rows.add(listOf("Sales Report"))
        rows.add(listOf("Generated", dateTimeLabel(report.generatedAt)))
        rows.add(emptyList())

        report.periods.forEach { period ->
            rows.add(listOf(period.label))
            rows.add(listOf("From", dateTimeLabel(period.startAt), "To", dateTimeLabel(period.endAt)))
            rows.add(listOf("Sales", period.summary.saleCount.toString()))
            rows.add(listOf("Items Sold", period.itemCount.toString()))
            rows.add(listOf("Gross", MoneyFormatter.format(period.summary.grossSalesCents)))
            rows.add(listOf("Discounts", MoneyFormatter.format(period.summary.discountCents)))
            rows.add(listOf("Service Fees", MoneyFormatter.format(period.summary.serviceFeeCents)))
            rows.add(listOf("Net Sales", MoneyFormatter.format(period.summary.netSalesCents)))
            rows.add(listOf("Cash Received", MoneyFormatter.format(period.summary.cashReceivedCents)))
            rows.add(listOf("Change Given", MoneyFormatter.format(period.summary.changeGivenCents)))
            rows.add(listOf("Balance Due", MoneyFormatter.format(period.summary.balanceDueCents)))
            rows.add(emptyList())
            rows.add(listOf("Receipt", "Date", "Customer", "Contact", "Items", "Gross", "Discount", "Fee", "Total", "Cash", "Change", "Balance"))
            period.sales.forEach { sale ->
                rows.add(
                    listOf(
                        sale.sale.receiptNumber,
                        dateTimeLabel(sale.sale.createdAt),
                        sale.sale.customerName.orEmpty(),
                        sale.sale.customerContact.orEmpty(),
                        sale.items.joinToString { "${it.quantity} ${it.productName}" },
                        MoneyFormatter.format(sale.sale.subtotalCents),
                        MoneyFormatter.format(sale.sale.discountCents),
                        MoneyFormatter.format(sale.sale.serviceFeeCents),
                        MoneyFormatter.format(sale.sale.totalCents),
                        MoneyFormatter.format(sale.sale.amountTenderedCents),
                        MoneyFormatter.format(sale.sale.changeCents),
                        MoneyFormatter.format(sale.sale.balanceDueCents),
                    ),
                )
            }
            rows.add(emptyList())
            rows.add(emptyList())
        }

        return buildWorkbook(rows)
    }

    private fun buildWorkbook(rows: List<List<String>>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.textEntry("[Content_Types].xml", CONTENT_TYPES)
            zip.textEntry("_rels/.rels", ROOT_RELS)
            zip.textEntry("xl/workbook.xml", WORKBOOK)
            zip.textEntry("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.textEntry("xl/styles.xml", STYLES)
            zip.textEntry("xl/worksheets/sheet1.xml", worksheetXml(rows))
        }
        return output.toByteArray()
    }

    private fun worksheetXml(rows: List<List<String>>): String {
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<cols><col min="1" max="12" width="18" customWidth="1"/></cols><sheetData>""")
            rows.forEachIndexed { rowIndex, row ->
                val rowNumber = rowIndex + 1
                append("""<row r="$rowNumber">""")
                row.forEachIndexed { columnIndex, value ->
                    val cell = "${columnName(columnIndex + 1)}$rowNumber"
                    append("""<c r="$cell" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
    }

    private fun columnName(index: Int): String {
        var value = index
        val name = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            name.insert(0, ('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return name.toString()
    }

    private fun ZipOutputStream.textEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun dateTimeLabel(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(Date(timestamp))
    }

    private class PdfReportWriter(private val document: PdfDocument) {
        private val output = ByteArrayOutputStream()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = MARGIN

        init {
            newPage()
        }

        fun line(text: String, size: Float = 10f, bold: Boolean = false) {
            ensureSpace(18)
            paint.color = Color.BLACK
            paint.textSize = size
            paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            page?.canvas?.drawText(text.take(95), MARGIN.toFloat(), y.toFloat(), paint)
            y += (size + 8).toInt()
        }

        fun space(lines: Int = 1) {
            y += 12 * lines
        }

        fun summary(period: SalesReportPeriod) {
            line("Sales: ${period.summary.saleCount}    Items: ${period.itemCount}")
            line("Gross: ${MoneyFormatter.format(period.summary.grossSalesCents)}    Discounts: ${MoneyFormatter.format(period.summary.discountCents)}")
            line("Service Fees: ${MoneyFormatter.format(period.summary.serviceFeeCents)}    Net Sales: ${MoneyFormatter.format(period.summary.netSalesCents)}", bold = true)
            line("Cash: ${MoneyFormatter.format(period.summary.cashReceivedCents)}    Change: ${MoneyFormatter.format(period.summary.changeGivenCents)}    Balance: ${MoneyFormatter.format(period.summary.balanceDueCents)}")
        }

        fun tableHeader() {
            ensureSpace(20)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            page?.canvas?.drawText("Date", MARGIN.toFloat(), y.toFloat(), paint)
            page?.canvas?.drawText("Receipt", 130f, y.toFloat(), paint)
            page?.canvas?.drawText("Customer", 240f, y.toFloat(), paint)
            page?.canvas?.drawText("Total", 430f, y.toFloat(), paint)
            page?.canvas?.drawText("Balance", 500f, y.toFloat(), paint)
            y += 14
        }

        fun saleRow(sale: SaleWithItems) {
            ensureSpace(34)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.2f
            page?.canvas?.drawText(shortDateLabel(sale.sale.createdAt), MARGIN.toFloat(), y.toFloat(), paint)
            page?.canvas?.drawText(sale.sale.receiptNumber.take(18), 130f, y.toFloat(), paint)
            page?.canvas?.drawText((sale.sale.customerName ?: "Walk-in").take(26), 240f, y.toFloat(), paint)
            page?.canvas?.drawText(MoneyFormatter.format(sale.sale.totalCents), 430f, y.toFloat(), paint)
            page?.canvas?.drawText(MoneyFormatter.format(sale.sale.balanceDueCents), 500f, y.toFloat(), paint)
            y += 12
            page?.canvas?.drawText(sale.items.joinToString { "${it.quantity} ${it.productName}" }.take(92), MARGIN.toFloat(), y.toFloat(), paint)
            y += 16
        }

        fun finish(): ByteArray {
            page?.let(document::finishPage)
            document.writeTo(output)
            document.close()
            return output.toByteArray()
        }

        private fun ensureSpace(height: Int) {
            if (y + height < PAGE_HEIGHT - MARGIN) return
            page?.let(document::finishPage)
            newPage()
        }

        private fun newPage() {
            pageNumber += 1
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
            )
            y = MARGIN
        }

        private fun shortDateLabel(timestamp: Long): String {
            return SimpleDateFormat("MM/dd hh:mm a", Locale.US).format(Date(timestamp))
        }

        private companion object {
            const val PAGE_WIDTH = 595
            const val PAGE_HEIGHT = 842
            const val MARGIN = 36
        }
    }

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""
    private const val ROOT_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
    private const val WORKBOOK =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Sales Report" sheetId="1" r:id="rId1"/></sheets></workbook>"""
    private const val WORKBOOK_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts><fills count="1"><fill><patternFill patternType="none"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf/></cellStyleXfs><cellXfs count="1"><xf xfId="0"/></cellXfs></styleSheet>"""
}
