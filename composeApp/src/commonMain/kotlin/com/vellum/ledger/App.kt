package com.vellum.ledger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.repository.LedgerRepository
import com.vellum.ledger.ui.screens.AddTransactionScreen
import com.vellum.ledger.ui.screens.AnalyticsScreen
import com.vellum.ledger.ui.screens.HomeScreen
import com.vellum.ledger.ui.screens.SettingsScreen
import com.vellum.ledger.ui.theme.*
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
    val viewModel = remember { LedgerViewModel(repository) }
    
    val ledger by viewModel.ledger.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val autoSync by viewModel.autoSync.collectAsState()
    val lastSyncedMessage by viewModel.lastSyncedMessage.collectAsState()
    
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    LedgerTheme(darkTheme = isDarkMode) {
        Scaffold(
            bottomBar = {
                if (currentScreen != Screen.AddTransaction) {
                    BottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { currentScreen = it }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        ledger = ledger,
                        isSyncing = isSyncing,
                        onSyncClick = { viewModel.syncNow() },
                        onAddClick = { currentScreen = Screen.AddTransaction },
                        onRetryTransaction = { id -> viewModel.retryTransaction(id) },
                    )
                    Screen.Cards -> {
                         Box(Modifier.fillMaxSize()) {
                             Text("Cards Screen", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onBackground)
                         }
                    }
                    Screen.Analytics -> AnalyticsScreen(ledger = ledger)
                    Screen.AddTransaction -> AddTransactionScreen(
                        onSave = { amount, type, category, note ->
                            viewModel.addTransaction(amount, type, category, note)
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
                            onExportCSV = { /* Implement CSV Export */ },
                            onClearData = { viewModel.clearAll() },
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.height(80.dp)
    ) {
        val items = listOf(
            Triple(Screen.Home, "Home", "🏠"),
            Triple(Screen.Cards, "Cards", "💳"),
            Triple(Screen.Analytics, "Charts", "📊"),
            Triple(Screen.Settings, "Settings", "⚙️")
        )

        items.forEach { (screen, label, icon) ->
            val selected = currentScreen == screen
            NavigationBarItem(
                selected = selected,
                onClick = { onScreenSelected(screen) },
                icon = { 
                    Text(
                        icon, 
                        fontSize = 24.sp
                    ) 
                },
                label = { 
                    Text(
                        label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
