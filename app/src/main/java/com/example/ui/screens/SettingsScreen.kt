package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.i18n.AppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.BrandGradientHeader
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VividPurple

@Composable
fun SettingsScreen(
    currentUser: UserEntity,
    language: AppLanguage,
    currency: String,
    onLanguageChange: (AppLanguage) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onChangePassword: (String, String, (Boolean) -> Unit) -> Unit,
    onOpenBackup: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BrandGradientHeader(
                title = strings.settingsTitle,
                subtitle = "${currentUser.fullName} (${currentUser.userId})",
                trailing = {
                    StatusBadge(
                        text = if (currentUser.role == UserRole.ADMIN) strings.roleAdmin else strings.roleMember,
                        containerColor = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White
                    )
                }
            )
        }

        // Language Selection
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = RoyalBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(strings.languageSelection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { onLanguageChange(lang) },
                            label = { Text(lang.displayName) },
                            modifier = Modifier.testTag("settings_lang_${lang.code}")
                        )
                    }
                }
            }
        }

        // Currency Symbol Selection
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = VividPurple)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(strings.currencySymbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("৳", "₹", "$", "€").forEach { sym ->
                        FilterChip(
                            selected = currency == sym,
                            onClick = { onCurrencyChange(sym) },
                            label = { Text(sym, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }

        // Action Items
        item {
            SettingsActionCard(
                title = strings.changeOwnPassword,
                icon = Icons.Default.Lock,
                tint = RoyalBlue,
                onClick = { showPasswordDialog = true },
                modifier = Modifier.testTag("settings_change_password")
            )
        }

        item {
            SettingsActionCard(
                title = strings.backupAndRestore,
                icon = Icons.Default.Backup,
                tint = VividPurple,
                onClick = onOpenBackup,
                modifier = Modifier.testTag("settings_backup_restore")
            )
        }

        item {
            SettingsActionCard(
                title = strings.appInformation,
                icon = Icons.Default.Info,
                tint = RoyalBlue,
                onClick = { showAppInfoDialog = true },
                modifier = Modifier.testTag("settings_app_info")
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.logout, fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showPasswordDialog) {
        ChangeOwnPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { curr, next ->
                onChangePassword(curr, next) { ok ->
                    if (ok) showPasswordDialog = false
                }
            }
        )
    }

    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = { Text(strings.appInformation, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${strings.appName} v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold)
                    Text(strings.offlineBadge)
                    Text(strings.backupPermanentNote)
                    Text("Security: Salted PBKDF2WithHmacSHA256 + Immediate Block Session Revocation")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppInfoDialog = false }) { Text(strings.close) }
            }
        )
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = tint)
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun ChangeOwnPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (currentPass: String, newPass: String) -> Unit
) {
    val strings = LocalStrings.current
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    val valid = currentPass.isNotEmpty() && newPass.length >= 4 && newPass == confirmPass

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.changeOwnPassword, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPass,
                    onValueChange = { currentPass = it },
                    label = { Text(strings.currentPassword) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text(strings.newPassword) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text(strings.confirmPassword) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPass, newPass) },
                enabled = valid
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
