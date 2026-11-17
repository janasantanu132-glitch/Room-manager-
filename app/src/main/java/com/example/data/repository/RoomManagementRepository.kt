package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.RoomManagementDatabase
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipEntity
import com.example.data.local.entity.MarketingSlipItemEntity
import com.example.data.local.entity.MarketingSlipWithItems
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomRentRecordEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.prefs.SessionPreferences
import com.example.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

sealed class LoginResult {
    data class Success(val user: UserEntity, val sessionToken: String) : LoginResult()
    data object InvalidCredentials : LoginResult()
    data object Blocked : LoginResult()
}

data class MarketingItemInput(
    val productName: String,
    val quantity: Double,
    val unitPrice: Double
)

class RoomManagementRepository(
    private val db: RoomManagementDatabase,
    private val prefs: SessionPreferences
) {
    private val userDao = db.userDao()
    private val buildingDao = db.buildingDao()
    private val roomDao = db.roomDao()
    private val electricityDao = db.electricityBillDao()
    private val rentDao = db.roomRentDao()
    private val slipDao = db.marketingSlipDao()
    private val creditDebitDao = db.creditDebitDao()

    val buildingsFlow: Flow<List<BuildingEntity>> = buildingDao.observeAll()
    val roomsFlow: Flow<List<RoomEntity>> = roomDao.observeAll()
    val membersFlow: Flow<List<UserEntity>> = userDao.observeAllMembers()
    val electricityBillsFlow: Flow<List<ElectricityBillEntity>> = electricityDao.observeAll()
    val rentRecordsFlow: Flow<List<RoomRentRecordEntity>> = rentDao.observeAll()
    val marketingSlipsFlow: Flow<List<MarketingSlipWithItems>> = slipDao.observeAllWithItems()
    val creditDebitFlow: Flow<List<CreditDebitTransactionEntity>> = creditDebitDao.observeAll()

    fun observeUserById(id: Long): Flow<UserEntity?> = userDao.observeById(id)

    suspend fun initialize() {
        db.ensureDefaultAdmin()
    }

    suspend fun login(userIdInput: String, passwordInput: String, rememberMe: Boolean): LoginResult {
        db.ensureDefaultAdmin()
        val trimmedId = userIdInput.trim()
        val user = userDao.getByUserId(trimmedId) ?: return LoginResult.InvalidCredentials
        if (!PasswordHasher.verifyPassword(passwordInput, user.passwordHash)) {
            return LoginResult.InvalidCredentials
        }
        if (user.isBlocked) {
            prefs.clearRememberedSessionIfMatches(user.userId)
            return LoginResult.Blocked
        }
        val token = PasswordHasher.generateSessionToken()
        userDao.updateSessionToken(user.id, token)
        if (rememberMe) {
            prefs.saveRememberedSession(user.userId, token)
        } else {
            prefs.clearRememberedSession()
        }
        return LoginResult.Success(user.copy(activeSessionToken = token), token)
    }

    suspend fun restoreRememberedSession(): Pair<UserEntity, String>? {
        db.ensureDefaultAdmin()
        val remembered = prefs.getRememberedSession() ?: return null
        val user = userDao.getByUserId(remembered.userId)
        if (user == null || user.isBlocked || user.activeSessionToken != remembered.sessionToken) {
            prefs.clearRememberedSession()
            return null
        }
        return user to remembered.sessionToken
    }

    suspend fun logout(userIdPk: Long?) {
        if (userIdPk != null) {
            userDao.updateSessionToken(userIdPk, null)
        }
        prefs.clearRememberedSession()
    }

    suspend fun clearRememberedSession() {
        prefs.clearRememberedSession()
    }

    suspend fun changeOwnPassword(
        actorId: Long,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        val user = userDao.getById(actorId)
            ?: return Result.failure(IllegalStateException("User not found"))
        if (user.isBlocked) {
            return Result.failure(SecurityException("Account is blocked"))
        }
        if (!PasswordHasher.verifyPassword(currentPassword, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect"))
        }
        if (newPassword.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
        }
        userDao.updatePasswordHash(user.id, PasswordHasher.hashPassword(newPassword))
        return Result.success(Unit)
    }

    private suspend fun requireActiveAdmin(actorId: Long): UserEntity {
        val actor = userDao.getById(actorId)
            ?: throw SecurityException("Unauthorized user")
        if (actor.isBlocked || actor.role != UserRole.ADMIN) {
            throw SecurityException("Administrator permission required")
        }
        return actor
    }

    // --- Buildings ---
    suspend fun saveBuilding(
        actorId: Long,
        id: Long = 0,
        name: String,
        location: String,
        description: String
    ): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Building name is required" }
        val entity = BuildingEntity(
            id = id,
            name = cleanName,
            location = location.trim(),
            description = description.trim()
        )
        if (id == 0L) buildingDao.insert(entity) else buildingDao.update(entity)
    }

    suspend fun deleteBuilding(actorId: Long, buildingId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        db.withTransaction {
            buildingDao.deleteById(buildingId)
        }
    }

    // --- Rooms ---
    suspend fun saveRoom(
        actorId: Long,
        id: Long = 0,
        buildingId: Long,
        roomNumber: String,
        location: String,
        monthlyRent: Double,
        advancePayment: Double
    ): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        val cleanRoomNo = roomNumber.trim()
        require(cleanRoomNo.isNotEmpty()) { "Room number is required" }
        require(monthlyRent >= 0.0) { "Rent cannot be negative" }
        require(advancePayment >= 0.0) { "Advance cannot be negative" }
        val entity = RoomEntity(
            id = id,
            buildingId = buildingId,
            roomNumber = cleanRoomNo,
            location = location.trim(),
            monthlyRent = monthlyRent,
            advancePayment = advancePayment
        )
        if (id == 0L) roomDao.insert(entity) else roomDao.update(entity)
    }

    suspend fun deleteRoom(actorId: Long, roomId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        db.withTransaction {
            roomDao.deleteById(roomId)
        }
    }

    // --- Members ---
    suspend fun createMember(
        actorId: Long,
        fullName: String,
        userId: String,
        password: String,
        buildingId: Long?,
        roomId: Long?
    ): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        val cleanName = fullName.trim()
        val cleanUid = userId.trim()
        require(cleanName.isNotEmpty()) { "Full name is required" }
        require(cleanUid.isNotEmpty()) { "User ID is required" }
        require(password.length >= 4) { "Password must be at least 4 characters" }
        require(userDao.getByUserId(cleanUid) == null) { "User ID '$cleanUid' already exists" }

        userDao.insert(
            UserEntity(
                userId = cleanUid,
                fullName = cleanName,
                passwordHash = PasswordHasher.hashPassword(password),
                role = UserRole.MEMBER,
                buildingId = buildingId,
                roomId = roomId,
                isBlocked = false
            )
        )
    }

    suspend fun updateMember(
        actorId: Long,
        memberId: Long,
        fullName: String,
        userId: String,
        buildingId: Long?,
        roomId: Long?
    ): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        val existing = userDao.getById(memberId) ?: error("Member not found")
        val cleanName = fullName.trim()
        val cleanUid = userId.trim()
        require(cleanName.isNotEmpty()) { "Full name is required" }
        require(cleanUid.isNotEmpty()) { "User ID is required" }
        val duplicate = userDao.getByUserId(cleanUid)
        require(duplicate == null || duplicate.id == memberId) { "User ID '$cleanUid' already exists" }

        userDao.update(
            existing.copy(
                fullName = cleanName,
                userId = cleanUid,
                buildingId = buildingId,
                roomId = roomId
            )
        )
    }

    suspend fun changeMemberPassword(
        actorId: Long,
        memberId: Long,
        newPassword: String
    ): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        require(newPassword.length >= 4) { "Password must be at least 4 characters" }
        userDao.updatePasswordHash(memberId, PasswordHasher.hashPassword(newPassword))
    }

    suspend fun blockMember(actorId: Long, memberId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        val target = userDao.getById(memberId) ?: error("Member not found")
        require(target.role != UserRole.ADMIN) { "Cannot block Administrator" }
        db.withTransaction {
            userDao.blockAndInvalidateSession(memberId)
        }
        prefs.clearRememberedSessionIfMatches(target.userId)
    }

    suspend fun unblockMember(actorId: Long, memberId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        userDao.unblockUser(memberId)
    }

    suspend fun deleteMember(actorId: Long, memberId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        val target = userDao.getById(memberId) ?: return@runCatching
        require(target.role != UserRole.ADMIN) { "Cannot delete Administrator" }
        db.withTransaction {
            userDao.deleteMemberById(memberId)
        }
        prefs.clearRememberedSessionIfMatches(target.userId)
    }

    // --- Electricity Bill ---
    suspend fun getLatestMeterReadingForRoom(roomId: Long): Double {
        return electricityDao.getLatestForRoom(roomId)?.currentReading ?: 0.0
    }

    suspend fun saveElectricityBill(
        actorId: Long,
        roomId: Long,
        month: Int,
        year: Int,
        previousReading: Double,
        currentReading: Double,
        ratePerUnit: Double,
        customMemberCount: Int?,
        notes: String
    ): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        require(currentReading >= previousReading) { "Current reading must be >= previous reading" }
        require(ratePerUnit >= 0.0) { "Rate per unit cannot be negative" }

        val room = roomDao.getById(roomId) ?: error("Room not found")
        val building = buildingDao.getById(room.buildingId)
        val activeMembers = userDao.getActiveMembersForRoom(roomId)
        val effectiveCount = (customMemberCount ?: activeMembers.size).coerceAtLeast(1)

        val totalUnits = currentReading - previousReading
        val totalBill = totalUnits * ratePerUnit
        val perMemberShare = totalBill / effectiveCount

        val breakdown = if (activeMembers.isNotEmpty()) {
            activeMembers.joinToString(", ") { member ->
                "${member.fullName}: ${String.format(Locale.US, "%.2f", perMemberShare)}"
            }
        } else {
            "$effectiveCount member(s): ${String.format(Locale.US, "%.2f", perMemberShare)} each"
        }

        electricityDao.insert(
            ElectricityBillEntity(
                buildingId = building?.id,
                roomId = room.id,
                buildingNameSnapshot = building?.name ?: "Unknown Building",
                roomNumberSnapshot = room.roomNumber,
                billingMonth = month,
                billingYear = year,
                previousReading = previousReading,
                currentReading = currentReading,
                totalUnits = totalUnits,
                ratePerUnit = ratePerUnit,
                totalBill = totalBill,
                memberCount = effectiveCount,
                perMemberShare = perMemberShare,
                memberBreakdownSnapshot = breakdown,
                notes = notes.trim()
            )
        )
    }

    suspend fun deleteElectricityBill(actorId: Long, billId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        electricityDao.deleteById(billId)
    }

    // --- Room Rent ---
    suspend fun generateRoomRentRecords(
        actorId: Long,
        roomId: Long,
        month: Int,
        year: Int,
        monthlyRentOverride: Double?,
        advanceAppliedTotal: Double,
        paidAmountPerMember: Double,
        isPaid: Boolean
    ): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        val room = roomDao.getById(roomId) ?: error("Room not found")
        val building = buildingDao.getById(room.buildingId)
        val activeMembers = userDao.getActiveMembersForRoom(roomId)
        val effectiveRent = monthlyRentOverride ?: room.monthlyRent
        require(effectiveRent >= 0.0) { "Rent cannot be negative" }

        db.withTransaction {
            if (activeMembers.isEmpty()) {
                val share = effectiveRent
                val advancePer = advanceAppliedTotal
                val paid = if (isPaid) max(0.0, share - advancePer) else paidAmountPerMember
                val due = max(0.0, share - advancePer - paid)
                rentDao.insert(
                    RoomRentRecordEntity(
                        buildingId = building?.id,
                        roomId = room.id,
                        memberId = null,
                        buildingNameSnapshot = building?.name ?: "Unknown Building",
                        roomNumberSnapshot = room.roomNumber,
                        memberNameSnapshot = "Room ${room.roomNumber}",
                        billingMonth = month,
                        billingYear = year,
                        monthlyRoomRent = effectiveRent,
                        memberCount = 1,
                        individualShare = share,
                        advanceApplied = advancePer,
                        paidAmount = paid,
                        dueAmount = due,
                        isPaid = due <= 0.001 || isPaid,
                        paidAt = if (isPaid || due <= 0.001) System.currentTimeMillis() else null
                    )
                )
            } else {
                val count = activeMembers.size
                val share = effectiveRent / count
                val advancePer = advanceAppliedTotal / count
                val records = activeMembers.map { member ->
                    val paid = if (isPaid) max(0.0, share - advancePer) else paidAmountPerMember
                    val due = max(0.0, share - advancePer - paid)
                    val settled = isPaid || due <= 0.001
                    RoomRentRecordEntity(
                        buildingId = building?.id,
                        roomId = room.id,
                        memberId = member.id,
                        buildingNameSnapshot = building?.name ?: "Unknown Building",
                        roomNumberSnapshot = room.roomNumber,
                        memberNameSnapshot = member.fullName,
                        billingMonth = month,
                        billingYear = year,
                        monthlyRoomRent = effectiveRent,
                        memberCount = count,
                        individualShare = share,
                        advanceApplied = advancePer,
                        paidAmount = paid,
                        dueAmount = if (settled) 0.0 else due,
                        isPaid = settled,
                        paidAt = if (settled) System.currentTimeMillis() else null
                    )
                }
                rentDao.insertAll(records)
            }
        }
    }

    suspend fun toggleRentPaidStatus(actorId: Long, record: RoomRentRecordEntity): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        val nextPaid = !record.isPaid
        val payable = max(0.0, record.individualShare - record.advanceApplied)
        if (nextPaid) {
            rentDao.updatePaymentStatus(
                id = record.id,
                isPaid = true,
                paidAmount = payable,
                dueAmount = 0.0,
                paidAt = System.currentTimeMillis()
            )
        } else {
            rentDao.updatePaymentStatus(
                id = record.id,
                isPaid = false,
                paidAmount = 0.0,
                dueAmount = payable,
                paidAt = null
            )
        }
    }

    suspend fun deleteRentRecord(actorId: Long, recordId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        rentDao.deleteById(recordId)
    }

    // --- Marketing Slip ---
    suspend fun generateNextSlipNumber(dateMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val count = slipDao.countForMonth(year, month) + 1
        return String.format(Locale.US, "MS-%04d%02d-%04d", year, month, count)
    }

    suspend fun saveMarketingSlip(
        actorId: Long,
        slipNumber: String,
        dateMillis: Long,
        paymentByMemberId: Long?,
        paymentByNameInput: String,
        notes: String,
        items: List<MarketingItemInput>
    ): Result<Long> = runCatching {
        requireActiveAdmin(actorId)
        val cleanSlipNo = slipNumber.trim()
        require(cleanSlipNo.isNotEmpty()) { "Slip number is required" }
        val validItems = items.filter { it.productName.isNotBlank() && it.quantity > 0.0 }
        require(validItems.isNotEmpty()) { "Add at least one valid product item" }

        val member = paymentByMemberId?.let { userDao.getById(it) }
        val payerName = member?.fullName ?: paymentByNameInput.trim().ifEmpty { "Cash / Admin" }

        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val entityItems = validItems.map { item ->
            MarketingSlipItemEntity(
                slipId = 0,
                productName = item.productName.trim(),
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                lineTotal = item.quantity * item.unitPrice
            )
        }
        val grandTotal = entityItems.sumOf { it.lineTotal }

        db.withTransaction {
            slipDao.saveSlipWithItems(
                slip = MarketingSlipEntity(
                    slipNumber = cleanSlipNo,
                    dateMillis = dateMillis,
                    billingMonth = month,
                    billingYear = year,
                    paymentByMemberId = member?.id,
                    paymentByName = payerName,
                    totalAmount = grandTotal,
                    notes = notes.trim()
                ),
                items = entityItems
            )
        }
    }

    suspend fun deleteMarketingSlip(actorId: Long, slipId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        slipDao.deleteSlipById(slipId)
    }

    // --- Credit / Debit ---
    suspend fun recordCreditDebit(
        actorId: Long,
        memberId: Long,
        dateMillis: Long,
        creditAmount: Double,
        debitAmount: Double,
        description: String
    ): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        require(creditAmount >= 0.0 && debitAmount >= 0.0) { "Amounts cannot be negative" }
        require(creditAmount > 0.0 || debitAmount > 0.0) { "Enter either credit or debit amount" }

        val member = userDao.getById(memberId) ?: error("Member not found")
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        db.withTransaction {
            val prevBalance = creditDebitDao.getLatestBalanceForMember(member.id) ?: 0.0
            val currentBalance = prevBalance + creditAmount - debitAmount
            creditDebitDao.insert(
                CreditDebitTransactionEntity(
                    memberId = member.id,
                    memberNameSnapshot = member.fullName,
                    dateMillis = dateMillis,
                    billingMonth = month,
                    billingYear = year,
                    creditAmount = creditAmount,
                    debitAmount = debitAmount,
                    previousBalance = prevBalance,
                    currentBalance = currentBalance,
                    description = description.trim()
                )
            )
        }
    }

    suspend fun deleteCreditDebit(actorId: Long, txId: Long): Result<Unit> = runCatching {
        requireActiveAdmin(actorId)
        creditDebitDao.deleteById(txId)
    }
}
