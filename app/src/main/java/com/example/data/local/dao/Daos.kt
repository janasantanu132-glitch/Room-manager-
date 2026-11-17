package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipEntity
import com.example.data.local.entity.MarketingSlipItemEntity
import com.example.data.local.entity.MarketingSlipWithItems
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomRentRecordEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE role = 'MEMBER' ORDER BY fullName ASC")
    fun observeAllMembers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY role ASC, fullName ASC")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Query("SELECT * FROM users WHERE roomId = :roomId AND role = 'MEMBER' AND isBlocked = 0 ORDER BY fullName ASC")
    suspend fun getActiveMembersForRoom(roomId: Long): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(users: List<UserEntity>)

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET activeSessionToken = :token WHERE id = :id")
    suspend fun updateSessionToken(id: Long, token: String?)

    @Query("UPDATE users SET passwordHash = :hash WHERE id = :id")
    suspend fun updatePasswordHash(id: Long, hash: String)

    @Query("UPDATE users SET isBlocked = 1, activeSessionToken = NULL WHERE id = :id")
    suspend fun blockAndInvalidateSession(id: Long)

    @Query("UPDATE users SET isBlocked = 0 WHERE id = :id")
    suspend fun unblockUser(id: Long)

    @Query("DELETE FROM users WHERE id = :id AND role != 'ADMIN'")
    suspend fun deleteMemberById(id: Long)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}

@Dao
interface BuildingDao {
    @Query("SELECT * FROM buildings ORDER BY name ASC")
    fun observeAll(): Flow<List<BuildingEntity>>

    @Query("SELECT * FROM buildings ORDER BY name ASC")
    suspend fun getAllOnce(): List<BuildingEntity>

    @Query("SELECT * FROM buildings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BuildingEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(building: BuildingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(buildings: List<BuildingEntity>)

    @Update
    suspend fun update(building: BuildingEntity)

    @Query("DELETE FROM buildings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM buildings")
    suspend fun deleteAll()
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY buildingId ASC, roomNumber ASC")
    fun observeAll(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms ORDER BY buildingId ASC, roomNumber ASC")
    suspend fun getAllOnce(): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(room: RoomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(rooms: List<RoomEntity>)

    @Update
    suspend fun update(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM rooms")
    suspend fun deleteAll()
}

@Dao
interface ElectricityBillDao {
    @Query("SELECT * FROM electricity_bills ORDER BY billingYear DESC, billingMonth DESC, recordedAt DESC")
    fun observeAll(): Flow<List<ElectricityBillEntity>>

    @Query("SELECT * FROM electricity_bills ORDER BY billingYear DESC, billingMonth DESC, recordedAt DESC")
    suspend fun getAllOnce(): List<ElectricityBillEntity>

    @Query("SELECT * FROM electricity_bills WHERE roomId = :roomId ORDER BY billingYear DESC, billingMonth DESC, recordedAt DESC LIMIT 1")
    suspend fun getLatestForRoom(roomId: Long): ElectricityBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: ElectricityBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(bills: List<ElectricityBillEntity>)

    @Query("DELETE FROM electricity_bills WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM electricity_bills")
    suspend fun deleteAll()
}

@Dao
interface RoomRentDao {
    @Query("SELECT * FROM room_rent_records ORDER BY billingYear DESC, billingMonth DESC, recordedAt DESC")
    fun observeAll(): Flow<List<RoomRentRecordEntity>>

    @Query("SELECT * FROM room_rent_records ORDER BY billingYear DESC, billingMonth DESC, recordedAt DESC")
    suspend fun getAllOnce(): List<RoomRentRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RoomRentRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<RoomRentRecordEntity>)

    @Update
    suspend fun update(record: RoomRentRecordEntity)

    @Query("UPDATE room_rent_records SET isPaid = :isPaid, paidAmount = :paidAmount, dueAmount = :dueAmount, paidAt = :paidAt WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, isPaid: Boolean, paidAmount: Double, dueAmount: Double, paidAt: Long?)

    @Query("DELETE FROM room_rent_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM room_rent_records")
    suspend fun deleteAll()
}

@Dao
interface MarketingSlipDao {
    @Transaction
    @Query("SELECT * FROM marketing_slips ORDER BY dateMillis DESC, createdAt DESC")
    fun observeAllWithItems(): Flow<List<MarketingSlipWithItems>>

    @Transaction
    @Query("SELECT * FROM marketing_slips ORDER BY dateMillis DESC, createdAt DESC")
    suspend fun getAllWithItemsOnce(): List<MarketingSlipWithItems>

    @Query("SELECT * FROM marketing_slips ORDER BY id DESC")
    suspend fun getAllSlipsOnce(): List<MarketingSlipEntity>

    @Query("SELECT * FROM marketing_slip_items ORDER BY id ASC")
    suspend fun getAllItemsOnce(): List<MarketingSlipItemEntity>

    @Query("SELECT COUNT(*) FROM marketing_slips WHERE billingYear = :year AND billingMonth = :month")
    suspend fun countForMonth(year: Int, month: Int): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSlip(slip: MarketingSlipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSlipsReplace(slips: List<MarketingSlipEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MarketingSlipItemEntity>)

    @Transaction
    suspend fun saveSlipWithItems(slip: MarketingSlipEntity, items: List<MarketingSlipItemEntity>): Long {
        val slipId = insertSlip(slip)
        val withSlipId = items.map { it.copy(slipId = slipId) }
        insertItems(withSlipId)
        return slipId
    }

    @Query("DELETE FROM marketing_slips WHERE id = :id")
    suspend fun deleteSlipById(id: Long)

    @Query("DELETE FROM marketing_slip_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM marketing_slips")
    suspend fun deleteAllSlips()
}

@Dao
interface CreditDebitDao {
    @Query("SELECT * FROM credit_debit_transactions ORDER BY dateMillis DESC, recordedAt DESC, id DESC")
    fun observeAll(): Flow<List<CreditDebitTransactionEntity>>

    @Query("SELECT * FROM credit_debit_transactions ORDER BY dateMillis DESC, recordedAt DESC, id DESC")
    suspend fun getAllOnce(): List<CreditDebitTransactionEntity>

    @Query("SELECT currentBalance FROM credit_debit_transactions WHERE memberId = :memberId ORDER BY dateMillis DESC, recordedAt DESC, id DESC LIMIT 1")
    suspend fun getLatestBalanceForMember(memberId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: CreditDebitTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(txs: List<CreditDebitTransactionEntity>)

    @Query("DELETE FROM credit_debit_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM credit_debit_transactions")
    suspend fun deleteAll()
}
