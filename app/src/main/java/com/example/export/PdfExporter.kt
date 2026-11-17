package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipWithItems
import com.example.data.local.entity.RoomRentRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter(private val context: Context) {

    private fun exportsDir(): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getShareUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun createShareIntent(file: File, mimeType: String, title: String): Intent {
        val uri = getShareUri(file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun generateMarketingSlipPdf(
        slipWithItems: MarketingSlipWithItems,
        currency: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val slip = slipWithItems.slip
            val items = slipWithItems.items
            val safeSlip = slip.slipNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(exportsDir(), "receipt_$safeSlip.pdf")
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(slip.dateMillis))

            val androidPdfWorked = runCatching {
                val pdf = PdfDocument()
                try {
                    val pageWidth = 595
                    val pageHeight = 842
                    var pageNumber = 1
                    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    var canvas = page.canvas

                    val headerPaint = Paint().apply {
                        color = Color.parseColor("#1D4ED8")
                        style = Paint.Style.FILL
                    }
                    val titlePaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 20f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val subWhitePaint = Paint().apply {
                        color = Color.parseColor("#E0E7FF")
                        textSize = 11f
                    }
                    val labelPaint = Paint().apply {
                        color = Color.parseColor("#1E293B")
                        textSize = 11.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val bodyPaint = Paint().apply {
                        color = Color.parseColor("#334155")
                        textSize = 11f
                    }
                    val linePaint = Paint().apply {
                        color = Color.parseColor("#CBD5E1")
                        strokeWidth = 1f
                    }
                    val totalBgPaint = Paint().apply {
                        color = Color.parseColor("#F3E8FF")
                        style = Paint.Style.FILL
                    }
                    val purpleTextPaint = Paint().apply {
                        color = Color.parseColor("#5B21B6")
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }

                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 96f, headerPaint)
                    canvas.drawText("ROOM MANAGEMENT", 36f, 40f, titlePaint)
                    canvas.drawText("Official Marketing / Shopping Receipt", 36f, 64f, subWhitePaint)

                    var y = 130f
                    canvas.drawText("Slip Number: ${slip.slipNumber}", 36f, y, labelPaint)
                    canvas.drawText("Date: $dateStr", 380f, y, labelPaint)
                    y += 22f
                    canvas.drawText("Payment By: ${slip.paymentByName}", 36f, y, bodyPaint)
                    if (slip.notes.isNotBlank()) {
                        y += 18f
                        canvas.drawText("Notes: ${slip.notes}", 36f, y, bodyPaint)
                    }
                    y += 24f

                    canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
                    y += 18f
                    canvas.drawText("#", 40f, y, labelPaint)
                    canvas.drawText("Product Name", 70f, y, labelPaint)
                    canvas.drawText("Qty", 320f, y, labelPaint)
                    canvas.drawText("Price", 390f, y, labelPaint)
                    canvas.drawText("Total", 480f, y, labelPaint)
                    y += 10f
                    canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
                    y += 20f

                    items.forEachIndexed { index, item ->
                        if (y > pageHeight - 100) {
                            pdf.finishPage(page)
                            pageNumber++
                            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                            canvas = page.canvas
                            y = 50f
                        }
                        canvas.drawText("${index + 1}", 40f, y, bodyPaint)
                        canvas.drawText(item.productName.take(36), 70f, y, bodyPaint)
                        canvas.drawText(formatQty(item.quantity), 320f, y, bodyPaint)
                        canvas.drawText("$currency${formatNum(item.unitPrice)}", 390f, y, bodyPaint)
                        canvas.drawText("$currency${formatNum(item.lineTotal)}", 480f, y, bodyPaint)
                        y += 22f
                    }

                    y += 8f
                    canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
                    y += 14f
                    canvas.drawRect(36f, y, pageWidth - 36f, y + 40f, totalBgPaint)
                    canvas.drawText("Grand Total Amount", 52f, y + 25f, purpleTextPaint)
                    canvas.drawText("$currency${formatNum(slip.totalAmount)}", 445f, y + 25f, purpleTextPaint)

                    y += 72f
                    canvas.drawText("Generated Offline - Room Management System", 36f, y, bodyPaint)

                    pdf.finishPage(page)
                    FileOutputStream(file).use { out -> pdf.writeTo(out) }
                } finally {
                    runCatching { pdf.close() }
                }
            }.isSuccess

            if (!androidPdfWorked) {
                val lines = buildList {
                    add("ROOM MANAGEMENT - MARKETING RECEIPT")
                    add("Slip Number: ${slip.slipNumber}   Date: $dateStr")
                    add("Payment By: ${slip.paymentByName}")
                    if (slip.notes.isNotBlank()) add("Notes: ${slip.notes}")
                    add("------------------------------------------------------------")
                    items.forEachIndexed { idx, item ->
                        add("${idx + 1}. ${item.productName} | Qty: ${formatQty(item.quantity)} x $currency${formatNum(item.unitPrice)} = $currency${formatNum(item.lineTotal)}")
                    }
                    add("------------------------------------------------------------")
                    add("GRAND TOTAL: $currency${formatNum(slip.totalAmount)}")
                }
                writeMinimalPdf(file, lines)
            }
            file
        }
    }

    suspend fun generateFinancialReportPdf(
        title: String,
        currency: String,
        electricityBills: List<ElectricityBillEntity>,
        rentRecords: List<RoomRentRecordEntity>,
        slips: List<MarketingSlipWithItems>,
        creditDebits: List<CreditDebitTransactionEntity>
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportsDir(), "financial_report_$stamp.pdf")

            val lines = buildList {
                add("ROOM MANAGEMENT - $title")
                add("Room Rent Total: $currency${formatNum(rentRecords.sumOf { it.individualShare })} (Due: $currency${formatNum(rentRecords.sumOf { it.dueAmount })})")
                add("Electricity Bills Total: $currency${formatNum(electricityBills.sumOf { it.totalBill })}")
                add("Marketing Expenses Total: $currency${formatNum(slips.sumOf { it.slip.totalAmount })}")
                add("Total Credit: $currency${formatNum(creditDebits.sumOf { it.creditAmount })} | Total Debit: $currency${formatNum(creditDebits.sumOf { it.debitAmount })}")
                add("")
                add("ROOM RENT RECORDS (${rentRecords.size})")
                rentRecords.forEach {
                    add("${it.billingMonth}/${it.billingYear} | ${it.buildingNameSnapshot} - Rm ${it.roomNumberSnapshot} | ${it.memberNameSnapshot} | Share: $currency${formatNum(it.individualShare)} | ${if (it.isPaid) "PAID" else "DUE $currency${formatNum(it.dueAmount)}"}")
                }
                add("")
                add("ELECTRICITY BILLS (${electricityBills.size})")
                electricityBills.forEach {
                    add("${it.billingMonth}/${it.billingYear} | ${it.buildingNameSnapshot} - Rm ${it.roomNumberSnapshot} | ${formatQty(it.previousReading)}->${formatQty(it.currentReading)} (${formatQty(it.totalUnits)}u) | Total: $currency${formatNum(it.totalBill)}")
                }
                add("")
                add("MARKETING SLIPS (${slips.size})")
                slips.forEach {
                    val d = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it.slip.dateMillis))
                    add("${it.slip.slipNumber} ($d) | By: ${it.slip.paymentByName} | Items: ${it.items.size} | Total: $currency${formatNum(it.slip.totalAmount)}")
                }
                add("")
                add("CREDIT / DEBIT TRANSACTIONS (${creditDebits.size})")
                creditDebits.forEach {
                    val d = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it.dateMillis))
                    add("$d | ${it.memberNameSnapshot} | Cr: +$currency${formatNum(it.creditAmount)} | Dr: -$currency${formatNum(it.debitAmount)} | Bal: $currency${formatNum(it.currentBalance)}")
                }
            }

            val androidPdfWorked = runCatching {
                val pdf = PdfDocument()
                try {
                    val pageWidth = 595
                    val pageHeight = 842
                    var pageNumber = 1
                    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    var canvas: Canvas = page.canvas

                    val headerPaint = Paint().apply { color = Color.parseColor("#1E3A8A") }
                    val titlePaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 18f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val textPaint = Paint().apply {
                        color = Color.parseColor("#1E293B")
                        textSize = 10f
                    }

                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 76f, headerPaint)
                    canvas.drawText("ROOM MANAGEMENT - $title", 32f, 44f, titlePaint)

                    var y = 104f
                    lines.drop(1).forEach { line ->
                        if (y + 18f > pageHeight - 40f) {
                            pdf.finishPage(page)
                            pageNumber++
                            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                            canvas = page.canvas
                            y = 48f
                        }
                        canvas.drawText(line.take(92), 32f, y, textPaint)
                        y += 16f
                    }

                    pdf.finishPage(page)
                    FileOutputStream(file).use { out -> pdf.writeTo(out) }
                } finally {
                    runCatching { pdf.close() }
                }
            }.isSuccess

            if (!androidPdfWorked) {
                writeMinimalPdf(file, lines)
            }
            file
        }
    }

    private fun writeMinimalPdf(file: File, lines: List<String>) {
        val contentStream = buildString {
            append("BT\n/F1 11 Tf\n36 800 Td\n15 TL\n")
            lines.forEach { raw ->
                val safe = raw
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace(Regex("[^\\x20-\\x7E]"), " ")
                append("($safe) Tj T*\n")
            }
            append("ET\n")
        }
        val streamBytes = contentStream.toByteArray(Charsets.US_ASCII)
        val objs = listOf(
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n",
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n",
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n",
            "4 0 obj << /Length ${streamBytes.size} >>\nstream\n${String(streamBytes, Charsets.US_ASCII)}endstream\nendobj\n",
            "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
        )
        val header = "%PDF-1.4\n"
        val sb = StringBuilder(header)
        val offsets = mutableListOf(0)
        objs.forEach { obj ->
            offsets.add(sb.length)
            sb.append(obj)
        }
        val xrefPos = sb.length
        sb.append("xref\n0 ${objs.size + 1}\n0000000000 65535 f \n")
        for (i in 1..objs.size) {
            sb.append(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        sb.append("trailer << /Size ${objs.size + 1} /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
        file.writeText(sb.toString(), Charsets.US_ASCII)
    }

    private fun formatNum(value: Double): String = String.format(Locale.US, "%,.2f", value)
    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
}
