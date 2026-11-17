package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.RoomManagementDatabase
import com.example.data.prefs.SessionPreferences
import com.example.data.repository.LoginResult
import com.example.data.repository.MarketingItemInput
import com.example.data.repository.RoomManagementRepository
import com.example.export.BackupManager
import com.example.export.ExcelExporter
import com.example.export.PdfExporter
import com.example.security.PasswordHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: RoomManagementDatabase
    private lateinit var prefs: SessionPreferences
    private lateinit var repo: RoomManagementRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, RoomManagementDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = SessionPreferences(context)
        repo = RoomManagementRepository(db, prefs)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches Room Management`() {
        assertEquals("Room Management", context.getString(R.string.app_name))
    }

    @Test
    fun `password hashing is salted and verifies properly`() {
        val h1 = PasswordHasher.hashPassword("secret123")
        val h2 = PasswordHasher.hashPassword("secret123")
        assertNotEquals(h1, h2)
        assertTrue(PasswordHasher.verifyPassword("secret123", h1))
        assertFalse(PasswordHasher.verifyPassword("wrong", h1))
    }

    @Test
    fun `default admin login and member blocking invalidates session and remember me`() = runTest {
        val adminLogin = repo.login("admin", "admin123", rememberMe = true)
        assertTrue(adminLogin is LoginResult.Success)
        val adminId = (adminLogin as LoginResult.Success).user.id

        // Create Building, Room, and Member
        repo.saveBuilding(adminId, 0L, "Sunrise Tower", "Block A", "Main residential").getOrThrow()
        val bId = db.buildingDao().getAllOnce().first().id
        repo.saveRoom(adminId, 0L, bId, "101", "1st Floor", 12000.0, 5000.0).getOrThrow()
        val rId = db.roomDao().getAllOnce().first().id
        repo.createMember(adminId, "Rahim Uddin", "rahim01", "pass1234", bId, rId).getOrThrow()

        // Member logs in with Remember Me
        val memberLogin = repo.login("rahim01", "pass1234", rememberMe = true)
        assertTrue(memberLogin is LoginResult.Success)
        val member = (memberLogin as LoginResult.Success).user
        assertTrue(repo.restoreRememberedSession() != null)

        // Admin blocks member -> immediately invalidates session & Remember Me
        repo.blockMember(adminId, member.id).getOrThrow()
        assertNull(repo.restoreRememberedSession())
        val blockedAttempt = repo.login("rahim01", "pass1234", rememberMe = true)
        assertTrue(blockedAttempt is LoginResult.Blocked)
    }

    @Test
    fun `electricity rent marketing credit debit backup pdf and excel work end to end`() = runTest {
        val admin = (repo.login("admin", "admin123", rememberMe = false) as LoginResult.Success).user

        repo.saveBuilding(admin.id, 0L, "Blue Heights", "North Rd", "").getOrThrow()
        val bId = db.buildingDao().getAllOnce().first().id
        repo.saveRoom(admin.id, 0L, bId, "202", "2nd Floor", 9000.0, 3000.0).getOrThrow()
        val rId = db.roomDao().getAllOnce().first().id

        repo.createMember(admin.id, "Alice", "alice1", "pass1111", bId, rId).getOrThrow()
        repo.createMember(admin.id, "Bob", "bob1", "pass2222", bId, rId).getOrThrow()
        val members = repo.membersFlow.first()
        assertEquals(2, members.size)

        // Electricity: (250 - 150) * 8.5 = 850.0, split across 2 members = 425.0 each
        repo.saveElectricityBill(admin.id, rId, 11, 2026, 150.0, 250.0, 8.5, null, "Nov meter").getOrThrow()
        val elec = db.electricityBillDao().getAllOnce().first()
        assertEquals(100.0, elec.totalUnits, 0.01)
        assertEquals(850.0, elec.totalBill, 0.01)
        assertEquals(425.0, elec.perMemberShare, 0.01)

        // Room Rent: 9000 / 2 = 4500 each
        repo.generateRoomRentRecords(admin.id, rId, 11, 2026, null, 0.0, 1500.0, false).getOrThrow()
        val rents = db.roomRentDao().getAllOnce()
        assertEquals(2, rents.size)
        assertEquals(4500.0, rents[0].individualShare, 0.01)
        assertEquals(3000.0, rents[0].dueAmount, 0.01)

        // Marketing Slip
        val slipId = repo.saveMarketingSlip(
            actorId = admin.id,
            slipNumber = "MS-202611-0001",
            dateMillis = System.currentTimeMillis(),
            paymentByMemberId = members[0].id,
            paymentByNameInput = "",
            notes = "Groceries",
            items = listOf(
                MarketingItemInput("Rice", 5.0, 70.0),
                MarketingItemInput("Oil", 2.0, 180.0)
            )
        ).getOrThrow()
        assertTrue(slipId > 0L)
        val slips = db.marketingSlipDao().getAllWithItemsOnce()
        assertEquals(710.0, slips.first().slip.totalAmount, 0.01)

        // Credit / Debit running balance
        repo.recordCreditDebit(admin.id, members[0].id, System.currentTimeMillis(), 2000.0, 0.0, "Deposit").getOrThrow()
        repo.recordCreditDebit(admin.id, members[0].id, System.currentTimeMillis(), 0.0, 710.0, "Market").getOrThrow()
        val txs = db.creditDebitDao().getAllOnce()
        assertEquals(1290.0, txs.first().currentBalance, 0.01)

        // Backup & Restore round-trip
        val backupManager = BackupManager(context, db)
        val backupFile = backupManager.createLocalBackup().getOrThrow()
        assertTrue(backupFile.exists())
        backupManager.restoreFromFile(backupFile).getOrThrow()
        assertEquals(1, db.buildingDao().getAllOnce().size)
        assertEquals(2, db.roomRentDao().getAllOnce().size)

        // PDF & Excel generation
        val pdfExporter = PdfExporter(context)
        val slipPdf = pdfExporter.generateMarketingSlipPdf(slips.first(), "৳").getOrThrow()
        assertTrue(slipPdf.exists() && slipPdf.length() > 0L)

        val excelExporter = ExcelExporter(context, db)
        val xlsx = excelExporter.exportWorkbook("৳").getOrThrow()
        assertTrue(xlsx.exists() && xlsx.length() > 0L)
    }
}
