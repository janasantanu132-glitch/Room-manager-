package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.export.LocalBackupInfo
import com.example.i18n.LocalStrings
import com.example.ui.components.BrandGradientHeader
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionCard
import com.example.ui.components.formatDate
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VividPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    currentUser: UserEntity,
    localBackups: List<LocalBackupInfo>,
    onCreateLocalBackup: () -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onRestoreLocalBackup: (LocalBackupInfo) -> Unit,
    onRestoreBackupUri: (Uri) -> Unit,
    onShareBackup: (LocalBackupInfo) -> Unit,
    onExportFullPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isAdmin = currentUser.role == UserRole.ADMIN
    var restoreLocalTarget by remember { mutableStateOf<LocalBackupInfo?>(null) }
    var restoreUriPending by remember { mutableStateOf<Uri?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupUri(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) restoreUriPending = uri
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                }
                Text(
                    text = strings.backupRestoreTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            BrandGradientHeader(
                title = strings.backupAndRestore,
                subtitle = strings.backupPermanentNote
            )
        }

        item {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCreateLocalBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_local_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.createLocalBackup, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                exportBackupLauncher.launch("room_management_backup_$stamp.json")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportBackupFile)
                        }

                        if (isAdmin) {
                            OutlinedButton(
                                onClick = {
                                    importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.importRestoreBackup)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = onExportFullPdf,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_full_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportFullPdf)
                        }

                        FilledTonalButton(
                            onClick = onExportExcel,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_excel_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportExcel)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = strings.localBackupsList,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (localBackups.isEmpty()) {
            item { EmptyStateView(strings.noRecordsFound, Icons.Default.Backup) }
        } else {
            items(localBackups, key = { it.filePath }) { backup ->
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(backup.fileName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${formatDate(backup.lastModifiedMillis)} • ${(backup.sizeBytes / 1024).coerceAtLeast(1)} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onShareBackup(backup) }) {
                            Icon(Icons.Default.Share, contentDescription = strings.shareReceipt, tint = RoyalBlue)
                        }
                        if (isAdmin) {
                            FilledTonalButton(
                                onClick = { restoreLocalTarget = backup },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = VividPurple
                                )
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.restore)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    restoreLocalTarget?.let { target ->
        ConfirmActionDialog(
            title = strings.restore,
            message = strings.confirmRestoreMessage,
            confirmText = strings.restore,
            isDestructive = false,
            onConfirm = {
                onRestoreLocalBackup(target)
                restoreLocalTarget = null
            },
            onDismiss = { restoreLocalTarget = null }
        )
    }

    restoreUriPending?.let { uri ->
        ConfirmActionDialog(
            title = strings.importRestoreBackup,
            message = strings.confirmRestoreMessage,
            confirmText = strings.restore,
            isDestructive = false,
            onConfirm = {
                onRestoreBackupUri(uri)
                restoreUriPending = null
            },
            onDismiss = { restoreUriPending = null }
        )
    }
}
