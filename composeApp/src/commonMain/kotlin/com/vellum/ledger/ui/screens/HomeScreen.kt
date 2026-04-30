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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
                        Text("👛", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "LedgerSync",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSyncClick, enabled = !isSyncing) {
                        Text(
                            if (isSyncing) "⌛" else "↻", 
                            fontSize = 24.sp, 
                            color = if (isSyncing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    fontSize = 20.sp,
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

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TotalBalanceCard(balance: Double, income: Double, expense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Balance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                text = formatMoney(balance),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Income", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("+" + formatMoney(income), color = Secondary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Expense", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("-" + formatMoney(expense), color = Error, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { },
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val (icon, tint) = categoryIconAndTint(transaction.category)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                Text(transaction.note.ifBlank { "No description" }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transaction.type == TransactionType.Income) "+" else "-") + formatMoney(transaction.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (transaction.type == TransactionType.Income) Secondary else Error
                )
                SyncStatusIcon(
                    status = transaction.syncStatus,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
fun SyncStatusIcon(
    status: SyncStatus,
    onRetry: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    when (status) {
        SyncStatus.Synced -> Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Secondary, modifier = Modifier.size(18.dp))
        SyncStatus.Pending -> Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Pending, modifier = Modifier.size(18.dp))
        SyncStatus.Syncing -> Icon(
            Icons.Outlined.Sync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp).rotate(rotation),
        )
        SyncStatus.Failed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onRetry),
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Retry", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
