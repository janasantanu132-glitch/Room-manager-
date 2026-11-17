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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.local.entity.MarketingSlipWithItems
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.repository.MarketingItemInput
import com.example.i18n.LocalStrings
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatDate
import com.example.ui.components.formatMoney
import com.example.ui.components.formatNumber
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VividPurple
import java.util.Calendar

private data class DraftSlipItem(
    val key: Long,
    val productName: String = "",
    val quantityText: String = "1",
    val priceText: String = ""
) {
    val qty: Double get() = quantityText.toDoubleOrNull() ?: 0.0
    val unitPrice: Double get() = priceText.toDoubleOrNull() ?: 0.0
    val lineTotal: Double get() = qty * unitPrice
}

@Composable
fun MarketingSlipScreen(
    currentUser: UserEntity,
    currency: String,
    members: List<UserEntity>,
    slips: List<MarketingSlipWithItems>,
    onRequestNextSlipNumber: suspend (Long) -> String,
    onSaveSlip: (String, Long, Long?, String, String, List<MarketingItemInput>, () -> Unit) -> Unit,
    onDeleteSlip: (Long) -> Unit,
    onSharePdf: (MarketingSlipWithItems) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val isAdmin = currentUser.role == UserRole.ADMIN

    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var slipNumber by remember { mutableStateOf("") }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var paymentByName by remember { mutableStateOf("Admin") }
    var notes by remember { mutableStateOf("") }
    var memberDropdown by remember { mutableStateOf(false) }

    var nextKey by remember { mutableLongStateOf(1L) }
    val draftItems = remember { mutableStateListOf(DraftSlipItem(key = 0L)) }
    var deleteTarget by remember { mutableStateOf<MarketingSlipWithItems?>(null) }

    LaunchedEffect(dateMillis, slips.size) {
        slipNumber = onRequestNextSlipNumber(dateMillis)
    }

    val grandTotal = draftItems.sumOf { it.lineTotal }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isAdmin) {
            item {
                SectionCard {
                    Text(
                        text = strings.marketingSlipTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = slipNumber,
                            onValueChange = { slipNumber = it },
                            label = { Text(strings.slipNumber) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val updated = Calendar.getInstance().apply {
                                            set(y, m, d, 12, 0, 0)
                                        }
                                        dateMillis = updated.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = strings.dateLabel)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(formatDate(dateMillis))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { memberDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    members.firstOrNull { it.id == selectedMemberId }?.fullName ?: strings.paymentBy,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = memberDropdown, onDismissRequest = { memberDropdown = false }) {
                                DropdownMenuItem(
                                    text = { Text("Custom / Admin") },
                                    onClick = {
                                        selectedMemberId = null
                                        memberDropdown = false
                                    }
                                )
                                members.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.fullName) },
                                        onClick = {
                                            selectedMemberId = m.id
                                            paymentByName = m.fullName
                                            memberDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedMemberId == null) {
                            OutlinedTextField(
                                value = paymentByName,
                                onValueChange = { paymentByName = it },
                                label = { Text(strings.paymentBy) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    draftItems.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.productName,
                                    onValueChange = { text -> draftItems[index] = item.copy(productName = text) },
                                    label = { Text("${strings.productName} #${index + 1}") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("slip_product_$index")
                                )
                                if (draftItems.size > 1) {
                                    IconButton(onClick = { draftItems.removeAt(index) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = strings.delete, tint = DangerRed)
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.quantityText,
                                    onValueChange = { q -> draftItems[index] = item.copy(quantityText = q) },
                                    label = { Text(strings.quantity) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.priceText,
                                    onValueChange = { p -> draftItems[index] = item.copy(priceText = p) },
                                    label = { Text(strings.price) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("slip_price_$index")
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = formatMoney(item.lineTotal, currency),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            draftItems.add(DraftSlipItem(key = nextKey++))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_slip_item_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.addItem)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(strings.notes) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.totalAmount, fontWeight = FontWeight.Bold)
                            Text(
                                text = formatMoney(grandTotal, currency),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = VividPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val inputs = draftItems.map {
                                MarketingItemInput(it.productName, it.qty, it.unitPrice)
                            }
                            onSaveSlip(slipNumber, dateMillis, selectedMemberId, paymentByName, notes, inputs) {
                                draftItems.clear()
                                draftItems.add(DraftSlipItem(key = nextKey++))
                                notes = ""
                            }
                        },
                        enabled = slipNumber.isNotBlank() && draftItems.any { it.productName.isNotBlank() && it.qty > 0.0 },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_slip_button")
                    ) {
                        Text(strings.saveSlip, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = strings.savedSlips,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (slips.isEmpty()) {
            item { EmptyStateView(strings.noRecordsFound, Icons.Default.ReceiptLong) }
        } else {
            items(slips, key = { it.slip.id }) { sw ->
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sw.slip.slipNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatDate(sw.slip.dateMillis)} • ${strings.paymentBy}: ${sw.slip.paymentByName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = formatMoney(sw.slip.totalAmount, currency),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    sw.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${item.productName} (${formatNumber(item.quantity)} × ${formatMoney(item.unitPrice, currency)})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatMoney(item.lineTotal, currency),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onSharePdf(sw) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("pdf_slip_${sw.slip.slipNumber}")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${strings.generatePdf} / ${strings.shareReceipt}")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (isAdmin) {
                            IconButton(onClick = { deleteTarget = sw }) {
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
            message = "${strings.confirmDeleteMessage}\n(${target.slip.slipNumber})",
            confirmText = strings.delete,
            onConfirm = {
                onDeleteSlip(target.slip.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
