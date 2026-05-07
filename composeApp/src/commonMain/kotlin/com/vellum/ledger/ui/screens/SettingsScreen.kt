package com.vellum.ledger.ui.screens

import com.vellum.ledger.ui.model.SettingsUiModel
import com.vellum.ledger.ui.theme.LocalCurrency
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsUiModel,
    onDarkModeChange: (Boolean) -> Unit,
    onBiometricChange: (Boolean) -> Unit = {},
    onAutoSyncChange: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    isSyncing: Boolean,
    onExportCSV: () -> Unit,
    onClearData: () -> Unit,
    onPopulateDemoData: () -> Unit = {},
    onDailyBudgetChange: (Double) -> Unit = {},
    onCurrencyChange: (String) -> Unit
) {
    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var showDemoConfirm by rememberSaveable { mutableStateOf(false) }
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = onSyncNow, enabled = !isSyncing) {
                        Icon(
                            Icons.Outlined.Sync, 
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSection(title = "Sync", icon = Icons.Outlined.Sync) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto Sync", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Switch(checked = settings.autoSync, onCheckedChange = onAutoSyncChange)
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = onSyncNow,
                            enabled = !isSyncing,
                            modifier = Modifier.height(48.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Sync Now", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Text(
                            settings.lastSyncMessage, 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Data", icon = Icons.Outlined.Storage) {
                    Column {
                        //SettingsItem(title = "Load Demo Data", showArrow = true, onClick = { showDemoConfirm = true })
                        //HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        SettingsItem(title = "Export Data (CSV)", showArrow = true, onClick = onExportCSV)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        SettingsItem(title = "Clear Local Data", color = Color(0xFFEF4444), onClick = { showClearConfirm = true })
                    }
                }
            }

            item {
                SettingsSection(title = "Preferences", icon = Icons.Outlined.Settings) {
                    Column {
                        SettingsItem(
                            title = "Daily Spending Limit", 
                            value = if (settings.dailyBudget > 0) settings.dailyBudgetFormatted else "Not Set", 
                            showArrow = true,
                            onClick = { showBudgetDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        SettingsItem(
                            title = "Default Currency", 
                            value = settings.currency, 
                            showArrow = true,
                            onClick = { showCurrencyDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dark Mode", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Switch(checked = settings.isDarkMode ?: false, onCheckedChange = onDarkModeChange)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Biometric Lock", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Switch(checked = settings.isBiometricEnabled, onCheckedChange = onBiometricChange)
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "About", icon = Icons.Outlined.Info) {
                    Column {
                        SettingsItem(title = "App Version", value = com.vellum.ledger.data.appVersion)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        SettingsItem(title = "About VellumLedger", showArrow = true, onClick = { showAboutDialog = true })
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = settings.currency,
            onDismiss = { showCurrencyDialog = false },
            onConfirm = { 
                onCurrencyChange(it)
                showCurrencyDialog = false 
            }
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = settings.dailyBudget,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { 
                onDailyBudgetChange(it)
                showBudgetDialog = false 
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all your transactions and linked cards. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDemoConfirm) {
        AlertDialog(
            onDismissRequest = { showDemoConfirm = false },
            title = { Text("Load Demo Data?") },
            text = { Text("This will clear your current data and replace it with realistic demo data for testing and screenshots.") },
            confirmButton = {
                Button(
                    onClick = {
                        onPopulateDemoData()
                        showDemoConfirm = false
                    }
                ) {
                    Text("Load Demo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CurrencySelectionDialog(
    currentCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val currencies = listOf("USD ($)", "EUR (€)", "GBP (£)", "INR (₹)", "JPY (¥)", "CNY (¥)")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency") },
        text = {
            Column {
                Text(
                    "This will update the display symbol across the app. It does not perform real-time conversion of existing values.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                currencies.forEach { currency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(currency) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(currency)
                        if (currency == currentCurrency) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Spending Limit") },
        text = {
            Column {
                Text(
                    "Set a daily budget to track your spending habits. Set to 0 to disable.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) text = it },
                    label = { Text("Daily Budget") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toDoubleOrNull() ?: 0.0) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) },
        title = { Text("About VellumLedger", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Version ${com.vellum.ledger.data.appVersion}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text(
                    "VellumLedger is a premium financial tracking application designed for speed, security, and simplicity. Manage your cards and transactions with ease.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                Text("© 2026 VellumLedger Team", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String, 
    value: String? = null, 
    showArrow: Boolean = false, 
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = color)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                if (showArrow) Spacer(Modifier.width(8.dp))
            }
            if (showArrow) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
