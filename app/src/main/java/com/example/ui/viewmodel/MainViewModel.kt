package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RoomManagementApp
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipWithItems
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomRentRecordEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.repository.LoginResult
import com.example.data.repository.MarketingItemInput
import com.example.export.LocalBackupInfo
import com.example.i18n.AppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

sealed class AuthState {
    data object Checking : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: UserEntity, val sessionToken: String) : AuthState()
}

sealed class UiEvent {
    data class Message(val text: String) : UiEvent()
    data class ShareFile(val intent: Intent) : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RoomManagementApp
    private val repository = app.repository
    private val prefs = app.sessionPreferences
    private val backupManager = app.backupManager
    private val pdfExporter = app.pdfExporter
    private val excelExporter = app.excelExporter

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _localBackups = MutableStateFlow<List<LocalBackupInfo>>(emptyList())
    val localBackups: StateFlow<List<LocalBackupInfo>> = _localBackups.asStateFlow()

    val language: StateFlow<AppLanguage> = prefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.ENGLISH)

    val currencySymbol: StateFlow<String> = prefs.currencySymbolFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "৳")

    val buildings: StateFlow<List<BuildingEntity>> = repository.buildingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rooms: StateFlow<List<RoomEntity>> = repository.roomsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members: StateFlow<List<UserEntity>> = repository.membersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val electricityBills: StateFlow<List<ElectricityBillEntity>> = repository.electricityBillsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rentRecords: StateFlow<List<RoomRentRecordEntity>> = repository.rentRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketingSlips: StateFlow<List<MarketingSlipWithItems>> = repository.marketingSlipsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditDebits: StateFlow<List<CreditDebitTransactionEntity>> = repository.creditDebitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val nowCal = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(nowCal.get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(nowCal.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private var sessionWatchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initialize()
            val restored = repository.restoreRememberedSession()
            if (restored != null) {
                startAuthenticatedSession(restored.first, restored.second)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
            refreshLocalBackups()
        }
    }

    private fun startAuthenticatedSession(user: UserEntity, token: String) {
        _loginError.value = null
        _authState.value = AuthState.Authenticated(user, token)
        sessionWatchJob?.cancel()
        sessionWatchJob = viewModelScope.launch {
            repository.observeUserById(user.id).collect { latest ->
                if (latest == null || latest.isBlocked || latest.activeSessionToken != token) {
                    repository.clearRememberedSession()
                    if (latest?.isBlocked == true) {
                        _loginError.value = if (language.value == AppLanguage.BENGALI) {
                            "প্রবেশ নিষিদ্ধ: অ্যাডমিন আপনার অ্যাকাউন্ট ব্লক করেছেন"
                        } else {
                            "Access denied: Your account is blocked by Admin"
                        }
                    }
                    _authState.value = AuthState.Unauthenticated
                    sessionWatchJob?.cancel()
                } else {
                    _authState.value = AuthState.Authenticated(latest, token)
                }
            }
        }
    }

    private fun currentActorId(): Long? = (authState.value as? AuthState.Authenticated)?.user?.id

    fun isCurrentUserAdmin(): Boolean {
        val user = (authState.value as? AuthState.Authenticated)?.user
        return user != null && !user.isBlocked && user.role == UserRole.ADMIN
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun login(userId: String, password: String, rememberMe: Boolean) {
        if (userId.isBlank() || password.isBlank()) {
            _loginError.value = if (language.value == AppLanguage.BENGALI) {
                "অনুগ্রহ করে ইউজার আইডি এবং পাসওয়ার্ড দিন"
            } else {
                "Please enter both User ID and Password"
            }
            return
        }
        viewModelScope.launch {
            _isLoggingIn.value = true
            when (val result = repository.login(userId, password, rememberMe)) {
                is LoginResult.Success -> startAuthenticatedSession(result.user, result.sessionToken)
                LoginResult.InvalidCredentials -> {
                    _loginError.value = if (language.value == AppLanguage.BENGALI) {
                        "ভুল ইউজার আইডি অথবা পাসওয়ার্ড"
                    } else {
                        "Invalid User ID or Password"
                    }
                }
                LoginResult.Blocked -> {
                    _loginError.value = if (language.value == AppLanguage.BENGALI) {
                        "প্রবেশ নিষিদ্ধ: অ্যাডমিন আপনার অ্যাকাউন্ট ব্লক করেছেন"
                    } else {
                        "Access denied: Your account is blocked by Admin"
                    }
                }
            }
            _isLoggingIn.value = false
        }
    }

    fun logout() {
        val uid = currentActorId()
        sessionWatchJob?.cancel()
        viewModelScope.launch {
            repository.logout(uid)
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun setDashboardMonthYear(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch { prefs.setCurrencySymbol(symbol) }
    }

    fun changeOwnPassword(currentPass: String, newPass: String, onDone: (Boolean) -> Unit) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            val res = repository.changeOwnPassword(actorId, currentPass, newPass)
            res.onSuccess {
                _events.tryEmit(UiEvent.Message("Password updated securely"))
                onDone(true)
            }.onFailure { err ->
                _events.tryEmit(UiEvent.Message(err.message ?: "Failed to update password"))
                onDone(false)
            }
        }
    }

    // --- Buildings ---
    fun saveBuilding(id: Long = 0, name: String, location: String, description: String) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.saveBuilding(actorId, id, name, location, description)
                .onSuccess { _events.tryEmit(UiEvent.Message("Building saved")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error saving building")) }
        }
    }

    fun deleteBuilding(id: Long) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.deleteBuilding(actorId, id)
                .onSuccess { _events.tryEmit(UiEvent.Message("Building deleted")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error deleting building")) }
        }
    }

    // --- Rooms ---
    fun saveRoom(
        id: Long = 0,
        buildingId: Long,
        roomNumber: String,
        location: String,
        monthlyRent: Double,
        advancePayment: Double
    ) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.saveRoom(actorId, id, buildingId, roomNumber, location, monthlyRent, advancePayment)
                .onSuccess { _events.tryEmit(UiEvent.Message("Room saved")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error saving room")) }
        }
    }

    fun deleteRoom(id: Long) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.deleteRoom(actorId, id)
                .onSuccess { _events.tryEmit(UiEvent.Message("Room deleted")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error deleting room")) }
        }
    }

    // --- Members ---
    fun createMember(
        fullName: String,
        userId: String,
        password: String,
        buildingId: Long?,
        roomId: Long?
    ) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.createMember(actorId, fullName, userId, password, buildingId, roomId)
                .onSuccess { _events.tryEmit(UiEvent.Message("Member created")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error creating member")) }
        }
    }

    fun updateMember(
        memberId: Long,
        fullName: String,
        userId: String,
        buildingId: Long?,
        roomId: Long?
    ) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.updateMember(actorId, memberId, fullName, userId, buildingId, roomId)
                .onSuccess { _events.tryEmit(UiEvent.Message("Member updated")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error updating member")) }
        }
    }

    fun changeMemberPassword(memberId: Long, newPassword: String) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.changeMemberPassword(actorId, memberId, newPassword)
                .onSuccess { _events.tryEmit(UiEvent.Message("Member password updated")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error updating password")) }
        }
    }

    fun toggleBlockMember(member: UserEntity) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            val res = if (member.isBlocked) {
                repository.unblockMember(actorId, member.id)
            } else {
                repository.blockMember(actorId, member.id)
            }
            res.onSuccess {
                val msg = if (member.isBlocked) "${member.fullName} unblocked" else "${member.fullName} blocked & logged out"
                _events.tryEmit(UiEvent.Message(msg))
            }.onFailure {
                _events.tryEmit(UiEvent.Message(it.message ?: "Operation failed"))
            }
        }
    }

    fun deleteMember(memberId: Long) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.deleteMember(actorId, memberId)
                .onSuccess { _events.tryEmit(UiEvent.Message("Member deleted")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error deleting member")) }
        }
    }

    // --- Electricity ---
    suspend fun getLatestMeterReading(roomId: Long): Double {
        return repository.getLatestMeterReadingForRoom(roomId)
    }

    fun saveElectricityBill(
        roomId: Long,
        month: Int,
        year: Int,
        previousReading: Double,
        currentReading: Double,
        ratePerUnit: Double,
        customMemberCount: Int?,
        notes: String
    ) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.saveElectricityBill(
                actorId, roomId, month, year, previousReading, currentReading, ratePerUnit, customMemberCount, notes
            ).onSuccess {
                _events.tryEmit(UiEvent.Message("Electricity bill saved"))
            }.onFailure {
                _events.tryEmit(UiEvent.Message(it.message ?: "Error saving electricity bill"))
            }
        }
    }

    fun deleteElectricityBill(id: Long) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.deleteElectricityBill(actorId, id)
                .onSuccess { _events.tryEmit(UiEvent.Message("Electricity record deleted")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error")) }
        }
    }

    // --- Room Rent ---
    fun generateRoomRent(
        roomId: Long,
        month: Int,
        year: Int,
        monthlyRentOverride: Double?,
        advanceAppliedTotal: Double,
        paidAmountPerMember: Double,
        isPaid: Boolean
    ) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.generateRoomRentRecords(
                actorId, roomId, month, year, monthlyRentOverride, advanceAppliedTotal, paidAmountPerMember, isPaid
            ).onSuccess {
                _events.tryEmit(UiEvent.Message("Room rent records saved"))
            }.onFailure {
                _events.tryEmit(UiEvent.Message(it.message ?: "Error saving room rent"))
            }
        }
    }

    fun toggleRentPaid(record: RoomRentRecordEntity) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.toggleRentPaidStatus(actorId, record)
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error updating rent")) }
        }
    }

    fun deleteRentRecord(id: Long) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.deleteRentRecord(actorId, id)
                .onSuccess { _events.tryEmit(UiEvent.Message("Rent record deleted")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error")) }
        }
    }

    // --- Marketing Slip ---
    suspend fun nextSlipNumber(dateMillis: Long): String {
        return repository.generateNextSlipNumber(dateMillis)
    }

    fun saveMarketingSlip(
        slipNumber: String,
        dateMillis: Long,
        paymentByMemberId: Long?,
        paymentByNameInput: String,
        notes: String,
        items: List<MarketingItemInput>,
        onSaved: () -> Unit = {}
    ) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.saveMarketingSlip(
                actorId, slipNumber, dateMillis, paymentByMemberId, paymentByNameInput, notes, items
            ).onSuccess {
                _events.tryEmit(UiEvent.Message("Marketing receipt saved"))
                onSaved()
            }.onFailure {
                _events.tryEmit(UiEvent.Message(it.message ?: "Error saving marketing slip"))
            }
        }
    }

    fun deleteMarketingSlip(id: Long) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.deleteMarketingSlip(actorId, id)
                .onSuccess { _events.tryEmit(UiEvent.Message("Receipt deleted")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error")) }
        }
    }

    fun exportAndShareMarketingSlipPdf(slip: MarketingSlipWithItems) {
        viewModelScope.launch {
            pdfExporter.generateMarketingSlipPdf(slip, currencySymbol.value)
                .onSuccess { file ->
                    val intent = pdfExporter.createShareIntent(
                        file = file,
                        mimeType = "application/pdf",
                        title = "Marketing Receipt ${slip.slip.slipNumber}"
                    )
                    _events.tryEmit(UiEvent.ShareFile(intent))
                }.onFailure {
                    _events.tryEmit(UiEvent.Message(it.message ?: "Failed to generate PDF"))
                }
        }
    }

    // --- Credit / Debit ---
    fun recordCreditDebit(
        memberId: Long,
        dateMillis: Long,
        creditAmount: Double,
        debitAmount: Double,
        description: String
    ) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.recordCreditDebit(actorId, memberId, dateMillis, creditAmount, debitAmount, description)
                .onSuccess { _events.tryEmit(UiEvent.Message("Transaction saved")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error saving transaction")) }
        }
    }

    fun deleteCreditDebit(id: Long) {
        val actorId = currentActorId() ?: return
        viewModelScope.launch {
            repository.deleteCreditDebit(actorId, id)
                .onSuccess { _events.tryEmit(UiEvent.Message("Transaction deleted")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Error")) }
        }
    }

    // --- Backup & Exports ---
    fun refreshLocalBackups() {
        _localBackups.value = backupManager.listLocalBackups()
    }

    fun createLocalBackup() {
        viewModelScope.launch {
            backupManager.createLocalBackup()
                .onSuccess { file ->
                    refreshLocalBackups()
                    _events.tryEmit(UiEvent.Message("Backup saved: ${file.name}"))
                }.onFailure {
                    _events.tryEmit(UiEvent.Message(it.message ?: "Backup failed"))
                }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportBackupToUri(uri)
                .onSuccess { _events.tryEmit(UiEvent.Message("Backup file exported")) }
                .onFailure { _events.tryEmit(UiEvent.Message(it.message ?: "Export failed")) }
        }
    }

    fun restoreLocalBackup(info: LocalBackupInfo) {
        if (!isCurrentUserAdmin()) {
            _events.tryEmit(UiEvent.Message("Administrator permission required"))
            return
        }
        viewModelScope.launch {
            backupManager.restoreFromFile(File(info.filePath))
                .onSuccess {
                    _events.tryEmit(UiEvent.Message("Database restored successfully"))
                }.onFailure {
                    _events.tryEmit(UiEvent.Message(it.message ?: "Restore failed"))
                }
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        if (!isCurrentUserAdmin()) {
            _events.tryEmit(UiEvent.Message("Administrator permission required"))
            return
        }
        viewModelScope.launch {
            backupManager.restoreFromUri(uri)
                .onSuccess {
                    _events.tryEmit(UiEvent.Message("Database restored from file"))
                }.onFailure {
                    _events.tryEmit(UiEvent.Message(it.message ?: "Restore failed"))
                }
        }
    }

    fun shareLocalBackup(info: LocalBackupInfo) {
        val file = File(info.filePath)
        val intent = pdfExporter.createShareIntent(file, "application/json", info.fileName)
        _events.tryEmit(UiEvent.ShareFile(intent))
    }

    fun exportFullReportPdf(
        title: String = "Full Financial Report",
        electricity: List<ElectricityBillEntity> = electricityBills.value,
        rents: List<RoomRentRecordEntity> = rentRecords.value,
        slips: List<MarketingSlipWithItems> = marketingSlips.value,
        credits: List<CreditDebitTransactionEntity> = creditDebits.value
    ) {
        viewModelScope.launch {
            pdfExporter.generateFinancialReportPdf(
                title = title,
                currency = currencySymbol.value,
                electricityBills = electricity,
                rentRecords = rents,
                slips = slips,
                creditDebits = credits
            ).onSuccess { file ->
                val intent = pdfExporter.createShareIntent(file, "application/pdf", title)
                _events.tryEmit(UiEvent.ShareFile(intent))
            }.onFailure {
                _events.tryEmit(UiEvent.Message(it.message ?: "PDF export failed"))
            }
        }
    }

    fun exportExcelWorkbook() {
        viewModelScope.launch {
            excelExporter.exportWorkbook(currencySymbol.value)
                .onSuccess { file ->
                    val intent = pdfExporter.createShareIntent(
                        file = file,
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        title = "Room Management Workbook"
                    )
                    _events.tryEmit(UiEvent.ShareFile(intent))
                }.onFailure {
                    _events.tryEmit(UiEvent.Message(it.message ?: "Excel export failed"))
                }
        }
    }
}
