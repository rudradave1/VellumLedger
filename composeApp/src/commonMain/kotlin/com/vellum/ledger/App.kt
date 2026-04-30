package com.vellum.ledger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vellum.ledger.repository.LedgerRepository
import com.vellum.ledger.ui.screens.AddTransactionScreen
import com.vellum.ledger.ui.screens.AnalyticsScreen
import com.vellum.ledger.ui.screens.HomeScreen
import com.vellum.ledger.ui.theme.Background
import com.vellum.ledger.ui.theme.Primary
import com.vellum.ledger.ui.theme.Surface
import com.vellum.ledger.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class Screen {
    Home,
    Analytics,
    Settings,
    AddTransaction
}

@Composable
fun App() {
    val repository = remember { LedgerRepository() }
    val ledger by repository.ledger.collectAsState()
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var isSyncing by remember { mutableStateOf(false) }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                if (currentScreen != Screen.AddTransaction) {
                    BottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { currentScreen = it }
                    )
                }
            },
            containerColor = Background
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        ledger = ledger,
                        isSyncing = isSyncing,
                        onSyncClick = {
                            scope.launch {
                                isSyncing = true
                                repository.syncNow()
                                isSyncing = false
                            }
                        },
                        onAddClick = { currentScreen = Screen.AddTransaction }
                    )
                    Screen.Analytics -> AnalyticsScreen(ledger = ledger)
                    Screen.AddTransaction -> AddTransactionScreen(
                        onSave = { amount, type, category, note ->
                            scope.launch {
                                repository.addTransaction(amount, type, category, note)
                                currentScreen = Screen.Home
                            }
                        },
                        onBack = { currentScreen = Screen.Home }
                    )
                    Screen.Settings -> {
                        Box(Modifier.padding(16.dp)) {
                            Text("Settings Screen (Coming Soon)", color = TextSecondary)
                        }
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
        containerColor = Surface,
        contentColor = Primary,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(Screen.Home, "Ledger", "L"),
            Triple(Screen.Analytics, "Analytics", "A"),
            Triple(Screen.Settings, "Settings", "S")
        )

        items.forEach { (screen, label, icon) ->
            NavigationBarItem(
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                icon = { 
                    Text(
                        icon, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (currentScreen == screen) Primary else TextSecondary
                    ) 
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
