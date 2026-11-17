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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.i18n.LocalStrings
import com.example.ui.components.BrandGradientHeader
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatMoney
import com.example.ui.theme.DangerContainerLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VividPurple

@Composable
fun ManageHubScreen(
    currentUser: UserEntity,
    currency: String,
    buildings: List<BuildingEntity>,
    rooms: List<RoomEntity>,
    members: List<UserEntity>,
    onSaveBuilding: (Long, String, String, String) -> Unit,
    onDeleteBuilding: (Long) -> Unit,
    onSaveRoom: (Long, Long, String, String, Double, Double) -> Unit,
    onDeleteRoom: (Long) -> Unit,
    onCreateMember: (String, String, String, Long?, Long?) -> Unit,
    onUpdateMember: (Long, String, String, Long?, Long?) -> Unit,
    onChangeMemberPassword: (Long, String) -> Unit,
    onToggleBlockMember: (UserEntity) -> Unit,
    onDeleteMember: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isAdmin = currentUser.role == UserRole.ADMIN
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var showBuildingDialog by remember { mutableStateOf(false) }
    var editingBuilding by remember { mutableStateOf<BuildingEntity?>(null) }
    var deleteBuildingTarget by remember { mutableStateOf<BuildingEntity?>(null) }

    var showRoomDialog by remember { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<RoomEntity?>(null) }
    var deleteRoomTarget by remember { mutableStateOf<RoomEntity?>(null) }

    var showMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<UserEntity?>(null) }
    var passwordMemberTarget by remember { mutableStateOf<UserEntity?>(null) }
    var blockMemberTarget by remember { mutableStateOf<UserEntity?>(null) }
    var deleteMemberTarget by remember { mutableStateOf<UserEntity?>(null) }

    val visibleBuildings = remember(isAdmin, currentUser, buildings) {
        if (isAdmin) buildings else buildings.filter { it.id == currentUser.buildingId }
    }
    val visibleRooms = remember(isAdmin, currentUser, rooms) {
        if (isAdmin) rooms else rooms.filter { it.id == currentUser.roomId }
    }
    val visibleMembers = remember(isAdmin, currentUser, members) {
        if (isAdmin) members else members.filter { it.roomId != null && it.roomId == currentUser.roomId }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                BrandGradientHeader(
                    title = if (isAdmin) strings.adminControlPanel else strings.navManage,
                    subtitle = "${buildings.size} ${strings.buildings} • ${rooms.size} ${strings.rooms} • ${members.size} ${strings.members}"
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.buildings) },
                    modifier = Modifier.testTag("tab_buildings")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.rooms) },
                    modifier = Modifier.testTag("tab_rooms")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(strings.members) },
                    modifier = Modifier.testTag("tab_members")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        if (visibleBuildings.isEmpty()) {
                            item { EmptyStateView(strings.noBuildingsYet, Icons.Default.Apartment) }
                        } else {
                            items(visibleBuildings, key = { it.id }) { b ->
                                val roomCount = rooms.count { it.buildingId == b.id }
                                SectionCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = b.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (b.location.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = RoyalBlue,
                                                        modifier = Modifier.height(14.dp)
                                                    )
                                                    Text(
                                                        text = b.location,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (b.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = b.description,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        StatusBadge(
                                            text = "$roomCount ${strings.rooms}",
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    if (isAdmin) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(onClick = {
                                                editingBuilding = b
                                                showBuildingDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = RoyalBlue)
                                            }
                                            IconButton(onClick = { deleteBuildingTarget = b }) {
                                                Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = DangerRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (visibleRooms.isEmpty()) {
                            item { EmptyStateView(strings.noRoomsYet, Icons.Default.MeetingRoom) }
                        } else {
                            items(visibleRooms, key = { it.id }) { room ->
                                val buildingName = buildings.firstOrNull { it.id == room.buildingId }?.name ?: "-"
                                val roomMembers = members.filter { it.roomId == room.id }
                                SectionCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${strings.roomNumber} ${room.roomNumber}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "$buildingName${if (room.location.isNotBlank()) " • ${room.location}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusBadge(
                                            text = formatMoney(room.monthlyRent, currency),
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${strings.advancePayment}: ${formatMoney(room.advancePayment, currency)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${strings.assignedMembers}: ${
                                            if (roomMembers.isEmpty()) "-" else roomMembers.joinToString(", ") { it.fullName }
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (isAdmin) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(onClick = {
                                                editingRoom = room
                                                showRoomDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = RoyalBlue)
                                            }
                                            IconButton(onClick = { deleteRoomTarget = room }) {
                                                Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = DangerRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (visibleMembers.isEmpty()) {
                            item { EmptyStateView(strings.noMembersYet, Icons.Default.Group) }
                        } else {
                            items(visibleMembers, key = { it.id }) { m ->
                                val bName = buildings.firstOrNull { it.id == m.buildingId }?.name ?: "-"
                                val rName = rooms.firstOrNull { it.id == m.roomId }?.roomNumber ?: "-"
                                SectionCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = m.fullName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${strings.userIdLabel}: ${m.userId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$bName • ${strings.roomNumber} $rName",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        StatusBadge(
                                            text = if (m.isBlocked) strings.blockedStatus else strings.activeStatus,
                                            containerColor = if (m.isBlocked) DangerContainerLight else SuccessContainerLight,
                                            contentColor = if (m.isBlocked) DangerRed else SuccessGreen
                                        )
                                    }

                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    if (m.isBlocked) {
                                                        onToggleBlockMember(m)
                                                    } else {
                                                        blockMemberTarget = m
                                                    }
                                                },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = if (m.isBlocked) SuccessContainerLight else DangerContainerLight,
                                                    contentColor = if (m.isBlocked) SuccessGreen else DangerRed
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("block_toggle_${m.userId}")
                                            ) {
                                                Icon(
                                                    imageVector = if (m.isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                                                    contentDescription = null
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (m.isBlocked) strings.unblockMember else strings.blockMember)
                                            }

                                            OutlinedButton(
                                                onClick = { passwordMemberTarget = m },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Key, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(strings.changeMemberPassword)
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            IconButton(onClick = {
                                                editingMember = m
                                                showMemberDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = RoyalBlue)
                                            }
                                            IconButton(onClick = { deleteMemberTarget = m }) {
                                                Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = DangerRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            editingBuilding = null
                            showBuildingDialog = true
                        }
                        1 -> {
                            editingRoom = null
                            showRoomDialog = true
                        }
                        else -> {
                            editingMember = null
                            showMemberDialog = true
                        }
                    }
                },
                containerColor = VividPurple,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("manage_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }

    if (showBuildingDialog) {
        BuildingEditorDialog(
            initial = editingBuilding,
            onDismiss = { showBuildingDialog = false },
            onSave = { id, name, loc, desc ->
                onSaveBuilding(id, name, loc, desc)
                showBuildingDialog = false
            }
        )
    }

    if (showRoomDialog) {
        RoomEditorDialog(
            initial = editingRoom,
            buildings = buildings,
            onDismiss = { showRoomDialog = false },
            onSave = { id, bId, rNo, loc, rent, adv ->
                onSaveRoom(id, bId, rNo, loc, rent, adv)
                showRoomDialog = false
            }
        )
    }

    if (showMemberDialog) {
        MemberEditorDialog(
            initial = editingMember,
            buildings = buildings,
            rooms = rooms,
            onDismiss = { showMemberDialog = false },
            onSaveCreate = { fullName, uid, pass, bId, rId ->
                onCreateMember(fullName, uid, pass, bId, rId)
                showMemberDialog = false
            },
            onSaveEdit = { mId, fullName, uid, bId, rId ->
                onUpdateMember(mId, fullName, uid, bId, rId)
                showMemberDialog = false
            }
        )
    }

    passwordMemberTarget?.let { member ->
        ChangeMemberPasswordDialog(
            member = member,
            onDismiss = { passwordMemberTarget = null },
            onConfirm = { newPass ->
                onChangeMemberPassword(member.id, newPass)
                passwordMemberTarget = null
            }
        )
    }

    blockMemberTarget?.let { member ->
        ConfirmActionDialog(
            title = "${strings.blockMember}: ${member.fullName}",
            message = strings.confirmBlockMessage,
            confirmText = strings.blockMember,
            onConfirm = {
                onToggleBlockMember(member)
                blockMemberTarget = null
            },
            onDismiss = { blockMemberTarget = null }
        )
    }

    deleteBuildingTarget?.let { b ->
        ConfirmActionDialog(
            title = strings.confirmDeleteTitle,
            message = "${strings.confirmDeleteMessage}\n(${b.name})",
            confirmText = strings.delete,
            onConfirm = {
                onDeleteBuilding(b.id)
                deleteBuildingTarget = null
            },
            onDismiss = { deleteBuildingTarget = null }
        )
    }

    deleteRoomTarget?.let { r ->
        ConfirmActionDialog(
            title = strings.confirmDeleteTitle,
            message = "${strings.confirmDeleteMessage}\n(Room ${r.roomNumber})",
            confirmText = strings.delete,
            onConfirm = {
                onDeleteRoom(r.id)
                deleteRoomTarget = null
            },
            onDismiss = { deleteRoomTarget = null }
        )
    }

    deleteMemberTarget?.let { m ->
        ConfirmActionDialog(
            title = strings.confirmDeleteTitle,
            message = "${strings.confirmDeleteMessage}\n(${m.fullName})",
            confirmText = strings.delete,
            onConfirm = {
                onDeleteMember(m.id)
                deleteMemberTarget = null
            },
            onDismiss = { deleteMemberTarget = null }
        )
    }
}
