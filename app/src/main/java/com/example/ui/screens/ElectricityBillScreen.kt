package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.i18n.AppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthYearPickerRow
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatMoney
import com.example.ui.components.formatNumber
import com.example.ui.components.monthLabel
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VividPurple
import java.util.Calendar
import kotlin.math.max

@Composable
fun ElectricityBillScreen(
    currentUser: UserEntity,
    language: AppLanguage,
    currency: String,
    buildings: List<BuildingEntity>,
    rooms: List<RoomEntity>,
    members: List<UserEntity>,
    bills: List<ElectricityBillEntity>,
    onFetchLatestReading: suspend (Long) -> Double,
    onSaveBill: (Long, Int, Int, Double, Double, Double, Int?, String) -> Unit,
    onDeleteBill: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isAdmin = currentUser.role == UserRole.ADMIN
    val now = Calendar.getInstance()
    var month by remember { mutableStateOf(now.get(Calendar.MONTH) + 1) }
    var year by remember { mutableStateOf(now.get(Calendar.YEAR)) }

    var selectedRoomId by remember(rooms) { mutableStateOf(rooms.firstOrNull()?.id ?: 0L) }
    var roomExpanded by remember { mutableStateOf(false) }

    var prevReadingText by remember { mutableStateOf("0") }
    var currReadingText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var memberCountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ElectricityBillEntity?>(null) }

    val selectedRoom = rooms.firstOrNull { it.id == selectedRoomId }
    val roomMembers = remember(selectedRoomId, members) {
        members.filter { it.roomId == selectedRoomId && !it.isBlocked }
    }

    LaunchedEffect(selectedRoomId) {
        if (selectedRoomId > 0L) {
            val latest = onFetchLatestReading(selectedRoomId)
            prevReadingText = formatNumber(latest)
            memberCountText = max(1, roomMembers.size).toString()
        }
    }

    val prevReading = prevReadingText.toDoubleOrNull() ?: 0.0
    val currReading = currReadingText.toDoubleOrNull() ?: 0.0
    val ratePerUnit = rateText.toDoubleOrNull() ?: 0.0
    val effectiveMemberCount = (memberCountText.toIntOrNull() ?: max(1, roomMembers.size)).coerceAtLeast(1)

    val validReading = currReadingText.isNotBlank() && currReading >= prevReading
    val totalUnits = if (validReading) currReading - prevReading else 0.0
    val totalBill = totalUnits * ratePerUnit
    val perMemberShare = totalBill / effectiveMemberCount

    val visibleBills = remember(isAdmin, currentUser, bills) {
        if (isAdmin) bills else bills.filter { it.roomId == currentUser.roomId }
    }
    val years = ((now.get(Calendar.YEAR) - 10)..(now.get(Calendar.YEAR) + 1)).reversed().toList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isAdmin) {
            item {
                SectionCard {
                    Text(
                        text = strings.electricityTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (rooms.isEmpty()) {
                        Text(strings.noRoomsYet, color = MaterialTheme.colorScheme.error)
                    } else {
                        Box {
                            val bName = selectedRoom?.let { r ->
                                buildings.firstOrNull { it.id == r.buildingId }?.name ?: ""
                            } ?: ""
                            OutlinedButton(
                                onClick = { roomExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = selectedRoom?.let { "$bName • ${strings.roomNumber} ${it.roomNumber}" }
                                        ?: strings.selectRoom,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                                rooms.forEach { r ->
                                    val bTitle = buildings.firstOrNull { it.id == r.buildingId }?.name ?: ""
                                    DropdownMenuItem(
                                        text = { Text("$bTitle • ${strings.roomNumber} ${r.roomNumber}") },
                                        onClick = {
                                            selectedRoomId = r.id
                                            roomExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        MonthYearPickerRow(
                            selectedMonth = month,
                            selectedYear = year,
                            allowAll = false,
                            availableYears = years,
                            language = language,
                            onMonthSelected = { if (it != null) month = it },
                            onYearSelected = { if (it != null) year = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = prevReadingText,
                                onValueChange = { prevReadingText = it },
                                label = { Text(strings.previousReading) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = currReadingText,
                                onValueChange = { currReadingText = it },
                                label = { Text(strings.currentReading) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("elec_curr_reading")
                            )
                        }

                        if (currReadingText.isNotBlank() && currReading < prevReading) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.readingError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = rateText,
                                onValueChange = { rateText = it },
                                label = { Text(strings.ratePerUnit) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("elec_rate")
                            )
                            OutlinedTextField(
                                value = memberCountText,
                                onValueChange = { memberCountText = it },
                                label = { Text(strings.numberOfMembers) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
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
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.totalUnits, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${formatNumber(totalUnits)} kWh",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.totalBill, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        formatMoney(totalBill, currency),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${strings.memberWiseDivision} ($effectiveMemberCount)", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        formatMoney(perMemberShare, currency),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = VividPurple
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onSaveBill(
                                    selectedRoomId,
                                    month,
                                    year,
                                    prevReading,
                                    currReading,
                                    ratePerUnit,
                                    effectiveMemberCount,
                                    notes
                                )
                                currReadingText = ""
                                notes = ""
                            },
                            enabled = selectedRoomId > 0L && validReading && rateText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_electricity_button")
                        ) {
                            Text(strings.saveElectricityBill, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = strings.meterHistory,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (visibleBills.isEmpty()) {
            item { EmptyStateView(strings.noRecordsFound, Icons.Default.Bolt) }
        } else {
            items(visibleBills, key = { it.id }) { bill ->
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${bill.buildingNameSnapshot} • ${strings.roomNumber} ${bill.roomNumberSnapshot}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${monthLabel(bill.billingMonth, language)} ${bill.billingYear}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = formatMoney(bill.totalBill, currency),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${formatNumber(bill.previousReading)} → ${formatNumber(bill.currentReading)} (${formatNumber(bill.totalUnits)} u × ${formatMoney(bill.ratePerUnit, currency)})",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMonoFamily
                    )
                    Text(
                        text = "${strings.memberWiseDivision}: ${bill.memberBreakdownSnapshot}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isAdmin) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { deleteTarget = bill }) {
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
                onDeleteBill(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
