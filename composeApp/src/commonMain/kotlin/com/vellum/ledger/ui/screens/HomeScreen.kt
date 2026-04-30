package com.vellum.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LedgerSync", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSyncClick, enabled = !isSyncing) {
                        Text(
                            if (isSyncing) "⌛" else "↻", 
                            fontSize = 24.sp, 
                            color = if (isSyncing) TextSecondary else Primary, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BalanceCard(ledger.analytics.currentBalance, ledger.analytics.totalIncome, ledger.analytics.totalExpense)
            }

            item {
                Text(
                    "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (ledger.transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet", color = TextSecondary)
                    }
                }
            } else {
                items(ledger.transactions.reversed()) { transaction ->
                    TransactionItem(transaction)
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun BalanceCard(balance: Double, income: Double, expense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Total Balance", color = TextSecondary, fontSize = 14.sp)
            Text(
                text = formatMoney(balance),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryItem("Income", income, Income, Modifier.weight(1f))
                SummaryItem("Expense", expense, Expense, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = (if (label == "Income") "+" else "-") + formatMoney(amount),
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TransactionItem(transaction: LedgerTransaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                transaction.category.take(1).uppercase(),
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.category, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Oct 24, 2023 • ${transaction.note.ifBlank { "No note" }}", fontSize = 12.sp, color = TextSecondary)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (transaction.type == TransactionType.Income) "+" else "-") + formatMoney(transaction.amount),
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.Income) Income else Expense
            )
            SyncStatusIndicator(transaction.syncStatus)
        }
    }
}

@Composable
fun SyncStatusIndicator(status: SyncStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val (text, color) = when (status) {
            SyncStatus.Synced -> "Synced" to Synced
            SyncStatus.Pending -> "Pending" to Warning
            SyncStatus.Syncing -> "Syncing..." to Syncing
            SyncStatus.Failed -> "Failed" to Failed
        }
        Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(4.dp))
        if (status == SyncStatus.Syncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 2.dp,
                color = color
            )
        } else {
            Box(Modifier.size(6.dp).background(color, CircleShape))
        }
        if (status == SyncStatus.Failed) {
            Spacer(Modifier.width(4.dp))
            Text(
                "Retry",
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { /* Retry logic */ }
            )
        }
    }
}

fun formatMoney(amount: Double): String {
    val totalCents = (abs(amount) * 100 + 0.5).toLong()
    val dollars = totalCents / 100
    val cents = totalCents % 100
    val dollarsStr = dollars.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$$dollarsStr.${cents.toString().padStart(2, '0')}"
}
