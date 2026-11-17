package com.example

import android.app.Application
import com.example.data.local.RoomManagementDatabase
import com.example.data.prefs.SessionPreferences
import com.example.data.repository.RoomManagementRepository
import com.example.export.BackupManager
import com.example.export.ExcelExporter
import com.example.export.PdfExporter

class RoomManagementApp : Application() {
    val database: RoomManagementDatabase by lazy { RoomManagementDatabase.getInstance(this) }
    val sessionPreferences: SessionPreferences by lazy { SessionPreferences(this) }
    val repository: RoomManagementRepository by lazy { RoomManagementRepository(database, sessionPreferences) }
    val backupManager: BackupManager by lazy { BackupManager(this, database) }
    val pdfExporter: PdfExporter by lazy { PdfExporter(this) }
    val excelExporter: ExcelExporter by lazy { ExcelExporter(this, database) }
}
