package com.vellum.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.ui.theme.*
import com.vellum.ledger.ui.util.extractCurrencySymbol
import com.vellum.ledger.ui.components.VellumTextField
import com.vellum.ledger.ui.components.VellumButton
import com.vellum.ledger.data.currentTimeMillis
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    onSave: (Long, TransactionType, String, String, Long) -> Unit,
    onBack: () -> Unit
) {
    val currency = LocalCurrency.current
    val currencySymbol = extractCurrencySymbol(currency)
    var amountText by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TransactionType.Expense) }
    var selectedCategory by rememberSaveable { mutableStateOf("Food") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedTimestamp by rememberSaveable { mutableStateOf(currentTimeMillis()) }
    
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var amountError by rememberSaveable { mutableStateOf<String?>(null) }
    
    val amountCents = remember(amountText) {
        (amountText.toDoubleOrNull()?.let { (it * 100 + 0.5).toLong() } ?: 0L)
    }
    val isAmountValid = amountCents > 0

    val expenseCategories = remember {
        listOf(
            Category("Food", Icons.Outlined.Restaurant),
            Category("Transport", Icons.Outlined.Commute),
            Category("Shopping", Icons.Outlined.ShoppingBag),
            Category("Bills", Icons.Outlined.ReceiptLong),
            Category("Health", Icons.Outlined.MedicalServices),
            Category("Entertainment", Icons.Outlined.SportsEsports),
            Category("Others", Icons.Outlined.Category)
        )
    }

    val incomeCategories = remember {
        listOf(
            Category("Salary", Icons.Outlined.Payments),
            Category("Freelance", Icons.Outlined.LaptopMac),
            Category("Investment", Icons.Outlined.TrendingUp),
            Category("Gift", Icons.Outlined.CardGiftcard),
            Category("Others", Icons.Outlined.Category)
        )
    }

    val currentCategories = if (type == TransactionType.Expense) expenseCategories else incomeCategories

    // Auto-select first category if current selection is not valid for the new type
    LaunchedEffect(type) {
        if (currentCategories.none { it.name == selectedCategory }) {
            selectedCategory = currentCategories.first().name
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Add Transaction",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SegmentedTypeControl(
                    type = type,
                    onTypeChange = { type = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                )

                AmountCard(
                    amountText = amountText,
                    onAmountChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            amountText = it
                            amountError = if (it.isNotEmpty() && (it.toDoubleOrNull() ?: 0.0) <= 0.0) "Must be > 0" else null
                        }
                    },
                    type = type,
                    currencySymbol = currencySymbol,
                    isError = amountError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (amountError != null) {
                    Text(
                        amountError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "CATEGORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.5.sp
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        currentCategories.forEach { category ->
                            CategoryChip(
                                name = category.name,
                                icon = category.icon,
                                selected = selectedCategory == category.name,
                                onClick = { selectedCategory = category.name },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TRANSACTION DATE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 1.5.sp
                        )
                        TextButton(onClick = { showDatePicker = true }) {
                            val dateText = Instant.fromEpochMilliseconds(selectedTimestamp)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .let { "${it.dayOfMonth} ${it.month.name.take(3)}, ${it.year}" }
                            Text(dateText, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    VellumTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Note (Optional)",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "What was this for?",
                        minLines = 3
                    )
                }
                
                Spacer(Modifier.height(24.dp))
            }

            VellumButton(
                onClick = {
                    if (isAmountValid) {
                        onSave(amountCents, type, selectedCategory, note, selectedTimestamp)
                    } else {
                        amountError = "Invalid amount"
                    }
                },
                text = "Save Transaction",
                modifier = Modifier.fillMaxWidth(),
                enabled = isAmountValid
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Cap at today (current time)
                    return utcTimeMillis <= currentTimeMillis()
                }
                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear = Instant.fromEpochMilliseconds(currentTimeMillis())
                        .toLocalDateTime(TimeZone.currentSystemDefault()).year
                    return year <= currentYear
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTimestamp = datePickerState.selectedDateMillis ?: selectedTimestamp
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SegmentedTypeControl(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
    ) {
        Row(Modifier.padding(4.dp)) {
            SegmentedItem(
                text = "Expense",
                selected = type == TransactionType.Expense,
                onClick = { onTypeChange(TransactionType.Expense) },
                modifier = Modifier.weight(1f),
            )
            SegmentedItem(
                text = "Income",
                selected = type == TransactionType.Income,
                onClick = { onTypeChange(TransactionType.Income) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentedItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AmountCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    type: TransactionType,
    currencySymbol: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            2.dp, 
            if (isError) MaterialTheme.colorScheme.error 
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "ENTER AMOUNT",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    currencySymbol,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isError) MaterialTheme.colorScheme.error else (if (type == TransactionType.Expense) Color(0xFFEF4444) else Color(0xFF10B981)),
                )
                Spacer(Modifier.width(12.dp))
                val fontSize = when {
                    amountText.length > 12 -> 24.sp
                    amountText.length > 8 -> 32.sp
                    else -> 48.sp
                }
                BasicTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (amountText.isBlank()) {
                            Text(
                                "0.00",
                                fontSize = fontSize,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
                        inner()
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(name: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.15f
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = 0.7f
            ),
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                name,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        decorationBox = decorationBox
    )
}

private data class Category(
    val name: String,
    val icon: ImageVector,
)
