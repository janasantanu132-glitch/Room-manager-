package com.example.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.RoomManagementDatabase
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipEntity
import com.example.data.local.entity.MarketingSlipItemEntity
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomRentRecordEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalBackupInfo(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long
)

class BackupManager(
    private val context: Context,
    private val db: RoomManagementDatabase
) {
    private fun backupsDir(): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listLocalBackups(): List<LocalBackupInfo> {
        return backupsDir().listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                LocalBackupInfo(
                    fileName = it.name,
                    filePath = it.absolutePath,
                    sizeBytes = it.length(),
                    lastModifiedMillis = it.lastModified()
                )
            } ?: emptyList()
    }

    suspend fun createLocalBackup(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val json = exportDatabaseToJsonString()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupsDir(), "room_mgmt_backup_$timestamp.json")
            file.writeText(json, Charsets.UTF_8)
            file
        }
    }

    suspend fun exportBackupToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = exportDatabaseToJsonString()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Cannot open output stream")
        }
    }

    suspend fun restoreFromFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = file.readText(Charsets.UTF_8)
            restoreDatabaseFromJsonString(json)
        }
    }

    suspend fun restoreFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Cannot read selected backup file")
            restoreDatabaseFromJsonString(json)
        }
    }

    private suspend fun exportDatabaseToJsonString(): String {
        val buildings = db.buildingDao().getAllOnce()
        val rooms = db.roomDao().getAllOnce()
        val users = db.userDao().getAllUsersOnce()
        val electricity = db.electricityBillDao().getAllOnce()
        val rents = db.roomRentDao().getAllOnce()
        val slips = db.marketingSlipDao().getAllSlipsOnce()
        val slipItems = db.marketingSlipDao().getAllItemsOnce()
        val creditDebits = db.creditDebitDao().getAllOnce()

        val root = JSONObject()
        root.put("app", "RoomManagement")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("buildings", JSONArray().apply {
            buildings.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("location", b.location)
                    put("description", b.description)
                    put("createdAt", b.createdAt)
                })
            }
        })

        root.put("rooms", JSONArray().apply {
            rooms.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("buildingId", r.buildingId)
                    put("roomNumber", r.roomNumber)
                    put("location", r.location)
                    put("monthlyRent", r.monthlyRent)
                    put("advancePayment", r.advancePayment)
                    put("createdAt", r.createdAt)
                })
            }
        })

        root.put("users", JSONArray().apply {
            users.forEach { u ->
                put(JSONObject().apply {
                    put("id", u.id)
                    put("userId", u.userId)
                    put("fullName", u.fullName)
                    put("passwordHash", u.passwordHash)
                    put("role", u.role)
                    put("buildingId", u.buildingId ?: JSONObject.NULL)
                    put("roomId", u.roomId ?: JSONObject.NULL)
                    put("isBlocked", u.isBlocked)
                    put("createdAt", u.createdAt)
                })
            }
        })

        root.put("electricityBills", JSONArray().apply {
            electricity.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("buildingId", e.buildingId ?: JSONObject.NULL)
                    put("roomId", e.roomId ?: JSONObject.NULL)
                    put("buildingNameSnapshot", e.buildingNameSnapshot)
                    put("roomNumberSnapshot", e.roomNumberSnapshot)
                    put("billingMonth", e.billingMonth)
                    put("billingYear", e.billingYear)
                    put("previousReading", e.previousReading)
                    put("currentReading", e.currentReading)
                    put("totalUnits", e.totalUnits)
                    put("ratePerUnit", e.ratePerUnit)
                    put("totalBill", e.totalBill)
                    put("memberCount", e.memberCount)
                    put("perMemberShare", e.perMemberShare)
                    put("memberBreakdownSnapshot", e.memberBreakdownSnapshot)
                    put("notes", e.notes)
                    put("recordedAt", e.recordedAt)
                })
            }
        })

        root.put("roomRentRecords", JSONArray().apply {
            rents.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("buildingId", r.buildingId ?: JSONObject.NULL)
                    put("roomId", r.roomId ?: JSONObject.NULL)
                    put("memberId", r.memberId ?: JSONObject.NULL)
                    put("buildingNameSnapshot", r.buildingNameSnapshot)
                    put("roomNumberSnapshot", r.roomNumberSnapshot)
                    put("memberNameSnapshot", r.memberNameSnapshot)
                    put("billingMonth", r.billingMonth)
                    put("billingYear", r.billingYear)
                    put("monthlyRoomRent", r.monthlyRoomRent)
                    put("memberCount", r.memberCount)
                    put("individualShare", r.individualShare)
                    put("advanceApplied", r.advanceApplied)
                    put("paidAmount", r.paidAmount)
                    put("dueAmount", r.dueAmount)
                    put("isPaid", r.isPaid)
                    put("recordedAt", r.recordedAt)
                    put("paidAt", r.paidAt ?: JSONObject.NULL)
                })
            }
        })

        root.put("marketingSlips", JSONArray().apply {
            slips.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("slipNumber", s.slipNumber)
                    put("dateMillis", s.dateMillis)
                    put("billingMonth", s.billingMonth)
                    put("billingYear", s.billingYear)
                    put("paymentByMemberId", s.paymentByMemberId ?: JSONObject.NULL)
                    put("paymentByName", s.paymentByName)
                    put("totalAmount", s.totalAmount)
                    put("notes", s.notes)
                    put("createdAt", s.createdAt)
                })
            }
        })

        root.put("marketingSlipItems", JSONArray().apply {
            slipItems.forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id)
                    put("slipId", i.slipId)
                    put("productName", i.productName)
                    put("quantity", i.quantity)
                    put("unitPrice", i.unitPrice)
                    put("lineTotal", i.lineTotal)
                })
            }
        })

        root.put("creditDebitTransactions", JSONArray().apply {
            creditDebits.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("memberId", c.memberId ?: JSONObject.NULL)
                    put("memberNameSnapshot", c.memberNameSnapshot)
                    put("dateMillis", c.dateMillis)
                    put("billingMonth", c.billingMonth)
                    put("billingYear", c.billingYear)
                    put("creditAmount", c.creditAmount)
                    put("debitAmount", c.debitAmount)
                    put("previousBalance", c.previousBalance)
                    put("currentBalance", c.currentBalance)
                    put("description", c.description)
                    put("recordedAt", c.recordedAt)
                })
            }
        })

        return root.toString(2)
    }

    private suspend fun restoreDatabaseFromJsonString(jsonString: String) {
        val root = JSONObject(jsonString)
        require(root.optString("app") == "RoomManagement") { "Invalid Room Management backup file" }

        val buildings = buildList {
            val arr = root.optJSONArray("buildings") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    BuildingEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        location = o.optString("location", ""),
                        description = o.optString("description", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val rooms = buildList {
            val arr = root.optJSONArray("rooms") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    RoomEntity(
                        id = o.getLong("id"),
                        buildingId = o.getLong("buildingId"),
                        roomNumber = o.getString("roomNumber"),
                        location = o.optString("location", ""),
                        monthlyRent = o.optDouble("monthlyRent", 0.0),
                        advancePayment = o.optDouble("advancePayment", 0.0),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val users = buildList {
            val arr = root.optJSONArray("users") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    UserEntity(
                        id = o.getLong("id"),
                        userId = o.getString("userId"),
                        fullName = o.getString("fullName"),
                        passwordHash = o.getString("passwordHash"),
                        role = o.getString("role"),
                        buildingId = if (o.isNull("buildingId")) null else o.getLong("buildingId"),
                        roomId = if (o.isNull("roomId")) null else o.getLong("roomId"),
                        isBlocked = o.optBoolean("isBlocked", false),
                        activeSessionToken = null,
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val electricityBills = buildList {
            val arr = root.optJSONArray("electricityBills") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    ElectricityBillEntity(
                        id = o.getLong("id"),
                        buildingId = if (o.isNull("buildingId")) null else o.getLong("buildingId"),
                        roomId = if (o.isNull("roomId")) null else o.getLong("roomId"),
                        buildingNameSnapshot = o.optString("buildingNameSnapshot", ""),
                        roomNumberSnapshot = o.optString("roomNumberSnapshot", ""),
                        billingMonth = o.getInt("billingMonth"),
                        billingYear = o.getInt("billingYear"),
                        previousReading = o.getDouble("previousReading"),
                        currentReading = o.getDouble("currentReading"),
                        totalUnits = o.getDouble("totalUnits"),
                        ratePerUnit = o.getDouble("ratePerUnit"),
                        totalBill = o.getDouble("totalBill"),
                        memberCount = o.optInt("memberCount", 1),
                        perMemberShare = o.optDouble("perMemberShare", 0.0),
                        memberBreakdownSnapshot = o.optString("memberBreakdownSnapshot", ""),
                        notes = o.optString("notes", ""),
                        recordedAt = o.optLong("recordedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val rentRecords = buildList {
            val arr = root.optJSONArray("roomRentRecords") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    RoomRentRecordEntity(
                        id = o.getLong("id"),
                        buildingId = if (o.isNull("buildingId")) null else o.getLong("buildingId"),
                        roomId = if (o.isNull("roomId")) null else o.getLong("roomId"),
                        memberId = if (o.isNull("memberId")) null else o.getLong("memberId"),
                        buildingNameSnapshot = o.optString("buildingNameSnapshot", ""),
                        roomNumberSnapshot = o.optString("roomNumberSnapshot", ""),
                        memberNameSnapshot = o.optString("memberNameSnapshot", ""),
                        billingMonth = o.getInt("billingMonth"),
                        billingYear = o.getInt("billingYear"),
                        monthlyRoomRent = o.getDouble("monthlyRoomRent"),
                        memberCount = o.optInt("memberCount", 1),
                        individualShare = o.getDouble("individualShare"),
                        advanceApplied = o.optDouble("advanceApplied", 0.0),
                        paidAmount = o.optDouble("paidAmount", 0.0),
                        dueAmount = o.optDouble("dueAmount", 0.0),
                        isPaid = o.optBoolean("isPaid", false),
                        recordedAt = o.optLong("recordedAt", System.currentTimeMillis()),
                        paidAt = if (o.isNull("paidAt")) null else o.getLong("paidAt")
                    )
                )
            }
        }

        val slips = buildList {
            val arr = root.optJSONArray("marketingSlips") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    MarketingSlipEntity(
                        id = o.getLong("id"),
                        slipNumber = o.getString("slipNumber"),
                        dateMillis = o.getLong("dateMillis"),
                        billingMonth = o.getInt("billingMonth"),
                        billingYear = o.getInt("billingYear"),
                        paymentByMemberId = if (o.isNull("paymentByMemberId")) null else o.getLong("paymentByMemberId"),
                        paymentByName = o.optString("paymentByName", ""),
                        totalAmount = o.getDouble("totalAmount"),
                        notes = o.optString("notes", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val slipItems = buildList {
            val arr = root.optJSONArray("marketingSlipItems") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    MarketingSlipItemEntity(
                        id = o.getLong("id"),
                        slipId = o.getLong("slipId"),
                        productName = o.getString("productName"),
                        quantity = o.getDouble("quantity"),
                        unitPrice = o.getDouble("unitPrice"),
                        lineTotal = o.getDouble("lineTotal")
                    )
                )
            }
        }

        val creditDebits = buildList {
            val arr = root.optJSONArray("creditDebitTransactions") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    CreditDebitTransactionEntity(
                        id = o.getLong("id"),
                        memberId = if (o.isNull("memberId")) null else o.getLong("memberId"),
                        memberNameSnapshot = o.optString("memberNameSnapshot", ""),
                        dateMillis = o.getLong("dateMillis"),
                        billingMonth = o.getInt("billingMonth"),
                        billingYear = o.getInt("billingYear"),
                        creditAmount = o.optDouble("creditAmount", 0.0),
                        debitAmount = o.optDouble("debitAmount", 0.0),
                        previousBalance = o.optDouble("previousBalance", 0.0),
                        currentBalance = o.optDouble("currentBalance", 0.0),
                        description = o.optString("description", ""),
                        recordedAt = o.optLong("recordedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        db.withTransaction {
            db.creditDebitDao().deleteAll()
            db.marketingSlipDao().deleteAllItems()
            db.marketingSlipDao().deleteAllSlips()
            db.roomRentDao().deleteAll()
            db.electricityBillDao().deleteAll()
            db.userDao().deleteAll()
            db.roomDao().deleteAll()
            db.buildingDao().deleteAll()

            if (buildings.isNotEmpty()) db.buildingDao().insertAllReplace(buildings)
            if (rooms.isNotEmpty()) db.roomDao().insertAllReplace(rooms)
            if (users.isNotEmpty()) db.userDao().insertAllReplace(users)
            if (electricityBills.isNotEmpty()) db.electricityBillDao().insertAllReplace(electricityBills)
            if (rentRecords.isNotEmpty()) db.roomRentDao().insertAll(rentRecords)
            if (slips.isNotEmpty()) db.marketingSlipDao().insertAllSlipsReplace(slips)
            if (slipItems.isNotEmpty()) db.marketingSlipDao().insertItems(slipItems)
            if (creditDebits.isNotEmpty()) db.creditDebitDao().insertAllReplace(creditDebits)
        }
        db.ensureDefaultAdmin()
    }
}
