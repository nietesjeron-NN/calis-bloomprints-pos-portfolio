package com.calisbloomprints.pos.barcode

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import com.calisbloomprints.pos.data.db.entity.ProductEntity
import com.calisbloomprints.pos.data.model.MoneyFormatter
import java.io.FileOutputStream
import kotlin.math.ceil

class BarcodeSheetPrintAdapter(
    private val context: Context,
    private val jobName: String,
    products: List<ProductEntity>,
    private val repeatSingleProduct: Boolean,
) : PrintDocumentAdapter() {
    private var attributes: PrintAttributes? = null
    private val labelProducts = if (repeatSingleProduct && products.isNotEmpty()) {
        List(LABELS_PER_PAGE) { products.first() }
    } else {
        products
    }

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        attributes = newAttributes
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }

        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount())
                .build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        val printAttributes = attributes ?: PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

        val document = PrintedPdfDocument(context, printAttributes)
        try {
            for (pageIndex in 0 until pageCount()) {
                if (cancellationSignal.isCanceled) {
                    callback.onWriteCancelled()
                    return
                }

                val page = document.startPage(pageIndex + 1)
                drawPage(page.canvas, page.info.pageWidth.toFloat(), page.info.pageHeight.toFloat(), pageIndex)
                document.finishPage(page)
            }

            FileOutputStream(destination.fileDescriptor).use { output ->
                document.writeTo(output)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (throwable: Throwable) {
            callback.onWriteFailed(throwable.message ?: "Could not print barcode sheet.")
        } finally {
            document.close()
        }
    }

    private fun drawPage(canvas: android.graphics.Canvas, pageWidth: Float, pageHeight: Float, pageIndex: Int) {
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val usableWidth = pageWidth - (MARGIN * 2)
        val usableHeight = pageHeight - (MARGIN * 2)
        val labelWidth = (usableWidth - (COLUMN_GAP * (COLUMNS - 1))) / COLUMNS
        val labelHeight = (usableHeight - (ROW_GAP * (ROWS - 1))) / ROWS
        val startIndex = pageIndex * LABELS_PER_PAGE

        for (indexOnPage in 0 until LABELS_PER_PAGE) {
            val product = labelProducts.getOrNull(startIndex + indexOnPage) ?: break
            val column = indexOnPage % COLUMNS
            val row = indexOnPage / COLUMNS
            val left = MARGIN + column * (labelWidth + COLUMN_GAP)
            val top = MARGIN + row * (labelHeight + ROW_GAP)
            drawLabel(canvas, product, RectF(left, top, left + labelWidth, top + labelHeight), paint)
        }
    }

    private fun drawLabel(canvas: android.graphics.Canvas, product: ProductEntity, bounds: RectF, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.6f
        paint.color = Color.rgb(215, 215, 215)
        canvas.drawRoundRect(bounds, 3f, 3f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText(product.name.take(26), bounds.centerX(), bounds.top + 13f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 7f
        canvas.drawText(MoneyFormatter.format(product.priceCents), bounds.centerX(), bounds.top + 24f, paint)

        val barcodeTop = bounds.top + 29f
        val barcodeBounds = RectF(bounds.left + 9f, barcodeTop, bounds.right - 9f, barcodeTop + 31f)
        Code128Barcode.draw(canvas, product.displayBarcode(), barcodeBounds, paint)

        paint.textSize = 7f
        canvas.drawText(product.displayBarcode(), bounds.centerX(), bounds.bottom - 7f, paint)
    }

    private fun pageCount(): Int {
        return ceil(labelProducts.size / LABELS_PER_PAGE.toFloat()).toInt().coerceAtLeast(1)
    }

    private companion object {
        const val COLUMNS = 3
        const val ROWS = 8
        const val LABELS_PER_PAGE = COLUMNS * ROWS
        const val MARGIN = 28f
        const val COLUMN_GAP = 8f
        const val ROW_GAP = 8f
    }
}
