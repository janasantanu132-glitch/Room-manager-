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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomRentRecordEntity
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
import com.example.ui.components.monthLabel
import com.example.ui.theme.DangerContainerLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VividPurple
import java.util.Calendar
import kotlin.math.max

@Composable
fun RoomRentScreen(
    currentUser: UserEntity,
    language: AppLanguage,
    currency: String,
    buildings: List<BuildingEntity>,
    rooms: List<RoomEntity>,
    members: List<UserEntity>,
    rentRecords: List<RoomRentRecordEntity>,
    onGenerateRoomRent: (Long, Int, Int, Double?, Double, Double, Boolean) -> Unit,
    onToggleRentPaid: (RoomRentRecordEntity) -> Unit,
    onDeleteRentRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isAdmin = currentUser.role == UserRole.ADMIN
    val now = Calendar.getInstance()
    var month by remember { mutableStateOf(now.get(Calendar.MONTH) + 1) }
    var year by remember { mutableStateOf(now.get(Calendar.YEAR)) }

    var selectedRoomId by remember(rooms) { mutableStateOf(rooms.firstOrNull()?.id ?: 0L) }
    var roomExpanded by remember { mutableStateOf(false) }

    var rentOverrideText by remember { mutableStateOf("") }
    var advanceText by remember { mutableStateOf("0") }
    var paidPerMemberText by remember { mutableStateOf("0") }
    var markAllPaid by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<RoomRentRecordEntity?>(null) }

    val selectedRoom = rooms.firstOrNull { it.id == selectedRoomId }
    val roomMembers = remember(selectedRoomId, members) {
        members.filter { it.roomId == selectedRoomId && !it.isBlocked }
    }

    LaunchedEffect(selectedRoomId, selectedRoom) {
        if (selectedRoom != null) {
            rentOverrideText = selectedRoom.monthlyRent.toString()
            advanceText = "0"
        }
    }

    val effectiveRent = rentOverrideText.toDoubleOrNull() ?: selectedRoom?.monthlyRent ?: 0.0
    val memberCount = max(1, roomMembers.size)
    val individualShare = effectiveRent / memberCount
    val advanceApplied = advanceText.toDoubleOrNull() ?: 0.0
    val paidPerMember = paidPerMemberText.toDoubleOrNull() ?: 0.0
    val estimatedDuePerMember = if (markAllPaid) 0.0 else max(0.0, individualShare - (advanceApplied / memberCount) - paidPerMember)

    val visibleRecords = remember(isAdmin, currentUser, rentRecords) {
        if (isAdmin) rentRecords else rentRecords.filter { it.memberId == currentUser.id || it.roomId == currentUser.roomId }
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
                        text = strings.roomRentTitle,
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
                                value = rentOverrideText,
                                onValueChange = { rentOverrideText = it },
                                label = { Text(strings.monthlyRent) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = advanceText,
                                onValueChange = { advanceText = it },
                                label = { Text(strings.advancePayment) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = paidPerMemberText,
                            onValueChange = { paidPerMemberText = it },
                            label = { Text(strings.paidAmount) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !markAllPaid,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = markAllPaid, onCheckedChange = { markAllPaid = it })
                            Text(strings.markPaid, style = MaterialTheme.typography.bodyMedium)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.numberOfMembers)
                                    Text("${roomMembers.size} (calc: $memberCount)", fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.individualRentShare)
                                    Text(
                                        formatMoney(individualShare, currency),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.dueAmount)
                                    Text(
                                        formatMoney(estimatedDuePerMember, currency),
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
                                onGenerateRoomRent(
                                    selectedRoomId,
                                    month,
                                    year,
                                    effectiveRent,
                                    advanceApplied,
                                    paidPerMember,
                                    markAllPaid
                                )
                            },
                            enabled = selectedRoomId > 0L,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_rent_button")
                        ) {
                            Text(strings.generateRoomRent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = strings.rentHistory,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (visibleRecords.isEmpty()) {
            item { EmptyStateView(strings.noRecordsFound, Icons.Default.Payments) }
        } else {
            items(visibleRecords, key = { it.id }) { rec ->
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.memberNameSnapshot,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${rec.buildingNameSnapshot} • ${strings.roomNumber} ${rec.roomNumberSnapshot} • ${monthLabel(rec.billingMonth, language)} ${rec.billingYear}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = if (rec.isPaid) strings.paidStatus else strings.unpaidStatus,
                            containerColor = if (rec.isPaid) SuccessContainerLight else DangerContainerLight,
                            contentColor = if (rec.isPaid) SuccessGreen else DangerRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${strings.individualRentShare}: ${formatMoney(rec.individualShare, currency)} | ${strings.paidAmount}: ${formatMoney(rec.paidAmount, currency)} | ${strings.dueAmount}: ${formatMoney(rec.dueAmount, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMonoFamily
                    )

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onToggleRentPaid(rec) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (rec.isPaid) strings.markUnpaid else strings.markPaid)
                            }
                            IconButton(onClick = { deleteTarget = rec }) {
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
                onDeleteRentRecord(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
