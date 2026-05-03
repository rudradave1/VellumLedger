package com.vellum.ledger.ui.screens

import com.vellum.ledger.ui.theme.LocalCurrency
import com.vellum.ledger.ui.util.formatMoney
import com.vellum.ledger.data.currentTimeMillis
import kotlinx.datetime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    ledger: LedgerSnapshot,
    onViewReport: () -> Unit = {},
    onRefreshSummary: (Boolean) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(1) } // 0: Weekly, 1: Monthly, 2: Yearly
    val tabs = listOf("Weekly", "Monthly", "Yearly")

    LaunchedEffect(Unit) {
        println("AnalyticsScreen: LaunchedEffect triggering onRefreshSummary")
        onRefreshSummary(false)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Charts", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
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
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Review your financial performance for the current period.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        tabs.forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTab == index) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { selectedTab = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("Spending Trend", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                TrendChart(ledger, selectedTab)
            }

            item {
                Text("Smart Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                InsightsCard(ledger)
            }

            ledger.settings.monthlySummary?.let { summary ->
                item {
                    Text("Monthly AI Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    AiSummaryCard(summary, onRefresh = { onRefreshSummary(true) })
                }
            }

            item {
                val now = currentTimeMillis()
                val stats = remember(selectedTab, ledger.transactions) {
                    val periodStart = when (selectedTab) {
                        0 -> now - (7 * 24 * 60 * 60 * 1000L) // Weekly
                        1 -> now - (30 * 24 * 60 * 60 * 1000L) // Monthly
                        else -> now - (365 * 24 * 60 * 60 * 1000L) // Yearly
                    }
                    val prevPeriodStart = periodStart - (now - periodStart)
                    
                    val currentRange = ledger.transactions.filter { it.createdAt >= periodStart }
                    val prevRange = ledger.transactions.filter { it.createdAt in prevPeriodStart until periodStart }
                    
                    val curInc = currentRange.filter { it.type == TransactionType.Income }.sumOf { it.amount }
                    val curExp = currentRange.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                    
                    val prevInc = prevRange.filter { it.type == TransactionType.Income }.sumOf { it.amount }
                    val prevExp = prevRange.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                    
                    val incChange = if (prevInc > 0) ((curInc - prevInc) / prevInc * 100).toInt() else null
                    val expChange = if (prevExp > 0) ((curExp - prevExp) / prevExp * 100).toInt() else null
                    
                    listOf(curInc, curExp, incChange?.toDouble() ?: Double.NaN, expChange?.toDouble() ?: Double.NaN)
                }

                val periodIncome = stats[0]
                val periodExpense = stats[1]
                val incomeChange = if (stats[2].isNaN()) null else stats[2].toInt()
                val expenseChange = if (stats[3].isNaN()) null else stats[3].toInt()

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SummaryCard(
                            label = "Income",
                            amount = periodIncome,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            isIncrease = (incomeChange ?: 0) >= 0,
                            percentageText = when {
                                incomeChange == null -> "No previous data"
                                incomeChange == 0 -> "No change"
                                else -> "${if (incomeChange > 0) "+" else ""}$incomeChange% from last period"
                            }
                        )
                        SummaryCard(
                            label = "Expense",
                            amount = periodExpense,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f),
                            isIncrease = (expenseChange ?: 0) < 0,
                            percentageText = when {
                                expenseChange == null -> "No previous data"
                                expenseChange == 0 -> "No change"
                                else -> "${if (expenseChange > 0) "+" else ""}$expenseChange% from last period"
                            }
                        )
                    }
                    BalanceHighlightCard(periodIncome - periodExpense)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Expense Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(20.dp))
                        
                        val filteredTransactions = remember(selectedTab, ledger.transactions) {
                            val now = currentTimeMillis()
                            val periodStart = when (selectedTab) {
                                0 -> now - (7 * 24 * 60 * 60 * 1000L)
                                1 -> now - (30 * 24 * 60 * 60 * 1000L)
                                else -> now - (365 * 24 * 60 * 60 * 1000L)
                            }
                            ledger.transactions.filter { it.createdAt >= periodStart }
                        }
                        
                        val breakdownColors = listOf(Color(0xFF3525CD), Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899))
                        val breakdown = getBreakdown(filteredTransactions, breakdownColors)
                        if (breakdown.isEmpty()) {
                            Text("No data for this period", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            breakdown.forEach { item ->
                                BreakdownRow(item)
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        TextButton(
                            onClick = onViewReport,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("View Full Report", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SummaryCard(label: String, amount: Double, color: Color, modifier: Modifier, isIncrease: Boolean, percentageText: String = "") {
    val currency = LocalCurrency.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isIncrease) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatMoney(amount, currency),
                fontSize = if (amount > 1_000_000) 18.sp else 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (percentageText.isNotEmpty()) {
                Text(
                    percentageText, 
                    fontSize = 10.sp, 
                    color = color.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun BalanceHighlightCard(balance: Double) {
    val currency = LocalCurrency.current
    Surface(
        modifier = Modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3525CD), Color(0xFF6366F1))
                )
            )
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
                }
                Text(
                    text = formatMoney(balance, currency),
                    fontSize = if (balance > 1_000_000) 24.sp else 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BreakdownRow(data: BreakdownData) {
    val currency = LocalCurrency.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(data.color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(data.category, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                Text(formatMoney(data.amount, currency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Text("${(data.percentage * 100).toInt()}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(32.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { data.percentage },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = data.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

data class BreakdownData(val category: String, val amount: Double, val percentage: Float, val color: Color)

@Composable
fun TrendChart(ledger: LedgerSnapshot, selectedTab: Int) {
    val currency = LocalCurrency.current
    val now = currentTimeMillis()
    val timeZone = TimeZone.currentSystemDefault()
    val today = kotlinx.datetime.Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val chartData = remember(ledger.transactions, selectedTab) {
        // ... (existing chart data logic)
        when (selectedTab) {
            0 -> { // Weekly
                (0 until 7).map { i ->
                    val date = today.minus(6 - i, DateTimeUnit.DAY)
                    val start = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val label = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
                    val transactions = ledger.transactions.filter { it.createdAt in start until end }
                    val income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }
                    val expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                    ChartPoint(label, income, expense)
                }
            }
            1 -> { // Monthly
                (0 until 6).map { i ->
                    val date = today.minus((5 - i) * 5, DateTimeUnit.DAY)
                    val start = date.minus(4, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val label = "${date.dayOfMonth} ${date.month.name.take(3)}"
                    val transactions = ledger.transactions.filter { it.createdAt in start until end }
                    val income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }
                    val expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                    ChartPoint(label, income, expense)
                }
            }
            else -> { // Yearly
                (0 until 12).map { i ->
                    val date = today.minus(11 - i, DateTimeUnit.MONTH)
                    val start = LocalDate(date.year, date.month, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val end = LocalDate(date.year, date.month, 1).plus(1, DateTimeUnit.MONTH).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val label = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
                    val transactions = ledger.transactions.filter { it.createdAt in start until end }
                    val income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }
                    val expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                    ChartPoint(label, income, expense)
                }
            }
        }
    }

    val maxVal = chartData.flatMap { listOf(it.income, it.expense) }.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

    Surface(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartData.forEachIndexed { index, point ->
                        val isSelected = selectedPointIndex == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedPointIndex = if (selectedPointIndex == index) null else index }
                                ), 
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(4.dp)) else Modifier), 
                                    horizontalArrangement = Arrangement.spacedBy(2.dp), 
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    val incomeHeight = (point.income / maxVal).toFloat().coerceIn(0.01f, 1f)
                                    val expenseHeight = (point.expense / maxVal).toFloat().coerceIn(0.01f, 1f)
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(incomeHeight)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF10B981), Color(0xFF10B981).copy(alpha = 0.3f))
                                                )
                                            )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(expenseHeight)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.3f))
                                                )
                                            )
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = point.label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Tooltip
                selectedPointIndex?.let { index ->
                    val point = chartData[index]
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp,
                        shadowElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(point.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("Inc: ${formatMoney(point.income, currency)}", fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("Exp: ${formatMoney(point.expense, currency)}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ChartLegendItem("Income", Color(0xFF10B981))
                Spacer(Modifier.width(24.dp))
                ChartLegendItem("Expense", Color(0xFFEF4444))
            }
        }
    }
}

data class ChartPoint(val label: String, val income: Double, val expense: Double)

@Composable
fun InsightsCard(ledger: LedgerSnapshot) {
    val currency = LocalCurrency.current
    val now = currentTimeMillis()
    val weekInMillis = 7 * 24 * 60 * 60 * 1000L
    
    val currentWeekTransactions = ledger.transactions.filter { it.createdAt >= now - weekInMillis && it.type == TransactionType.Expense }
    val lastWeekTransactions = ledger.transactions.filter { it.createdAt in (now - 2 * weekInMillis) until (now - weekInMillis) && it.type == TransactionType.Expense }
    
    val currentWeekTotal = currentWeekTransactions.sumOf { it.amount }
    val lastWeekTotal = lastWeekTransactions.sumOf { it.amount }
    
    val largestCategory = currentWeekTransactions.groupBy { it.category }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .maxByOrNull { it.value }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (lastWeekTotal > 0) {
                val diff = currentWeekTotal - lastWeekTotal
                val percent = (abs(diff) / lastWeekTotal * 100).toInt()
                val trend = if (diff >= 0) "more" else "less"
                InsightItem(
                    icon = if (diff >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    text = "You spent $percent% $trend this week vs last week.",
                    color = if (diff >= 0) Color(0xFFEF4444) else Color(0xFF10B981)
                )
            }
            
            largestCategory?.let { (category, amount) ->
                InsightItem(
                    icon = Icons.Outlined.Info,
                    text = "Largest expense category: $category (${formatMoney(amount, currency)})",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (currentWeekTransactions.isEmpty() && lastWeekTotal == 0.0) {
                Text("Not enough data for insights yet. Keep tracking!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AiSummaryCard(summary: String, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI Analysis", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                summary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun InsightItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

fun getBreakdown(transactions: List<com.vellum.ledger.domain.LedgerTransaction>, categoryColors: List<Color>): List<BreakdownData> {
    val expenses = transactions.filter { it.type == TransactionType.Expense }
    val totalExpense = expenses.sumOf { it.amount }
    if (totalExpense == 0.0) return emptyList()

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
