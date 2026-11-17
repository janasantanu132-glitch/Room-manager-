package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BuildingEntity
import com.example.data.local.entity.CreditDebitTransactionEntity
import com.example.data.local.entity.ElectricityBillEntity
import com.example.data.local.entity.MarketingSlipWithItems
import com.example.data.local.entity.RoomEntity
import com.example.data.local.entity.RoomRentRecordEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.MarketingItemInput
import com.example.i18n.AppLanguage
import com.example.i18n.LocalStrings

@Composable
fun BillingHubScreen(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    currentUser: UserEntity,
    language: AppLanguage,
    currency: String,
    buildings: List<BuildingEntity>,
    rooms: List<RoomEntity>,
    members: List<UserEntity>,
    electricityBills: List<ElectricityBillEntity>,
    rentRecords: List<RoomRentRecordEntity>,
    marketingSlips: List<MarketingSlipWithItems>,
    creditDebits: List<CreditDebitTransactionEntity>,
    onFetchLatestReading: suspend (Long) -> Double,
    onSaveElectricityBill: (Long, Int, Int, Double, Double, Double, Int?, String) -> Unit,
    onDeleteElectricityBill: (Long) -> Unit,
    onGenerateRoomRent: (Long, Int, Int, Double?, Double, Double, Boolean) -> Unit,
    onToggleRentPaid: (RoomRentRecordEntity) -> Unit,
    onDeleteRentRecord: (Long) -> Unit,
    onRequestNextSlipNumber: suspend (Long) -> String,
    onSaveMarketingSlip: (String, Long, Long?, String, String, List<MarketingItemInput>, () -> Unit) -> Unit,
    onDeleteMarketingSlip: (Long) -> Unit,
    onShareMarketingSlipPdf: (MarketingSlipWithItems) -> Unit,
    onRecordCreditDebit: (Long, Long, Double, Double, String) -> Unit,
    onDeleteCreditDebit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabChange(0) },
                text = { Text(strings.electricityTitle) },
                modifier = Modifier.testTag("billing_tab_electricity")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabChange(1) },
                text = { Text(strings.roomRentTitle) },
                modifier = Modifier.testTag("billing_tab_rent")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { onTabChange(2) },
                text = { Text(strings.marketingSlipTitle) },
                modifier = Modifier.testTag("billing_tab_marketing")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { onTabChange(3) },
                text = { Text(strings.creditDebitTitle) },
                modifier = Modifier.testTag("billing_tab_credit")
            )
        }

        when (selectedTab) {
            0 -> ElectricityBillScreen(
                currentUser = currentUser,
                language = language,
                currency = currency,
                buildings = buildings,
                rooms = rooms,
                members = members,
                bills = electricityBills,
                onFetchLatestReading = onFetchLatestReading,
                onSaveBill = onSaveElectricityBill,
                onDeleteBill = onDeleteElectricityBill
            )
            1 -> RoomRentScreen(
                currentUser = currentUser,
                language = language,
                currency = currency,
                buildings = buildings,
                rooms = rooms,
                members = members,
                rentRecords = rentRecords,
                onGenerateRoomRent = onGenerateRoomRent,
                onToggleRentPaid = onToggleRentPaid,
                onDeleteRentRecord = onDeleteRentRecord
            )
            2 -> MarketingSlipScreen(
                currentUser = currentUser,
                currency = currency,
                members = members,
                slips = marketingSlips,
                onRequestNextSlipNumber = onRequestNextSlipNumber,
                onSaveSlip = onSaveMarketingSlip,
                onDeleteSlip = onDeleteMarketingSlip,
                onSharePdf = onShareMarketingSlipPdf
            )
            else -> CreditDebitScreen(
                currentUser = currentUser,
                currency = currency,
                members = members,
                transactions = creditDebits,
                onRecordTransaction = onRecordCreditDebit,
                onDeleteTransaction = onDeleteCreditDebit
            )
        }
    }
}
