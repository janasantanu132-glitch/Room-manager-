package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BuildingDao
import com.example.data.local.dao.CreditDebitDao
import com.example.data.local.dao.ElectricityBillDao
import com.example.data.local.dao.MarketingSlipDao
import com.example.data.local.dao.RoomDao
import com.example.data.local.dao.RoomRentDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipEntity
import com.example.data.local.entity.MarketingSlipItemEntity
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomRentRecordEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.security.PasswordHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        BuildingEntity::class,
        RoomEntity::class,
        UserEntity::class,
        ElectricityBillEntity::class,
        RoomRentRecordEntity::class,
        MarketingSlipEntity::class,
        MarketingSlipItemEntity::class,
        CreditDebitTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RoomManagementDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun buildingDao(): BuildingDao
    abstract fun roomDao(): RoomDao
    abstract fun electricityBillDao(): ElectricityBillDao
    abstract fun roomRentDao(): RoomRentDao
    abstract fun marketingSlipDao(): MarketingSlipDao
    abstract fun creditDebitDao(): CreditDebitDao

    suspend fun ensureDefaultAdmin() {
        val existing = userDao().getByUserId("admin")
        if (existing == null) {
            userDao().insert(
                UserEntity(
                    userId = "admin",
                    fullName = "Administrator",
                    passwordHash = PasswordHasher.hashPassword("admin123"),
                    role = UserRole.ADMIN,
                    isBlocked = false
                )
            )
        }
    }

    companion object {
        const val DATABASE_NAME = "room_management.db"

        @Volatile
        private var INSTANCE: RoomManagementDatabase? = null

        fun getInstance(context: Context): RoomManagementDatabase {
            return INSTANCE ?: synchronized(this) {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoomManagementDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            scope.launch {
                                INSTANCE?.ensureDefaultAdmin()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
