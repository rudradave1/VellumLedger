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
import com.vellum.ledger.ui.screens.AddTransactionScreen
import com.vellum.ledger.ui.screens.AnalyticsScreen
import com.vellum.ledger.ui.screens.CardsScreen
import com.vellum.ledger.ui.screens.HomeScreen
import com.vellum.ledger.ui.screens.SettingsScreen
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
    val viewModel: LedgerViewModel = viewModel { LedgerViewModel(repository) }
    val haptic = LocalHapticFeedback.current
    
    val ledger by viewModel.ledger.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val autoSync by viewModel.autoSync.collectAsState()
    val lastSyncedMessage by viewModel.lastSyncedMessage.collectAsState()
    val currency by viewModel.currency.collectAsState()
    
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var exportCsvData by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorEvents) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LedgerTheme(darkTheme = isDarkMode, currency = currency) {
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
                        )
                        Screen.Cards -> {
                            CardsScreen(
                                cards = ledger.cards,
                                onAddCard = { name, number, type, expiry, balance, color ->
                                    viewModel.addCard(name, number, type, expiry, balance, color)
                                },
                                onDeleteCard = { id -> viewModel.deleteCard(id) }
                            )
                        }
                        Screen.Analytics -> AnalyticsScreen(
                            ledger = ledger,
                            onViewReport = { showReportDialog = true }
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
                                isDarkMode = isDarkMode,
                                onDarkModeChange = { viewModel.toggleDarkMode(it) },
                                autoSync = autoSync,
                                onAutoSyncChange = { viewModel.toggleAutoSync(it) },
                                lastSyncedMessage = lastSyncedMessage,
                                onSyncNow = { viewModel.syncNow() },
                                isSyncing = isSyncing,
                                onExportCSV = { 
                                    exportCsvData = viewModel.exportCSV()
                                },
                                onClearData = { viewModel.clearAll() },
                                onBack = { currentScreen = Screen.Home },
                                onCurrencyChange = { viewModel.setCurrency(it) }
                            )
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
            title = { Text("Full Financial Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val income = ledger.analytics.totalIncome
                    val expense = ledger.analytics.totalExpense
                    val balance = ledger.analytics.currentBalance
                    
                    ReportItem("Total Income", income, Color(0xFF10B981))
                    ReportItem("Total Expense", expense, Color(0xFFEF4444))
                    HorizontalDivider()
                    ReportItem("Net Balance", balance, if (balance >= 0) Color(0xFF10B981) else Color(0xFFEF4444))
                    
                    Spacer(Modifier.height(8.dp))
                    Text("Transaction Count: ${ledger.transactions.size}", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showReportDialog = false }) {
                    Text("Got it")
                }
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
