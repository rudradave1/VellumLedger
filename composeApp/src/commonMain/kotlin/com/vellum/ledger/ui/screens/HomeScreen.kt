package com.vellum.ledger.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    ledger: LedgerSnapshot,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onAddClick: () -> Unit,
    onRetryTransaction: (String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Payments, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "LedgerSync",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSyncClick, enabled = !isSyncing) {
                        Icon(
                            Icons.Outlined.Sync, 
                            contentDescription = "Sync",
                            tint = if (isSyncing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp).rotate(if (isSyncing) 180f else 0f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(24.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                TotalBalanceCard(
                    ledger.analytics.currentBalance,
                    ledger.analytics.totalIncome,
                    ledger.analytics.totalExpense
                )
            }

            item {
                Text(
                    "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (ledger.transactions.isEmpty()) {
                item {
                    EmptyState(onAddClick)
                }
            } else {
                items(ledger.transactions.reversed()) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onRetry = { onRetryTransaction(transaction.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun TotalBalanceCard(balance: Double, income: Double, expense: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3525CD),
                            Color(0xFF6366F1)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total Balance", 
                    color = Color.White.copy(alpha = 0.8f), 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatMoney(balance),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp),
                    letterSpacing = (-1).sp
                )
                
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column {
                        Text("Income", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            "+" + formatMoney(income), 
                            color = Color(0xFF4ADE80), 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("Expense", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            "-" + formatMoney(expense), 
                            color = Color(0xFFF87171), 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: LedgerTransaction,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val (icon, _) = categoryIconAndTint(transaction.category)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.category, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 16.sp
                )
                Text(
                    transaction.note.ifBlank { "Uncategorized" }, 
                    fontSize = 13.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transaction.type == TransactionType.Income) "+" else "-") + formatMoney(transaction.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (transaction.type == TransactionType.Income) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                Spacer(Modifier.height(4.dp))
                SyncStatusIndicator(
                    status = transaction.syncStatus,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    onRetry: () -> Unit,
) {
    when (status) {
        SyncStatus.Synced -> Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
        SyncStatus.Pending -> Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
        SyncStatus.Syncing -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
        SyncStatus.Failed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onRetry),
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Retry", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📭", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("No transactions yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Start by adding your first expense or income to begin tracking your finances.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("+ Add Transaction", modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

@Composable
private fun categoryIconAndTint(category: String): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (category.lowercase()) {
    "food" -> Icons.Outlined.Restaurant to MaterialTheme.colorScheme.primary
    "transport" -> Icons.Outlined.DirectionsCar to MaterialTheme.colorScheme.primary
    "shopping" -> Icons.Outlined.ShoppingCart to MaterialTheme.colorScheme.primary
    "bills" -> Icons.Outlined.ElectricBolt to MaterialTheme.colorScheme.tertiary
    "salary" -> Icons.Outlined.Payments to Secondary
    else -> Icons.Outlined.ShoppingCart to MaterialTheme.colorScheme.primary
}

fun formatMoney(amount: Double): String {
    val totalCents = (abs(amount) * 100 + 0.5).toLong()
    val dollars = totalCents / 100
    val cents = totalCents % 100
    val dollarsStr = dollars.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$$dollarsStr.${cents.toString().padStart(2, '0')}"
}
