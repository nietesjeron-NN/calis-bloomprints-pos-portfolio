package com.calisbloomprints.pos.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import com.calisbloomprints.pos.R
import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.db.entity.PrinterSettingsEntity
import com.calisbloomprints.pos.data.model.MoneyFormatter
import com.calisbloomprints.pos.data.model.ReceiptWidth
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object EscPosReceiptBuilder {
    private val esc = 0x1B
    private val gs = 0x1D

    fun buildReceipt(
        context: Context,
        saleWithItems: SaleWithItems,
        settings: PrinterSettingsEntity,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val width = settings.width

        out.command(esc, '@'.code)
        out.alignCenter()
        if (settings.printLogoOnReceipts) {
            out.logo(context, settings.logoUri, width)
        }
        out.bold(true)
        out.textLine(settings.shopName)
        if (settings.shopSubtitle.isNotBlank()) {
            out.textLine(settings.shopSubtitle)
        }
        out.bold(false)
        out.textLine("Flower Shop POS")
        out.textLine("")

        out.alignLeft()
        out.textLine("Receipt: ${saleWithItems.sale.receiptNumber}")
        out.textLine("Date: ${dateLabel(saleWithItems.sale.createdAt)}")
        saleWithItems.sale.pickupAt?.let { out.textLine("Pickup: ${dateLabel(it)}") }
        saleWithItems.sale.customerName?.let { out.textLine("Customer: $it") }
        saleWithItems.sale.customerContact?.let { out.textLine("Contact: $it") }
        out.separator(width)

        saleWithItems.items.forEach { item ->
            out.textLine(wrapName("${item.quantity} x ${item.productName}", width.charsPerLine))
            out.textLine(twoColumn("  ${MoneyFormatter.format(item.unitPriceCents)}", MoneyFormatter.format(item.lineTotalCents), width.charsPerLine))
        }

        out.separator(width)
        out.textLine(twoColumn("Subtotal", MoneyFormatter.format(saleWithItems.sale.subtotalCents), width.charsPerLine))
        if (saleWithItems.sale.discountCents > 0L) {
            out.textLine(twoColumn("Discount", "-${MoneyFormatter.format(saleWithItems.sale.discountCents)}", width.charsPerLine))
        }
        if (saleWithItems.sale.serviceFeeCents > 0L) {
            out.textLine(twoColumn("Service fee", MoneyFormatter.format(saleWithItems.sale.serviceFeeCents), width.charsPerLine))
        }
        out.textLine(twoColumn("Total", MoneyFormatter.format(saleWithItems.sale.totalCents), width.charsPerLine))
        if (saleWithItems.sale.depositCents > 0L) {
            out.textLine(twoColumn("Deposit", MoneyFormatter.format(saleWithItems.sale.depositCents), width.charsPerLine))
            out.textLine(twoColumn("Balance due", MoneyFormatter.format(saleWithItems.sale.balanceDueCents), width.charsPerLine))
        }
        out.textLine(twoColumn("Cash", MoneyFormatter.format(saleWithItems.sale.amountTenderedCents), width.charsPerLine))
        out.textLine(twoColumn("Change", MoneyFormatter.format(saleWithItems.sale.changeCents), width.charsPerLine))

        saleWithItems.sale.notes?.let {
            out.separator(width)
            out.textLine("Notes:")
            it.chunked(width.charsPerLine).forEach { chunk -> out.textLine(chunk) }
        }

        out.textLine("")
        out.alignCenter()
        settings.receiptFooter
            .chunked(width.charsPerLine)
            .forEach { line -> out.textLine(line) }
        out.feed(lines = 4)
        out.partialCut()

        return out.toByteArray()
    }

    fun buildTestPrint(context: Context, settings: PrinterSettingsEntity): ByteArray {
        val out = ByteArrayOutputStream()
        val width = settings.width

        out.command(esc, '@'.code)
        out.alignCenter()
        if (settings.printLogoOnReceipts) {
            out.logo(context, settings.logoUri, width)
        }
        out.bold(true)
        out.textLine(settings.shopName)
        out.bold(false)
        out.textLine("Printer Test")
        out.textLine(settings.width.label)
        out.separator(width)
        out.alignLeft()
        out.textLine("Device: ${settings.deviceName ?: "Not selected"}")
        out.textLine("Width: ${settings.width.charsPerLine} chars")
        out.textLine("Status: Ready")
        out.textLine(dateLabel(System.currentTimeMillis()))
        out.feed(lines = 4)
        out.partialCut()

        return out.toByteArray()
    }

    fun buildReservationTicket(
        context: Context,
        saleWithItems: SaleWithItems,
        settings: PrinterSettingsEntity,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val width = settings.width

        out.command(esc, '@'.code)
        out.alignCenter()
        if (settings.printLogoOnReceipts) {
            out.logo(context, settings.logoUri, width)
        }
        out.bold(true)
        out.textLine(settings.shopName)
        out.bold(false)
        if (settings.shopSubtitle.isNotBlank()) {
            out.textLine(settings.shopSubtitle)
        }
        out.textLine("")
        out.bold(true)
        out.textLine("RESERVATION TICKET")
        out.bold(false)
        out.textLine("Flower pickup claim slip")
        out.textLine("")

        out.alignLeft()
        out.textLine("Order: ${saleWithItems.sale.receiptNumber}")
        out.textLine("Reserved: ${dateLabel(saleWithItems.sale.createdAt)}")
        out.textLine("Pickup: ${saleWithItems.sale.pickupAt?.let(::dateLabel) ?: "To be scheduled"}")
        out.textLine("Customer: ${saleWithItems.sale.customerName ?: "Walk-in customer"}")
        saleWithItems.sale.customerContact?.let { out.textLine("Contact: $it") }
        out.separator(width)

        out.textLine("Reserved items:")
        saleWithItems.items.forEach { item ->
            out.textLine(wrapName("${item.quantity} x ${item.productName}", width.charsPerLine))
            out.textLine(twoColumn("  ${MoneyFormatter.format(item.unitPriceCents)}", MoneyFormatter.format(item.lineTotalCents), width.charsPerLine))
        }

        out.separator(width)
        out.textLine(twoColumn("Total", MoneyFormatter.format(saleWithItems.sale.totalCents), width.charsPerLine))
        if (saleWithItems.sale.depositCents > 0L) {
            out.textLine(twoColumn("Deposit", MoneyFormatter.format(saleWithItems.sale.depositCents), width.charsPerLine))
        }
        out.textLine(twoColumn("Balance due", MoneyFormatter.format(saleWithItems.sale.balanceDueCents), width.charsPerLine))

        saleWithItems.sale.notes?.let {
            out.separator(width)
            out.textLine("Notes:")
            it.chunked(width.charsPerLine).forEach { chunk -> out.textLine(chunk) }
        }

        out.separator(width)
        out.alignCenter()
        listOf(
            "Please present this ticket",
            "when picking up your flowers.",
        ).forEach { line -> out.textLine(line) }
        out.feed(lines = 4)
        out.partialCut()

        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.command(vararg values: Int) {
        values.forEach { write(it) }
    }

    private fun ByteArrayOutputStream.textLine(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
        write('\n'.code)
    }

    private fun ByteArrayOutputStream.alignLeft() {
        command(esc, 'a'.code, 0)
    }

    private fun ByteArrayOutputStream.alignCenter() {
        command(esc, 'a'.code, 1)
    }

    private fun ByteArrayOutputStream.bold(enabled: Boolean) {
        command(esc, 'E'.code, if (enabled) 1 else 0)
    }

    private fun ByteArrayOutputStream.feed(lines: Int) {
        repeat(lines) { write('\n'.code) }
    }

    private fun ByteArrayOutputStream.partialCut() {
        command(gs, 'V'.code, 1)
    }

    private fun ByteArrayOutputStream.logo(context: Context, logoUri: String?, width: ReceiptWidth) {
        val bitmap = if (logoUri.isNullOrBlank()) {
            BitmapFactory.decodeResource(context.resources, R.drawable.calis_bloomprints_logo)
        } else {
            runCatching {
                context.contentResolver
                    .openInputStream(Uri.parse(logoUri))
                    ?.use(BitmapFactory::decodeStream)
            }.getOrNull() ?: BitmapFactory.decodeResource(context.resources, R.drawable.calis_bloomprints_logo)
        }

        val receiptBitmap = bitmap.prepareForReceipt(
            maxWidth = width.logoMaxDots,
            maxHeight = LOGO_MAX_HEIGHT_DOTS,
            paperWidth = width.paperDots,
        )
        writeRasterImage(receiptBitmap)
        textLine("")
    }

    private fun ByteArrayOutputStream.writeRasterImage(bitmap: Bitmap) {
        val widthBytes = (bitmap.width + 7) / 8
        command(
            gs,
            'v'.code,
            '0'.code,
            0,
            widthBytes and 0xFF,
            (widthBytes shr 8) and 0xFF,
            bitmap.height and 0xFF,
            (bitmap.height shr 8) and 0xFF,
        )

        for (y in 0 until bitmap.height) {
            for (xByte in 0 until widthBytes) {
                var value = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < bitmap.width && bitmap.getPixel(x, y).printsBlack()) {
                        value = value or (0x80 shr bit)
                    }
                }
                write(value)
            }
        }
        textLine("")
    }

    private fun ByteArrayOutputStream.separator(width: ReceiptWidth) {
        textLine("-".repeat(width.charsPerLine))
    }

    private fun twoColumn(left: String, right: String, width: Int): String {
        val safeLeft = left.take(width)
        val safeRight = right.take(width)
        val spaceCount = (width - safeLeft.length - safeRight.length).coerceAtLeast(1)
        return safeLeft + " ".repeat(spaceCount) + safeRight
    }

    private fun wrapName(value: String, width: Int): String {
        return if (value.length <= width) value else value.take(width - 1) + "."
    }

    private fun dateLabel(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(Date(timestamp))
    }

    private fun Bitmap.prepareForReceipt(maxWidth: Int, maxHeight: Int, paperWidth: Int): Bitmap {
        val scale = minOf(
            maxWidth.toFloat() / width.toFloat(),
            maxHeight.toFloat() / height.toFloat(),
        )
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
        val alignedWidth = ((paperWidth + 7) / 8) * 8

        val padded = Bitmap.createBitmap(alignedWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(padded)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(scaled, ((alignedWidth - targetWidth) / 2).toFloat(), 0f, null)
        return padded
    }

    private fun Int.printsBlack(): Boolean {
        val alpha = Color.alpha(this)
        if (alpha < 80) return false

        val red = blendWithWhite(Color.red(this), alpha)
        val green = blendWithWhite(Color.green(this), alpha)
        val blue = blendWithWhite(Color.blue(this), alpha)
        val luminance = (red * 0.299) + (green * 0.587) + (blue * 0.114)
        return luminance < BLACK_THRESHOLD
    }

    private fun blendWithWhite(channel: Int, alpha: Int): Int {
        return ((channel * alpha) + (255 * (255 - alpha))) / 255
    }

    private const val LOGO_MAX_HEIGHT_DOTS = 160
    private const val BLACK_THRESHOLD = 210
}
