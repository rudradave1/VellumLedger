package com.vellum.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoSync: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
    lastSyncedMessage: String,
    onSyncNow: () -> Unit,
    onExportCSV: () -> Unit,
    onClearData: () -> Unit,
    onBack: () -> Unit
) {
    var selectedCurrency by remember { mutableStateOf("USD ($)") }
    
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onSyncNow) {
                        Text("↻", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            // SYNC SECTION
            item {
                SettingsSectionCard(
                    title = "Sync",
                    icon = "🔄",
                    content = {
                        Column {
                            SettingsToggleRow(
                                title = "Auto Sync",
                                checked = autoSync,
                                onCheckedChange = onAutoSyncChange
                            )
                            
                            SettingsDivider()
                            
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedButton(
                                    onClick = onSyncNow,
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier.height(44.dp).fillMaxWidth(0.4f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Sync Now", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Last synced: $lastSyncedMessage", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                )
            }

            // DATA SECTION
            item {
                SettingsSectionCard(
                    title = "Data",
                    icon = "📂",
                    content = {
                        Column {
                            SettingsActionRow(
                                title = "Export Data (CSV)",
                                onClick = onExportCSV,
                                trailingIcon = "›"
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                title = "Clear Local Data",
                                titleColor = MaterialTheme.colorScheme.error,
                                onClick = { showDeleteDialog = true }
                            )
                        }
                    }
                )
            }

            // PREFERENCES SECTION
            item {
                SettingsSectionCard(
                    title = "Preferences",
                    icon = "🎚️",
                    content = {
                        Column {
                            SettingsActionRow(
                                title = "Default Currency",
                                onClick = { showCurrencyDialog = true },
                                trailingText = selectedCurrency
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                title = "Theme",
                                onClick = { onDarkModeChange(!isDarkMode) },
                                trailingText = if (isDarkMode) "Dark" else "Light"
                            )
                        }
                    }
                )
            }

            // ABOUT SECTION
            item {
                SettingsSectionCard(
                    title = "About",
                    icon = "ⓘ",
                    content = {
                        Column {
                            SettingsActionRow(
                                title = "App Version",
                                trailingText = "v1.2.0",
                                clickable = false
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                title = "About LedgerSync",
                                onClick = { },
                                trailingIcon = "›"
                            )
                        }
                    }
                )
            }
        }
    }

    // Dialogs
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    listOf("USD ($)", "EUR (€)", "GBP (£)", "INR (₹)").forEach { currency ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selectedCurrency = currency
                                showCurrencyDialog = false
                            }.padding(vertical = 12.dp)
                        ) {
                            Text(currency, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear Local Data?", color = MaterialTheme.colorScheme.error) },
            text = { Text("This will permanently delete all transactions from this device. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearData()
                    showDeleteDialog = false
                }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    titleColor: Color = Color.Unspecified,
    trailingText: String? = null,
    trailingIcon: String? = null,
    clickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    val finalTitleColor = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, color = finalTitleColor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(trailingText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingIcon != null) {
                Text(
                    trailingIcon,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    )
}
