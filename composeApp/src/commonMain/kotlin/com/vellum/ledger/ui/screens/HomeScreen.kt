package com.vellum.ledger.ui.screens

import com.vellum.ledger.ui.util.formatMoney
import org.jetbrains.compose.resources.stringResource
import vellumledger.composeapp.generated.resources.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.datetime.*
import kotlinx.coroutines.delay
import com.vellum.ledger.data.currentTimeMillis
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    ledger: LedgerSnapshot,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onAddClick: () -> Unit,
    onRetryTransaction: (String) -> Unit = {},
    onDeleteTransaction: (String) -> Unit = {},
    lastSyncedMessage: String = "",
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }
    
    val categories = remember(ledger.transactions) {
        listOf("All") + ledger.transactions.map { it.category }.distinct().sorted()
    }
    
    val filteredTransactions = remember(ledger.transactions, searchQuery, selectedCategoryFilter) {
        ledger.transactions.reversed().filter {
            val matchesSearch = it.note.contains(searchQuery, ignoreCase = true) || 
                                it.category.contains(searchQuery, ignoreCase = true) ||
                                it.amount.toString().contains(searchQuery)
            val matchesCategory = selectedCategoryFilter == "All" || it.category == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "VellumLedger",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (lastSyncedMessage.isNotEmpty() && !isSyncing) {
                                Text(
                                    lastSyncedMessage,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            IconButton(onClick = onSyncClick, enabled = !isSyncing) {
                                val rotation by animateFloatAsState(
                                    targetValue = if (isSyncing) 360f else 0f,
                                    animationSpec = if (isSyncing) {
                                        infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart)
                                    } else {
                                        spring()
                                    }
                                )
                                Icon(
                                    Icons.Outlined.Sync, 
                                    contentDescription = "Sync",
                                    tint = if (isSyncing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp).rotate(rotation)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                TotalBalanceCard(
                    ledger.analytics.currentBalance,
                    ledger.analytics.totalIncome,
                    ledger.analytics.totalExpense
                )
            }

            if (ledger.settings.dailyBudget > 0) {
                item {
                    DailyBudgetCard(ledger)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search transactions...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategoryFilter == category,
                                onClick = { selectedCategoryFilter = category },
                                label = { Text(category) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    enabled = true,
                                    selected = selectedCategoryFilter == category
                                )
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    if (searchQuery.isEmpty() && selectedCategoryFilter == "All") 
                        stringResource(Res.string.recent_transactions)
                    else 
                        "Filtered Results (${filteredTransactions.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isSyncing && ledger.transactions.isEmpty()) {
                items(5) {
                    TransactionSkeleton()
                }
            } else if (filteredTransactions.isEmpty()) {
                item {
                    if (ledger.transactions.isEmpty()) {
                        EmptyState(onAddClick)
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                itemsIndexed(filteredTransactions, key = { _, it -> it.id }) { index, transaction ->
                    var isVisible by remember { mutableStateOf(index > 8) }
                    LaunchedEffect(Unit) {
                        if (!isVisible) {
                            val delayTime = (index.coerceAtMost(5) * 50).toLong()
                            kotlinx.coroutines.delay(delayTime)
                            isVisible = true
                        }
                    }

                    val density = androidx.compose.ui.platform.LocalDensity.current
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300)) + 
                                slideInVertically(animationSpec = tween(300)) { with(density) { 20.dp.roundToPx() } }
                    ) {
                        SwipeToDeleteContainer(
                            onDelete = { onDeleteTransaction(transaction.id) }
                        ) {
                            TransactionListItem(
                                transaction = transaction,
                                onRetry = { onRetryTransaction(transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
    ) {
        content()
    }
}


@Composable
fun DailyBudgetCard(ledger: LedgerSnapshot) {
    val currency = LocalCurrency.current
    val budget = ledger.settings.dailyBudget
    
    val todaySpending = remember(ledger.transactions) {
        val now = currentTimeMillis()
        val timeZone = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date
        val startOfDay = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
        
        ledger.transactions
            .filter { it.createdAt >= startOfDay && it.type == TransactionType.Expense }
            .sumOf { it.amount }
    }

    val remaining = budget - todaySpending
    val progress = (todaySpending / budget).toFloat().coerceIn(0f, 1f)
    val isOverBudget = todaySpending > budget
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Daily Spending Limit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (remaining >= 0) {
                            "${formatMoney(remaining, currency)} left for today"
                        } else {
                            "${formatMoney(0.0, currency)} left · Over by ${formatMoney(abs(remaining), currency)}"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            (if (isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isOverBudget) Icons.Outlined.WarningAmber else Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = if (isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (progress > 0.9f) Color(0xFFEF4444) else if (progress > 0.7f) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                if (isOverBudget) "Over limit" else if (progress > 0.8f) "Almost at limit" else "On track",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TransactionSkeleton() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).background(Color.Gray.copy(alpha = 0.2f), CircleShape))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.width(100.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.width(150.dp).height(12.dp).background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
            }
            Box(modifier = Modifier.width(60.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
fun TotalBalanceCard(balance: Double, income: Double, expense: Double) {
    val currency = LocalCurrency.current
    var isBalanceVisible by rememberSaveable { mutableStateOf(true) }
    
    var targetBalance by remember { mutableStateOf(0f) }
    LaunchedEffect(balance) {
        targetBalance = balance.toFloat()
    }
    
    val animatedBalance by animateFloatAsState(
        targetValue = targetBalance,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 16.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3525CD),
                            Color(0xFF4F46E5),
                            Color(0xFF6366F1)
                        )
                    )
                )
                .padding(28.dp)
        ) {
            // Decorative circles
            Box(modifier = Modifier.size(150.dp).offset(x = 200.dp, y = (-80).dp).background(Color.White.copy(alpha = 0.05f), CircleShape))
            Box(modifier = Modifier.size(100.dp).offset(x = 240.dp, y = 100.dp).background(Color.White.copy(alpha = 0.03f), CircleShape))

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.total_balance), 
                        color = Color.White.copy(alpha = 0.8f), 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { isBalanceVisible = !isBalanceVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (isBalanceVisible) "Hide balance" else "Show balance",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = if (isBalanceVisible) {
                        formatMoney(animatedBalance.toDouble(), currency, compact = balance > 1_000_000_000_000)
                    } else {
                        "••••••••"
                    },
                    fontSize = when {
                        !isBalanceVisible -> 48.sp
                        balance > 1_000_000_000_000 -> 32.sp
                        balance > 1_000_000_000 -> 28.sp
                        balance > 1_000_000 -> 32.sp
                        else -> 48.sp
                    },
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = if (isBalanceVisible) (-1.5).sp else 2.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BalanceStatItem(
                        label = stringResource(Res.string.income),
                        amount = income,
                        icon = Icons.Outlined.ArrowDownward,
                        color = Color(0xFF4ADE80),
                        currency = currency,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                    
                    BalanceStatItem(
                        label = stringResource(Res.string.expense),
                        amount = expense,
                        icon = Icons.Outlined.ArrowUpward,
                        color = Color(0xFFF87171),
                        currency = currency,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceStatItem(
    label: String, 
    amount: Double, 
    icon: ImageVector, 
    color: Color, 
    currency: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label, 
                color = Color.White.copy(alpha = 0.6f), 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                formatMoney(amount, currency, compact = amount > 1_000_000), 
                color = Color.White, 
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: LedgerTransaction,
    onRetry: () -> Unit
) {
    val currency = LocalCurrency.current
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
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        transaction.note, 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transaction.type == TransactionType.Income) "+" else "-") + formatMoney(transaction.amount, currency, compact = transaction.amount > 1_000_000),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (transaction.type == TransactionType.Income) Color(0xFF10B981) else Color(0xFFEF4444),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

