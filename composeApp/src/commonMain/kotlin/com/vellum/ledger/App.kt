package com.vellum.ledger

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vellum.ledger.repository.LedgerRepository
import com.vellum.ledger.ui.mapper.UiMapper
import com.vellum.ledger.ui.provider.CommonStringProvider
import com.vellum.ledger.ui.screens.AddTransactionScreen
import com.vellum.ledger.ui.screens.AnalyticsScreen
import com.vellum.ledger.ui.screens.CardsScreen
import com.vellum.ledger.ui.screens.HomeScreen
import com.vellum.ledger.ui.screens.SettingsScreen
import com.vellum.ledger.ui.components.VellumButton
import com.vellum.ledger.ui.theme.*
import com.vellum.ledger.ui.util.*
import com.vellum.ledger.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch

enum class Screen {
    Home,
    Cards,
    Analytics,
    Settings,
    AddTransaction
}

@Composable
fun App() {
    val repository = remember { LedgerRepository() }
    val stringProvider = remember { CommonStringProvider() }
    val uiMapper = remember { UiMapper(stringProvider) }
    val viewModel: LedgerViewModel = viewModel { LedgerViewModel(repository, uiMapper) }
    val haptic = LocalHapticFeedback.current
    val authenticator = rememberBiometricAuthenticator()
    
    val ledger by viewModel.ledger.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Home) }
    var exportCsvData by rememberSaveable { mutableStateOf<String?>(null) }
    var showReportDialog by rememberSaveable { mutableStateOf(false) }
    var isUnlocked by rememberSaveable { mutableStateOf(false) }

    val isAuthAvailable = remember(authenticator) { authenticator.isAvailable() }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorEvents) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkMode = settings.isDarkMode ?: isSystemDark

    LedgerTheme(darkTheme = isDarkMode, currency = settings.currency) {
        if (settings.isBiometricEnabled && !isUnlocked) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "VellumLedger is Locked",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Please authenticate to continue",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(48.dp))
                    
                    if (isAuthAvailable) {
                        VellumButton(
                            onClick = {
                                authenticator.authenticate(
                                    title = "Unlock VellumLedger",
                                    subtitle = "Confirm your identity to continue",
                                    onSuccess = { isUnlocked = true },
                                    onError = { /* Error handled by authenticator */ }
                                )
                            },
                            text = "Unlock Now",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // If no biometric/lock is set up, but the setting is somehow ON
                        // (e.g. user disabled lock in system settings after enabling it in app)
                        VellumButton(
                            onClick = { 
                                // Reset the setting so they aren't stuck
                                viewModel.toggleBiometricEnabled(false)
                                isUnlocked = true 
                            },
                            text = "Disable Lock & Continue",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No screen lock detected on this device. You can re-enable this in Settings after setting up a PIN or biometric.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (currentScreen != Screen.AddTransaction) {
                        BottomNavigationBar(
                            currentScreen = currentScreen,
                            onScreenSelected = { 
                                if (it != currentScreen) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentScreen = it 
                                }
                            }
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == Screen.AddTransaction || initialState == Screen.AddTransaction) {
                                (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                            } else {
                                fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
                            }
                        }
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> HomeScreen(
                                ledger = ledger,
                                transactions = transactions,
                                isSyncing = isSyncing,
                                onSyncClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.syncNow() 
                                },
                                onAddClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentScreen = Screen.AddTransaction 
                                },
                                onRetryTransaction = { id -> viewModel.retryTransaction(id) },
                                onDeleteTransaction = { id -> viewModel.deleteTransaction(id) },
                                lastSyncedMessage = settings.lastSyncMessage,
                            )
                            Screen.Cards -> {
                                CardsScreen(
                                    cards = cards,
                                    onAddCard = { name, number, type, expiry, balance, color ->
                                        viewModel.addCard(name, number, type, expiry, balance, color)
                                    },
                                    onDeleteCard = { id -> viewModel.deleteCard(id) }
                                )
                            }
                            Screen.Analytics -> AnalyticsScreen(
                                analytics = analytics,
                                settings = settings,
                                onViewReport = { showReportDialog = true },
                                onRefreshSummary = { force -> viewModel.refreshSummary(force) }
                            )
                            Screen.AddTransaction -> AddTransactionScreen(
                                onSave = { amount, type, category, note, timestamp ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addTransaction(amount, type, category, note, timestamp)
                                    currentScreen = Screen.Home
                                },
                                onBack = { currentScreen = Screen.Home }
                            )
                            Screen.Settings -> {
                                SettingsScreen(
                                    settings = settings,
                                    onDarkModeChange = { viewModel.toggleDarkMode(it) },
                                    onBiometricChange = { viewModel.toggleBiometricEnabled(it) },
                                    onAutoSyncChange = { viewModel.toggleAutoSync(it) },
                                    onSyncNow = { viewModel.syncNow() },
                                    isSyncing = isSyncing,
                                    onExportCSV = { 
                                        exportCsvData = viewModel.exportCSV()
                                    },
                                    onClearData = { viewModel.clearAll() },
                                    onPopulateDemoData = { viewModel.populateDemoData() },
                                    onDailyBudgetChange = { viewModel.setDailyBudget(it) },
                                    onCurrencyChange = { viewModel.setCurrency(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (exportCsvData != null) {
        AlertDialog(
            onDismissRequest = { exportCsvData = null },
            title = { Text("Export Data") },
            text = {
                Column {
                    Text("CSV Data generated successfully.", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("You can now share or save this file.")
                }
            },
            confirmButton = {
                Button(onClick = { 
                    com.vellum.ledger.data.shareText(exportCsvData!!, "Ledger_Export.csv")
                    exportCsvData = null 
                }) {
                    Text("Share / Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportCsvData = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { 
                Column {
                    Text("Financial Summary", fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Overview of your income and expenses.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val income = analytics.totalIncome
                    val expense = analytics.totalExpense
                    val balance = analytics.currentBalance
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReportItem("Total Income", income, IncomeColor)
                            ReportItem("Total Expense", expense, ExpenseColor)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ReportItem("Net Cash Flow", balance, if (balance >= 0) IncomeColor else ExpenseColor)
                        }
                    }
                    
                    Text(
                        "Net Cash Flow represents your total earnings minus total spending across all accounts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transactions", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${analytics.transactions.size}", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                VellumButton(
                    onClick = { showReportDialog = false },
                    text = "Close Report",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Composable
private fun ReportItem(label: String, amount: Double, color: Color) {
    val currency = com.vellum.ledger.ui.theme.LocalCurrency.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(com.vellum.ledger.ui.util.formatMoney(amount, currency), fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val items = listOf(
        Triple(Screen.Home, "Home", Icons.Outlined.Home),
        Triple(Screen.Cards, "Cards", Icons.Outlined.CreditCard),
        Triple(Screen.Analytics, "Charts", Icons.Outlined.BarChart),
        Triple(Screen.Settings, "Settings", Icons.Outlined.Settings)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                thickness = 1.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (screen, label, icon) ->
                    val selected = currentScreen == screen
                    
                    val backgroundAlpha = if (selected) 0.12f else 0f
                    val contentColor = if (selected) MaterialTheme.colorScheme.primary 
                                     else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = { onScreenSelected(screen) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = contentColor,
                                letterSpacing = 0.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
