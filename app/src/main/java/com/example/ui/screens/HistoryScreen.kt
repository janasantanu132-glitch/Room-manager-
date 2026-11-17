package com.example.ui.screens

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipWithItems
import com.example.data.local.entity.RoomRentRecordEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.i18n.AppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.BrandGradientHeader
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthYearPickerRow
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatDate
import com.example.ui.components.formatMoney
import com.example.ui.components.formatNumber
import com.example.ui.components.monthLabel
import com.example.ui.theme.DangerContainerLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VividPurple
import java.util.Calendar

@Composable
fun HistoryScreen(
    currentUser: UserEntity,
    language: AppLanguage,
    currency: String,
    electricityBills: List<ElectricityBillEntity>,
    rentRecords: List<RoomRentRecordEntity>,
    marketingSlips: List<MarketingSlipWithItems>,
    creditDebits: List<CreditDebitTransactionEntity>,
    onShareSlipPdf: (MarketingSlipWithItems) -> Unit,
    onExportFilteredPdf: (String, List<ElectricityBillEntity>, List<RoomRentRecordEntity>, List<MarketingSlipWithItems>, List<CreditDebitTransactionEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isAdmin = currentUser.role == UserRole.ADMIN

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    var filterYear by rememberSaveable { mutableStateOf<Int?>(null) }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val availableYears = remember(electricityBills, rentRecords, marketingSlips, creditDebits) {
        val yearsInDb = buildSet {
            electricityBills.forEach { add(it.billingYear) }
            rentRecords.forEach { add(it.billingYear) }
            marketingSlips.forEach { add(it.slip.billingYear) }
            creditDebits.forEach { add(it.billingYear) }
            for (y in (currentYear - 10)..(currentYear + 1)) add(y)
        }
        yearsInDb.sortedDescending()
    }

    val q = searchQuery.trim().lowercase()

    val filteredElec = remember(electricityBills, filterMonth, filterYear, q, isAdmin, currentUser) {
        electricityBills.filter { b ->
            (isAdmin || b.roomId == currentUser.roomId) &&
                (filterMonth == null || b.billingMonth == filterMonth) &&
                (filterYear == null || b.billingYear == filterYear) &&
                (q.isEmpty() || b.buildingNameSnapshot.lowercase().contains(q) ||
                    b.roomNumberSnapshot.lowercase().contains(q) ||
                    b.memberBreakdownSnapshot.lowercase().contains(q))
        }
    }

    val filteredRent = remember(rentRecords, filterMonth, filterYear, q, isAdmin, currentUser) {
        rentRecords.filter { r ->
            (isAdmin || r.memberId == currentUser.id || r.roomId == currentUser.roomId) &&
                (filterMonth == null || r.billingMonth == filterMonth) &&
                (filterYear == null || r.billingYear == filterYear) &&
                (q.isEmpty() || r.memberNameSnapshot.lowercase().contains(q) ||
                    r.buildingNameSnapshot.lowercase().contains(q) ||
                    r.roomNumberSnapshot.lowercase().contains(q))
        }
    }

    val filteredSlips = remember(marketingSlips, filterMonth, filterYear, q) {
        marketingSlips.filter { sw ->
            (filterMonth == null || sw.slip.billingMonth == filterMonth) &&
                (filterYear == null || sw.slip.billingYear == filterYear) &&
                (q.isEmpty() || sw.slip.slipNumber.lowercase().contains(q) ||
                    sw.slip.paymentByName.lowercase().contains(q) ||
                    sw.items.any { it.productName.lowercase().contains(q) })
        }
    }

    val filteredCredits = remember(creditDebits, filterMonth, filterYear, q, isAdmin, currentUser) {
        creditDebits.filter { tx ->
            (isAdmin || tx.memberId == currentUser.id) &&
                (filterMonth == null || tx.billingMonth == filterMonth) &&
                (filterYear == null || tx.billingYear == filterYear) &&
                (q.isEmpty() || tx.memberNameSnapshot.lowercase().contains(q) ||
                    tx.description.lowercase().contains(q))
        }
    }

    val monthlyReportKeys = remember(filteredElec, filteredRent, filteredSlips, filteredCredits) {
        val keys = linkedSetOf<Pair<Int, Int>>()
        filteredRent.forEach { keys.add(it.billingYear to it.billingMonth) }
        filteredElec.forEach { keys.add(it.billingYear to it.billingMonth) }
        filteredSlips.forEach { keys.add(it.slip.billingYear to it.slip.billingMonth) }
        filteredCredits.forEach { keys.add(it.billingYear to it.billingMonth) }
        keys.sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrandGradientHeader(
                title = strings.permanentHistoryTitle,
                subtitle = strings.backupPermanentNote,
                trailing = {
                    FilledTonalButton(
                        onClick = {
                            val label = buildString {
                                append("History Report")
                                if (filterMonth != null) append(" ${monthLabel(filterMonth!!, language)}")
                                if (filterYear != null) append(" $filterYear")
                            }
                            onExportFilteredPdf(label, filteredElec, filteredRent, filteredSlips, filteredCredits)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = strings.generatePdf)
                    }
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )

            MonthYearPickerRow(
                selectedMonth = filterMonth,
                selectedYear = filterYear,
                allowAll = true,
                availableYears = availableYears,
                language = language,
                onMonthSelected = { filterMonth = it },
                onYearSelected = { filterYear = it }
            )
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.monthlyReportsTab) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.electricityTitle) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.roomRentTitle) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(strings.marketingSlipTitle) })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text(strings.creditDebitTitle) })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    if (monthlyReportKeys.isEmpty()) {
                        item { EmptyStateView(strings.noRecordsFound, Icons.Default.History) }
                    } else {
                        items(monthlyReportKeys, key = { "${it.first}-${it.second}" }) { (y, m) ->
                            val mRent = filteredRent.filter { it.billingYear == y && it.billingMonth == m }
                            val mElec = filteredElec.filter { it.billingYear == y && it.billingMonth == m }
                            val mSlip = filteredSlips.filter { it.slip.billingYear == y && it.slip.billingMonth == m }
                            val mCred = filteredCredits.filter { it.billingYear == y && it.billingMonth == m }

                            SectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${monthLabel(m, language)} $y",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            onExportFilteredPdf(
                                                "${monthLabel(m, language)} $y Report",
                                                mElec, mRent, mSlip, mCred
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PDF")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                ReportRow(strings.totalMonthlyRent, formatMoney(mRent.sumOf { it.individualShare }, currency))
                                ReportRow(strings.dueAmount, formatMoney(mRent.sumOf { it.dueAmount }, currency))
                                ReportRow(strings.electricityBill, formatMoney(mElec.sumOf { it.totalBill }, currency))
                                ReportRow(strings.marketingExpenses, formatMoney(mSlip.sumOf { it.slip.totalAmount }, currency))
                                ReportRow(strings.totalCredit, "+${formatMoney(mCred.sumOf { it.creditAmount }, currency)}")
                                ReportRow(strings.totalDebit, "-${formatMoney(mCred.sumOf { it.debitAmount }, currency)}")
                            }
                        }
                    }
                }

                1 -> {
                    if (filteredElec.isEmpty()) {
                        item { EmptyStateView(strings.noRecordsFound, Icons.Default.History) }
                    } else {
                        items(filteredElec, key = { it.id }) { b ->
                            SectionCard {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "${b.buildingNameSnapshot} • ${strings.roomNumber} ${b.roomNumberSnapshot}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        formatMoney(b.totalBill, currency),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = VividPurple
                                    )
                                }
                                Text(
                                    "${monthLabel(b.billingMonth, language)} ${b.billingYear} | ${formatNumber(b.previousReading)} → ${formatNumber(b.currentReading)} (${formatNumber(b.totalUnits)} u)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                Text(b.memberBreakdownSnapshot, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                2 -> {
                    if (filteredRent.isEmpty()) {
                        item { EmptyStateView(strings.noRecordsFound, Icons.Default.History) }
                    } else {
                        items(filteredRent, key = { it.id }) { r ->
                            SectionCard {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(r.memberNameSnapshot, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${r.buildingNameSnapshot} • ${strings.roomNumber} ${r.roomNumberSnapshot} • ${monthLabel(r.billingMonth, language)} ${r.billingYear}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    StatusBadge(
                                        text = if (r.isPaid) strings.paidStatus else strings.unpaidStatus,
                                        containerColor = if (r.isPaid) SuccessContainerLight else DangerContainerLight,
                                        contentColor = if (r.isPaid) SuccessGreen else DangerRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${strings.individualRentShare}: ${formatMoney(r.individualShare, currency)} | ${strings.dueAmount}: ${formatMoney(r.dueAmount, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFamily
                                )
                            }
                        }
                    }
                }

                3 -> {
                    if (filteredSlips.isEmpty()) {
                        item { EmptyStateView(strings.noRecordsFound, Icons.Default.History) }
                    } else {
                        items(filteredSlips, key = { it.slip.id }) { sw ->
                            SectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(sw.slip.slipNumber, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
                                        Text("${formatDate(sw.slip.dateMillis)} • ${sw.slip.paymentByName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        formatMoney(sw.slip.totalAmount, currency),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = VividPurple
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                FilledTonalButton(onClick = { onShareSlipPdf(sw) }, shape = RoundedCornerShape(10.dp)) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.shareReceipt)
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (filteredCredits.isEmpty()) {
                        item { EmptyStateView(strings.noRecordsFound, Icons.Default.History) }
                    } else {
                        items(filteredCredits, key = { it.id }) { tx ->
                            SectionCard {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tx.memberNameSnapshot, fontWeight = FontWeight.Bold)
                                    Text(formatDate(tx.dateMillis), style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "Cr: +${formatMoney(tx.creditAmount, currency)} | Dr: -${formatMoney(tx.debitAmount, currency)} | Bal: ${formatMoney(tx.currentBalance, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                if (tx.description.isNotBlank()) {
                                    Text(tx.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
    }
}
