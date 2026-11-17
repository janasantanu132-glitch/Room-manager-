package com.example.ui.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.i18n.AppLanguage
import com.example.i18n.BengaliStrings
import com.example.i18n.EnglishStrings
import com.example.i18n.LocalStrings
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.BillingHubScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ManageHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiEvent

object AppRoutes {
    const val DASHBOARD = "dashboard"
    const val MANAGE = "manage"
    const val BILLING = "billing"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun RoomManagementRoot(viewModel: MainViewModel) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val strings = if (language == AppLanguage.BENGALI) BengaliStrings else EnglishStrings

    CompositionLocalProvider(LocalStrings provides strings) {
        val authState by viewModel.authState.collectAsStateWithLifecycle()
        val loginError by viewModel.loginError.collectAsStateWithLifecycle()
        val isLoggingIn by viewModel.isLoggingIn.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
                    is UiEvent.ShareFile -> {
                        runCatching {
                            context.startActivity(Intent.createChooser(event.intent, strings.appName))
                        }.onFailure {
                            snackbarHostState.showSnackbar("No app found to open exported file")
                        }
                    }
                }
            }
        }

        when (val state = authState) {
            AuthState.Checking -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            AuthState.Unauthenticated -> {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { padding ->
                    LoginScreen(
                        language = language,
                        errorMessage = loginError,
                        isLoading = isLoggingIn,
                        onLanguageChange = viewModel::setLanguage,
                        onClearError = viewModel::clearLoginError,
                        onLogin = viewModel::login,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            is AuthState.Authenticated -> {
                AuthenticatedAppScaffold(
                    viewModel = viewModel,
                    currentUser = state.user,
                    language = language,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedAppScaffold(
    viewModel: MainViewModel,
    currentUser: com.example.data.local.entity.UserEntity,
    language: AppLanguage,
    snackbarHostState: SnackbarHostState
) {
    val strings = LocalStrings.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.DASHBOARD

    var billingTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val buildings by viewModel.buildings.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val electricityBills by viewModel.electricityBills.collectAsStateWithLifecycle()
    val rentRecords by viewModel.rentRecords.collectAsStateWithLifecycle()
    val marketingSlips by viewModel.marketingSlips.collectAsStateWithLifecycle()
    val creditDebits by viewModel.creditDebits.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()

    val tabs = listOf(
        NavTabItem(AppRoutes.DASHBOARD, strings.navDashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTabItem(AppRoutes.MANAGE, strings.navManage, Icons.Filled.Apartment, Icons.Outlined.Apartment),
        NavTabItem(AppRoutes.BILLING, strings.navBilling, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
        NavTabItem(AppRoutes.HISTORY, strings.navHistory, Icons.Filled.History, Icons.Outlined.History),
        NavTabItem(AppRoutes.SETTINGS, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoutes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail && currentRoute != AppRoutes.BACKUP) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && currentRoute != AppRoutes.BACKUP) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppRoutes.DASHBOARD) {
                        DashboardScreen(
                            currentUser = currentUser,
                            language = language,
                            currency = currency,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            buildings = buildings,
                            rooms = rooms,
                            members = members,
                            electricityBills = electricityBills,
                            rentRecords = rentRecords,
                            marketingSlips = marketingSlips,
                            creditDebits = creditDebits,
                            onMonthYearChange = viewModel::setDashboardMonthYear,
                            onNavigateTab = { target ->
                                when (target) {
                                    "billing_electricity" -> {
                                        billingTabIndex = 0
                                        navigateToTab(AppRoutes.BILLING)
                                    }
                                    "billing_rent" -> {
                                        billingTabIndex = 1
                                        navigateToTab(AppRoutes.BILLING)
                                    }
                                    "billing_marketing" -> {
                                        billingTabIndex = 2
                                        navigateToTab(AppRoutes.BILLING)
                                    }
                                    "billing_credit" -> {
                                        billingTabIndex = 3
                                        navigateToTab(AppRoutes.BILLING)
                                    }
                                    "history" -> navigateToTab(AppRoutes.HISTORY)
                                }
                            },
                            onOpenBackup = {
                                viewModel.refreshLocalBackups()
                                navController.navigate(AppRoutes.BACKUP)
                            }
                        )
                    }

                    composable(AppRoutes.MANAGE) {
                        ManageHubScreen(
                            currentUser = currentUser,
                            currency = currency,
                            buildings = buildings,
                            rooms = rooms,
                            members = members,
                            onSaveBuilding = viewModel::saveBuilding,
                            onDeleteBuilding = viewModel::deleteBuilding,
                            onSaveRoom = viewModel::saveRoom,
                            onDeleteRoom = viewModel::deleteRoom,
                            onCreateMember = viewModel::createMember,
                            onUpdateMember = viewModel::updateMember,
                            onChangeMemberPassword = viewModel::changeMemberPassword,
                            onToggleBlockMember = viewModel::toggleBlockMember,
                            onDeleteMember = viewModel::deleteMember
                        )
                    }

                    composable(AppRoutes.BILLING) {
                        BillingHubScreen(
                            selectedTab = billingTabIndex,
                            onTabChange = { billingTabIndex = it },
                            currentUser = currentUser,
                            language = language,
                            currency = currency,
                            buildings = buildings,
                            rooms = rooms,
                            members = members,
                            electricityBills = electricityBills,
                            rentRecords = rentRecords,
                            marketingSlips = marketingSlips,
                            creditDebits = creditDebits,
                            onFetchLatestReading = viewModel::getLatestMeterReading,
                            onSaveElectricityBill = viewModel::saveElectricityBill,
                            onDeleteElectricityBill = viewModel::deleteElectricityBill,
                            onGenerateRoomRent = viewModel::generateRoomRent,
                            onToggleRentPaid = viewModel::toggleRentPaid,
                            onDeleteRentRecord = viewModel::deleteRentRecord,
                            onRequestNextSlipNumber = viewModel::nextSlipNumber,
                            onSaveMarketingSlip = viewModel::saveMarketingSlip,
                            onDeleteMarketingSlip = viewModel::deleteMarketingSlip,
                            onShareMarketingSlipPdf = viewModel::exportAndShareMarketingSlipPdf,
                            onRecordCreditDebit = viewModel::recordCreditDebit,
                            onDeleteCreditDebit = viewModel::deleteCreditDebit
                        )
                    }

                    composable(AppRoutes.HISTORY) {
                        HistoryScreen(
                            currentUser = currentUser,
                            language = language,
                            currency = currency,
                            electricityBills = electricityBills,
                            rentRecords = rentRecords,
                            marketingSlips = marketingSlips,
                            creditDebits = creditDebits,
                            onShareSlipPdf = viewModel::exportAndShareMarketingSlipPdf,
                            onExportFilteredPdf = viewModel::exportFullReportPdf
                        )
                    }

                    composable(AppRoutes.SETTINGS) {
                        SettingsScreen(
                            currentUser = currentUser,
                            language = language,
                            currency = currency,
                            onLanguageChange = viewModel::setLanguage,
                            onCurrencyChange = viewModel::setCurrencySymbol,
                            onChangePassword = viewModel::changeOwnPassword,
                            onOpenBackup = {
                                viewModel.refreshLocalBackups()
                                navController.navigate(AppRoutes.BACKUP)
                            },
                            onLogout = viewModel::logout
                        )
                    }

                    composable(AppRoutes.BACKUP) {
                        BackHandler { navController.popBackStack() }
                        BackupScreen(
                            currentUser = currentUser,
                            localBackups = localBackups,
                            onCreateLocalBackup = viewModel::createLocalBackup,
                            onExportBackupUri = viewModel::exportBackupToUri,
                            onRestoreLocalBackup = viewModel::restoreLocalBackup,
                            onRestoreBackupUri = viewModel::restoreBackupFromUri,
                            onShareBackup = viewModel::shareLocalBackup,
                            onExportFullPdf = { viewModel.exportFullReportPdf() },
                            onExportExcel = viewModel::exportExcelWorkbook,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
