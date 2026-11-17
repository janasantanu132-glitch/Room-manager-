package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.UserEntity
import com.example.i18n.LocalStrings

@Composable
fun BuildingEditorDialog(
    initial: BuildingEntity?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, location: String, description: String) -> Unit
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) strings.addBuilding else strings.editBuilding,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.buildingName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(strings.buildingLocation) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.buildingDescription) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initial?.id ?: 0L, name, location, description) },
                enabled = name.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun RoomEditorDialog(
    initial: RoomEntity?,
    buildings: List<BuildingEntity>,
    onDismiss: () -> Unit,
    onSave: (id: Long, buildingId: Long, roomNumber: String, location: String, monthlyRent: Double, advance: Double) -> Unit
) {
    val strings = LocalStrings.current
    var selectedBuildingId by remember { mutableStateOf(initial?.buildingId ?: buildings.firstOrNull()?.id ?: 0L) }
    var buildingDropdown by remember { mutableStateOf(false) }
    var roomNumber by remember { mutableStateOf(initial?.roomNumber ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var rentText by remember { mutableStateOf(initial?.monthlyRent?.toString() ?: "") }
    var advanceText by remember { mutableStateOf(initial?.advancePayment?.toString() ?: "0") }

    val selectedBuildingName = buildings.firstOrNull { it.id == selectedBuildingId }?.name ?: strings.selectBuilding

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) strings.addRoom else strings.editRoom,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { buildingDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(selectedBuildingName, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = buildingDropdown, onDismissRequest = { buildingDropdown = false }) {
                        buildings.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name) },
                                onClick = {
                                    selectedBuildingId = b.id
                                    buildingDropdown = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text(strings.roomNumber) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(strings.roomLocation) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it },
                    label = { Text(strings.monthlyRent) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = advanceText,
                    onValueChange = { advanceText = it },
                    label = { Text(strings.advancePayment) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rent = rentText.toDoubleOrNull() ?: 0.0
                    val adv = advanceText.toDoubleOrNull() ?: 0.0
                    onSave(initial?.id ?: 0L, selectedBuildingId, roomNumber, location, rent, adv)
                },
                enabled = selectedBuildingId > 0L && roomNumber.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun MemberEditorDialog(
    initial: UserEntity?,
    buildings: List<BuildingEntity>,
    rooms: List<RoomEntity>,
    onDismiss: () -> Unit,
    onSaveCreate: (fullName: String, userId: String, password: String, buildingId: Long?, roomId: Long?) -> Unit,
    onSaveEdit: (memberId: Long, fullName: String, userId: String, buildingId: Long?, roomId: Long?) -> Unit
) {
    val strings = LocalStrings.current
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var userId by remember { mutableStateOf(initial?.userId ?: "") }
    var password by remember { mutableStateOf("") }
    var selectedBuildingId by remember { mutableStateOf(initial?.buildingId) }
    var selectedRoomId by remember { mutableStateOf(initial?.roomId) }

    var buildingExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }

    val filteredRooms = remember(selectedBuildingId, rooms) {
        if (selectedBuildingId == null) rooms else rooms.filter { it.buildingId == selectedBuildingId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) strings.addMember else strings.editMember,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(strings.fullName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text(strings.userIdLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (initial == null) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(strings.passwordLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box {
                    val bName = buildings.firstOrNull { it.id == selectedBuildingId }?.name ?: strings.assignBuilding
                    OutlinedButton(
                        onClick = { buildingExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(bName, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = buildingExpanded, onDismissRequest = { buildingExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("-") },
                            onClick = {
                                selectedBuildingId = null
                                selectedRoomId = null
                                buildingExpanded = false
                            }
                        )
                        buildings.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name) },
                                onClick = {
                                    selectedBuildingId = b.id
                                    if (rooms.none { it.id == selectedRoomId && it.buildingId == b.id }) {
                                        selectedRoomId = null
                                    }
                                    buildingExpanded = false
                                }
                            )
                        }
                    }
                }

                Box {
                    val rName = filteredRooms.firstOrNull { it.id == selectedRoomId }?.roomNumber?.let { "Room $it" }
                        ?: strings.assignRoom
                    OutlinedButton(
                        onClick = { roomExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(rName, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("-") },
                            onClick = {
                                selectedRoomId = null
                                roomExpanded = false
                            }
                        )
                        filteredRooms.forEach { r ->
                            val bTitle = buildings.firstOrNull { it.id == r.buildingId }?.name ?: ""
                            DropdownMenuItem(
                                text = { Text("$bTitle • Room ${r.roomNumber}") },
                                onClick = {
                                    selectedRoomId = r.id
                                    selectedBuildingId = r.buildingId
                                    roomExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val valid = fullName.isNotBlank() && userId.isNotBlank() && (initial != null || password.length >= 4)
            Button(
                onClick = {
                    if (initial == null) {
                        onSaveCreate(fullName, userId, password, selectedBuildingId, selectedRoomId)
                    } else {
                        onSaveEdit(initial.id, fullName, userId, selectedBuildingId, selectedRoomId)
                    }
                },
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

@Composable
fun ChangeMemberPasswordDialog(
    member: UserEntity,
    onDismiss: () -> Unit,
    onConfirm: (newPassword: String) -> Unit
) {
    val strings = LocalStrings.current
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.changeMemberPassword}: ${member.fullName}", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text(strings.newPassword) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newPassword) },
                enabled = newPassword.length >= 4
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
