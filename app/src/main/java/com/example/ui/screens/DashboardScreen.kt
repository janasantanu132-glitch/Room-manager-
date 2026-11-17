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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipWithItems
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomRentRecordEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.i18n.AppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.BrandGradientHeader
import com.example.ui.components.MetricCard
import com.example.ui.components.MonthYearPickerRow
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatMoney
import com.example.ui.theme.BrightBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VividPurple
import com.example.ui.theme.WarningAmber
import java.util.Calendar

@Composable
fun DashboardScreen(
    currentUser: UserEntity,
    language: AppLanguage,
    currency: String,
    selectedMonth: Int,
    selectedYear: Int,
    buildings: List<BuildingEntity>,
    rooms: List<RoomEntity>,
    members: List<UserEntity>,
    electricityBills: List<ElectricityBillEntity>,
    rentRecords: List<RoomRentRecordEntity>,
    marketingSlips: List<MarketingSlipWithItems>,
    creditDebits: List<CreditDebitTransactionEntity>,
    onMonthYearChange: (Int, Int) -> Unit,
    onNavigateTab: (String) -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isAdmin = currentUser.role == UserRole.ADMIN

    val activeMembersCount = members.count { !it.isBlocked }
    val blockedMembersCount = members.count { it.isBlocked }

    val monthRents = rentRecords.filter { it.billingMonth == selectedMonth && it.billingYear == selectedYear }
    val monthlyRentTotal = if (monthRents.isNotEmpty()) {
        monthRents.sumOf { it.individualShare }
    } else {
        rooms.sumOf { it.monthlyRent }
    }

    val monthlyElectricity = electricityBills
        .filter { it.billingMonth == selectedMonth && it.billingYear == selectedYear }
        .sumOf { it.totalBill }

    val monthlyMarketing = marketingSlips
        .filter { it.slip.billingMonth == selectedMonth && it.slip.billingYear == selectedYear }
        .sumOf { it.slip.totalAmount }

    val scopedCreditDebits = if (isAdmin) {
        creditDebits
    } else {
        creditDebits.filter { it.memberId == currentUser.id }
    }
    val totalCredit = scopedCreditDebits.sumOf { it.creditAmount }
    val totalDebit = scopedCreditDebits.sumOf { it.debitAmount }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val availableYears = ((currentYear - 10)..(currentYear + 1)).reversed().toList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BrandGradientHeader(
                title = "${strings.welcomeBack} ${currentUser.fullName}",
                subtitle = strings.dashboardOverview,
                trailing = {
                    StatusBadge(
                        text = if (isAdmin) strings.roleAdmin else strings.roleMember,
                        containerColor = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White
                    )
                }
            )
        }

        item {
            MonthYearPickerRow(
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                allowAll = false,
                availableYears = availableYears,
                language = language,
                onMonthSelected = { m -> if (m != null) onMonthYearChange(m, selectedYear) },
                onYearSelected = { y -> if (y != null) onMonthYearChange(selectedMonth, y) }
            )
        }

        // Row 1: Buildings & Rooms
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = strings.totalBuildings,
                    value = buildings.size.toString(),
                    icon = Icons.Default.Apartment,
                    accentColor = RoyalBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = strings.totalRooms,
                    value = rooms.size.toString(),
                    icon = Icons.Default.MeetingRoom,
                    accentColor = VividPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Total Members, Active Members, Blocked Members
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    label = strings.totalMembers,
                    value = members.size.toString(),
                    icon = Icons.Default.Group,
                    accentColor = BrightBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = strings.activeMembers,
                    value = activeMembersCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = strings.blockedMembers,
                    value = blockedMembersCount.toString(),
                    icon = Icons.Default.Block,
                    accentColor = DangerRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: Monthly Rent & Electricity Bill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = strings.totalMonthlyRent,
                    value = formatMoney(monthlyRentTotal, currency),
                    icon = Icons.Default.Payments,
                    accentColor = RoyalBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = strings.electricityBill,
                    value = formatMoney(monthlyElectricity, currency),
                    icon = Icons.Default.Bolt,
                    accentColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 4: Marketing Expenses, Total Credit, Total Debit
        item {
            MetricCard(
                label = strings.marketingExpenses,
                value = formatMoney(monthlyMarketing, currency),
                icon = Icons.Default.ShoppingCart,
                accentColor = DeepPurple,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = strings.totalCredit,
                    value = formatMoney(totalCredit, currency),
                    icon = Icons.Default.TrendingUp,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = strings.totalDebit,
                    value = formatMoney(totalDebit, currency),
                    icon = Icons.Default.TrendingDown,
                    accentColor = DangerRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = strings.electricityTitle,
                        icon = Icons.Default.Bolt,
                        tint = WarningAmber,
                        onClick = { onNavigateTab("billing_electricity") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_electricity")
                    )
                    QuickActionTile(
                        title = strings.roomRentTitle,
                        icon = Icons.Default.Payments,
                        tint = RoyalBlue,
                        onClick = { onNavigateTab("billing_rent") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_rent")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = strings.marketingSlipTitle,
                        icon = Icons.Default.ReceiptLong,
                        tint = VividPurple,
                        onClick = { onNavigateTab("billing_marketing") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_marketing")
                    )
                    QuickActionTile(
                        title = strings.creditDebitTitle,
                        icon = Icons.Default.AccountBalanceWallet,
                        tint = SuccessGreen,
                        onClick = { onNavigateTab("billing_credit") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_credit")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = strings.permanentHistoryTitle,
                        icon = Icons.Default.History,
                        tint = DeepPurple,
                        onClick = { onNavigateTab("history") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_history")
                    )
                    QuickActionTile(
                        title = strings.backupAndRestore,
                        icon = Icons.Default.Backup,
                        tint = BrightBlue,
                        onClick = onOpenBackup,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_backup")
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
