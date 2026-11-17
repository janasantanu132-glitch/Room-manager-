package com.example.export

import android.content.Context
import com.example.data.local.RoomManagementDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure-Kotlin offline OpenXML (.xlsx) multi-sheet Excel exporter.
 */
class ExcelExporter(
    private val context: Context,
    private val db: RoomManagementDatabase
) {
    private data class SheetData(
        val name: String,
        val rows: List<List<String>>
    )

    suspend fun exportWorkbook(currency: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val buildings = db.buildingDao().getAllOnce()
            val rooms = db.roomDao().getAllOnce()
            val users = db.userDao().getAllUsersOnce()
            val electricity = db.electricityBillDao().getAllOnce()
            val rents = db.roomRentDao().getAllOnce()
            val slips = db.marketingSlipDao().getAllWithItemsOnce()
            val creditDebits = db.creditDebitDao().getAllOnce()

            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            val sheets = listOf(
                SheetData(
                    name = "Buildings",
                    rows = listOf(listOf("ID", "Name", "Location", "Description")) +
                        buildings.map { listOf(it.id.toString(), it.name, it.location, it.description) }
                ),
                SheetData(
                    name = "Rooms",
                    rows = listOf(listOf("ID", "BuildingID", "RoomNumber", "Location", "MonthlyRent ($currency)", "Advance ($currency)")) +
                        rooms.map {
                            listOf(
                                it.id.toString(),
                                it.buildingId.toString(),
                                it.roomNumber,
                                it.location,
                                fmt(it.monthlyRent),
                                fmt(it.advancePayment)
                            )
                        }
                ),
                SheetData(
                    name = "Members",
                    rows = listOf(listOf("ID", "UserID", "FullName", "Role", "BuildingID", "RoomID", "Status")) +
                        users.map {
                            listOf(
                                it.id.toString(),
                                it.userId,
                                it.fullName,
                                it.role,
                                it.buildingId?.toString() ?: "",
                                it.roomId?.toString() ?: "",
                                if (it.isBlocked) "Blocked" else "Active"
                            )
                        }
                ),
                SheetData(
                    name = "Electricity",
                    rows = listOf(listOf("Month", "Year", "Building", "Room", "OldReading", "NewReading", "Units", "Rate", "TotalBill ($currency)", "Members", "PerMemberShare")) +
                        electricity.map {
                            listOf(
                                it.billingMonth.toString(),
                                it.billingYear.toString(),
                                it.buildingNameSnapshot,
                                it.roomNumberSnapshot,
                                fmt(it.previousReading),
                                fmt(it.currentReading),
                                fmt(it.totalUnits),
                                fmt(it.ratePerUnit),
                                fmt(it.totalBill),
                                it.memberCount.toString(),
                                fmt(it.perMemberShare)
                            )
                        }
                ),
                SheetData(
                    name = "RoomRent",
                    rows = listOf(listOf("Month", "Year", "Building", "Room", "Member", "RoomRent", "Members", "Share", "Advance", "Paid", "Due", "Status")) +
                        rents.map {
                            listOf(
                                it.billingMonth.toString(),
                                it.billingYear.toString(),
                                it.buildingNameSnapshot,
                                it.roomNumberSnapshot,
                                it.memberNameSnapshot,
                                fmt(it.monthlyRoomRent),
                                it.memberCount.toString(),
                                fmt(it.individualShare),
                                fmt(it.advanceApplied),
                                fmt(it.paidAmount),
                                fmt(it.dueAmount),
                                if (it.isPaid) "Paid" else "Unpaid"
                            )
                        }
                ),
                SheetData(
                    name = "MarketingSlips",
                    rows = listOf(listOf("SlipNumber", "Date", "PaymentBy", "ProductName", "Quantity", "UnitPrice", "LineTotal", "SlipTotal ($currency)")) +
                        slips.flatMap { sw ->
                            val d = dateFmt.format(Date(sw.slip.dateMillis))
                            if (sw.items.isEmpty()) {
                                listOf(listOf(sw.slip.slipNumber, d, sw.slip.paymentByName, "-", "0", "0", "0", fmt(sw.slip.totalAmount)))
                            } else {
                                sw.items.map { item ->
                                    listOf(
                                        sw.slip.slipNumber,
                                        d,
                                        sw.slip.paymentByName,
                                        item.productName,
                                        fmt(item.quantity),
                                        fmt(item.unitPrice),
                                        fmt(item.lineTotal),
                                        fmt(sw.slip.totalAmount)
                                    )
                                }
                            }
                        }
                ),
                SheetData(
                    name = "CreditDebit",
                    rows = listOf(listOf("Date", "Month", "Year", "Member", "Credit ($currency)", "Debit ($currency)", "PrevBalance", "CurrentBalance", "Description")) +
                        creditDebits.map {
                            listOf(
                                dateFmt.format(Date(it.dateMillis)),
                                it.billingMonth.toString(),
                                it.billingYear.toString(),
                                it.memberNameSnapshot,
                                fmt(it.creditAmount),
                                fmt(it.debitAmount),
                                fmt(it.previousBalance),
                                fmt(it.currentBalance),
                                it.description
                            )
                        }
                )
            )

            val exportsDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(exportsDir, "room_management_$stamp.xlsx")
            writeXlsx(outFile, sheets)
            outFile
        }
    }

    private fun writeXlsx(file: File, sheets: List<SheetData>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(buildContentTypes(sheets.size).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(ROOT_RELS.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(buildWorkbookXml(sheets).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(buildWorkbookRels(sheets.size).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            sheets.forEachIndexed { idx, sheet ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${idx + 1}.xml"))
                zip.write(buildSheetXml(sheet.rows).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private fun buildContentTypes(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun buildWorkbookXml(sheets: List<SheetData>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        sheets.forEachIndexed { idx, sheet ->
            append("""<sheet name="${escapeXml(sheet.name)}" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun buildWorkbookRels(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("</Relationships>")
    }

    private fun buildSheetXml(rows: List<List<String>>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rIdx, row ->
            val rowNum = rIdx + 1
            append("""<row r="$rowNum">""")
            row.forEachIndexed { cIdx, value ->
                val colRef = colName(cIdx) + rowNum
                append("""<c r="$colRef" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun colName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A' + (n % 26)))
            n = n / 26 - 1
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)

    companion object {
        private const val ROOT_RELS =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
    }
}
