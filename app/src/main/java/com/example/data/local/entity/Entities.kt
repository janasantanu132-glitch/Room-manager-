package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

object UserRole {
    const val ADMIN = "ADMIN"
    const val MEMBER = "MEMBER"
}

@Entity(tableName = "buildings")
data class BuildingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = BuildingEntity::class,
            parentColumns = ["id"],
            childColumns = ["buildingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["buildingId"])]
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val buildingId: Long,
    val roomNumber: String,
    val location: String = "",
    val monthlyRent: Double,
    val advancePayment: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = BuildingEntity::class,
            parentColumns = ["id"],
            childColumns = ["buildingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"], unique = true),
        Index(value = ["buildingId"]),
        Index(value = ["roomId"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val fullName: String,
    val passwordHash: String,
    val role: String = UserRole.MEMBER,
    val buildingId: Long? = null,
    val roomId: Long? = null,
    val isBlocked: Boolean = false,
    val activeSessionToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "electricity_bills",
    foreignKeys = [
        ForeignKey(
            entity = BuildingEntity::class,
            parentColumns = ["id"],
            childColumns = ["buildingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["buildingId"]),
        Index(value = ["roomId"]),
        Index(value = ["billingYear", "billingMonth"])
    ]
)
data class ElectricityBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val buildingId: Long?,
    val roomId: Long?,
    val buildingNameSnapshot: String,
    val roomNumberSnapshot: String,
    val billingMonth: Int,
    val billingYear: Int,
    val previousReading: Double,
    val currentReading: Double,
    val totalUnits: Double,
    val ratePerUnit: Double,
    val totalBill: Double,
    val memberCount: Int,
    val perMemberShare: Double,
    val memberBreakdownSnapshot: String = "",
    val notes: String = "",
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "room_rent_records",
    foreignKeys = [
        ForeignKey(
            entity = BuildingEntity::class,
            parentColumns = ["id"],
            childColumns = ["buildingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["buildingId"]),
        Index(value = ["roomId"]),
        Index(value = ["memberId"]),
        Index(value = ["billingYear", "billingMonth"])
    ]
)
data class RoomRentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val buildingId: Long?,
    val roomId: Long?,
    val memberId: Long?,
    val buildingNameSnapshot: String,
    val roomNumberSnapshot: String,
    val memberNameSnapshot: String,
    val billingMonth: Int,
    val billingYear: Int,
    val monthlyRoomRent: Double,
    val memberCount: Int,
    val individualShare: Double,
    val advanceApplied: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val isPaid: Boolean = false,
    val recordedAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null
)

@Entity(
    tableName = "marketing_slips",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentByMemberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["slipNumber"], unique = true),
        Index(value = ["paymentByMemberId"]),
        Index(value = ["billingYear", "billingMonth"])
    ]
)
data class MarketingSlipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slipNumber: String,
    val dateMillis: Long,
    val billingMonth: Int,
    val billingYear: Int,
    val paymentByMemberId: Long? = null,
    val paymentByName: String,
    val totalAmount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "marketing_slip_items",
    foreignKeys = [
        ForeignKey(
            entity = MarketingSlipEntity::class,
            parentColumns = ["id"],
            childColumns = ["slipId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["slipId"])]
)
data class MarketingSlipItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slipId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double
)

data class MarketingSlipWithItems(
    @Embedded val slip: MarketingSlipEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "slipId"
    )
    val items: List<MarketingSlipItemEntity>
)

@Entity(
    tableName = "credit_debit_transactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["memberId"]),
        Index(value = ["billingYear", "billingMonth"])
    ]
)
data class CreditDebitTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long?,
    val memberNameSnapshot: String,
    val dateMillis: Long,
    val billingMonth: Int,
    val billingYear: Int,
    val creditAmount: Double = 0.0,
    val debitAmount: Double = 0.0,
    val previousBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val description: String = "",
    val recordedAt: Long = System.currentTimeMillis()
)
