package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.i18n.LocalStrings
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionCard
import com.example.ui.components.formatDate
import com.example.ui.components.formatMoney
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VividPurple
import java.util.Calendar

@Composable
fun CreditDebitScreen(
    currentUser: UserEntity,
    currency: String,
    members: List<UserEntity>,
    transactions: List<CreditDebitTransactionEntity>,
    onRecordTransaction: (Long, Long, Double, Double, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val isAdmin = currentUser.role == UserRole.ADMIN

    var selectedMemberId by remember(members, isAdmin) {
        mutableStateOf(
            if (isAdmin) members.firstOrNull()?.id else currentUser.id
        )
    }
    var memberExpanded by remember { mutableStateOf(false) }

    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var creditText by remember { mutableStateOf("") }
    var debitText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CreditDebitTransactionEntity?>(null) }

    val memberTxs = remember(selectedMemberId, transactions, isAdmin) {
        val targetId = if (isAdmin) selectedMemberId else currentUser.id
        if (targetId == null) transactions else transactions.filter { it.memberId == targetId }
    }

    val latestBalance = memberTxs.firstOrNull()?.currentBalance ?: 0.0
    val totalCr = memberTxs.sumOf { it.creditAmount }
    val totalDr = memberTxs.sumOf { it.debitAmount }

    val creditVal = creditText.toDoubleOrNull() ?: 0.0
    val debitVal = debitText.toDoubleOrNull() ?: 0.0
    val projectedBalance = latestBalance + creditVal - debitVal

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard {
                Text(
                    text = strings.memberAccount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isAdmin) {
                    Box {
                        val mName = members.firstOrNull { it.id == selectedMemberId }?.fullName ?: strings.selectMember
                        OutlinedButton(
                            onClick = { memberExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(mName, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false }) {
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("${m.fullName} (${m.userId})") },
                                    onClick = {
                                        selectedMemberId = m.id
                                        memberExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Text(
                        text = currentUser.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        color = RoyalBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.totalCredit, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "+${formatMoney(totalCr, currency)}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Column {
                            Text(strings.totalDebit, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "-${formatMoney(totalDr, currency)}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.currentBalance, style = MaterialTheme.typography.labelSmall)
                            Text(
                                formatMoney(latestBalance, currency),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = VividPurple
                            )
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                SectionCard {
                    Text(
                        text = strings.creditDebitTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (members.isEmpty()) {
                        Text(strings.noMembersYet, color = MaterialTheme.colorScheme.error)
                    } else {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val updated = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }
                                        dateMillis = updated.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${strings.dateLabel}: ${formatDate(dateMillis)}")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = creditText,
                                onValueChange = { creditText = it },
                                label = { Text(strings.creditAmount) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("credit_input")
                            )
                            OutlinedTextField(
                                value = debitText,
                                onValueChange = { debitText = it },
                                label = { Text(strings.debitAmount) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("debit_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(strings.descriptionNote) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${strings.previousBalance}: ${formatMoney(latestBalance, currency)}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${strings.currentBalance}: ${formatMoney(projectedBalance, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = VividPurple
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val targetId = selectedMemberId ?: return@Button
                                onRecordTransaction(targetId, dateMillis, creditVal, debitVal, description)
                                creditText = ""
                                debitText = ""
                                description = ""
                            },
                            enabled = selectedMemberId != null && (creditVal > 0.0 || debitVal > 0.0),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_credit_debit_button")
                        ) {
                            Text(strings.saveTransaction, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = strings.monthlyTransactions,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (memberTxs.isEmpty()) {
            item { EmptyStateView(strings.noRecordsFound, Icons.Default.AccountBalanceWallet) }
        } else {
            items(memberTxs, key = { it.id }) { tx ->
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.memberNameSnapshot, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${formatDate(tx.dateMillis)}${if (tx.description.isNotBlank()) " • ${tx.description}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (tx.creditAmount > 0) {
                                Text(
                                    "+${formatMoney(tx.creditAmount, currency)}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                            if (tx.debitAmount > 0) {
                                Text(
                                    "-${formatMoney(tx.debitAmount, currency)}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.previousBalance}: ${formatMoney(tx.previousBalance, currency)} → ${strings.currentBalance}: ${formatMoney(tx.currentBalance, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFamily
                        )
                        if (isAdmin) {
                            IconButton(onClick = { deleteTarget = tx }) {
                                Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = DangerRed)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    deleteTarget?.let { target ->
        ConfirmActionDialog(
            title = strings.confirmDeleteTitle,
            message = strings.confirmDeleteMessage,
            confirmText = strings.delete,
            onConfirm = {
                onDeleteTransaction(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
