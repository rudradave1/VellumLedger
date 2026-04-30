package com.vellum.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    ledger: LedgerSnapshot
) {
    var selectedTab by remember { mutableStateOf(1) } // 0: Weekly, 1: Monthly, 2: Yearly
    val tabs = listOf("Weekly", "Monthly", "Yearly")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
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
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    fontSize = 14.sp, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) Primary else TextSecondary
                                ) 
                            }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard("Total Income", ledger.analytics.totalIncome, Income, Modifier.weight(1f))
                    MetricCard("Total Expense", ledger.analytics.totalExpense, Expense, Modifier.weight(1f))
                }
            }

            item {
                CurrentBalanceCard(ledger.analytics.currentBalance)
            }

            item {
                Text(
                    "Expense Breakdown",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val breakdown = getBreakdown(ledger)
            if (breakdown.isEmpty()) {
                item {
                    Text("No expenses to show", color = TextSecondary, modifier = Modifier.padding(16.dp))
                }
            } else {
                items(breakdown) { item ->
                    BreakdownItem(item)
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun MetricCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(label, color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                formatMoney(amount),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text("+14.5% from last month", fontSize = 10.sp, color = color)
        }
    }
}

@Composable
fun CurrentBalanceCard(balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Current Balance", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Text(
                formatMoney(balance),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

data class BreakdownData(val category: String, val amount: Double, val percentage: Float, val color: Color)

@Composable
fun BreakdownItem(data: BreakdownData) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(data.color, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(data.category, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
            Text(formatMoney(data.amount), fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("${(data.percentage * 100).toInt()}%", color = TextSecondary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { data.percentage },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = data.color,
            trackColor = data.color.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

fun getBreakdown(ledger: LedgerSnapshot): List<BreakdownData> {
    val expenses = ledger.transactions.filter { it.type == TransactionType.Expense }
    val totalExpense = expenses.sumOf { it.amount }
    if (totalExpense == 0.0) return emptyList()

    val categoryColors = listOf(Color(0xFF4339F2), Color(0xFFFF9800), Color(0xFF00BCD4), Color(0xFFE91E63), Color(0xFF4CAF50))

    return expenses.groupBy { it.category }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .toList()
        .sortedByDescending { it.second }
        .mapIndexed { index, (category, amount) ->
            BreakdownData(
                category = category,
                amount = amount,
                percentage = (amount / totalExpense).toFloat(),
                color = categoryColors[index % categoryColors.size]
            )
        }
}
