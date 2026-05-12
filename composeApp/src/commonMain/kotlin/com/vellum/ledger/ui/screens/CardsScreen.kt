package com.vellum.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.domain.CardType
import com.vellum.ledger.ui.model.CardUiModel
import com.vellum.ledger.ui.components.VellumTextField
import com.vellum.ledger.ui.components.VellumButton
import com.vellum.ledger.ui.components.ExpiryDateTransformation
import com.vellum.ledger.ui.util.parseHexColor
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    cards: List<CardUiModel>,
    onAddCard: (String, String, CardType, String, Long, String) -> Unit,
    onDeleteCard: (String) -> Unit,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Cards", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyCardsState { showAddDialog = true }
                }
            } else {
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { cards.size })
                
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        CreditCardItem(card = cards[page], onDelete = { onDeleteCard(cards[page].id) })
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Pager Indicator
                    Row(
                        Modifier
                            .height(10.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(cards.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(if (pagerState.currentPage == iteration) 24.dp else 8.dp, 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // Card Details / Stats Section
                Text(
                    "Card Details",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                val safeIndex = pagerState.currentPage.coerceIn(0, cards.size - 1)
                CardDetailList(cards[safeIndex], modifier = Modifier.padding(24.dp))
            }
        }
    }

    if (showAddDialog) {
        AddCardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, number, type, expiry, balance, color ->
                onAddCard(name, number, type, expiry, balance, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CardDetailList(card: CardUiModel, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailItem("Card Balance", card.balanceFormatted, MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
            DetailItem("Card Type", card.cardType.name, MaterialTheme.colorScheme.onSurface)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
            DetailItem("Holder Name", card.cardName, MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
    }
}

@Composable
fun CreditCardItem(card: CardUiModel, onDelete: () -> Unit) {
    val cardColor = card.color
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(cardColor, cardColor.copy(alpha = 0.7f))
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        card.cardType.name.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.weight(1f))

                Text(
                    card.cardNumberMasked,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("CARD HOLDER", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text(card.cardName.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EXPIRES", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text(card.expiry, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCardsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text("No cards linked", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Add your credit or debit cards to manage them here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Text("Add Card")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, CardType, String, Long, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var balance by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(CardType.Visa) }
    var selectedColor by rememberSaveable { mutableStateOf("#1A1A1A") }

    var numberError by rememberSaveable { mutableStateOf<String?>(null) }
    var expiryError by rememberSaveable { mutableStateOf<String?>(null) }
    var balanceError by rememberSaveable { mutableStateOf<String?>(null) }

    val isFormValid = name.isNotBlank() && 
                      number.length == 4 && 
                      expiry.length == 4 &&
                      (balance.isEmpty() || balance.toDoubleOrNull() != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VellumTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = "Card Holder Name", 
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank() && name.isNotEmpty()
                )
                VellumTextField(
                    value = number, 
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) number = it 
                        numberError = if (it.length < 4 && it.isNotEmpty()) "Need 4 digits" else null
                    }, 
                    label = "Last 4 Digits", 
                    modifier = Modifier.fillMaxWidth(),
                    isError = numberError != null,
                    supportingText = numberError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VellumTextField(
                        value = expiry, 
                        onValueChange = { 
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                expiry = it
                                expiryError = if (it.length == 4 && !it.matches(Regex("\\d{4}"))) "Invalid date" else null
                            }
                        }, 
                        label = "Expiry (MM/YY)", 
                        modifier = Modifier.weight(1f),
                        isError = expiryError != null,
                        supportingText = expiryError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ExpiryDateTransformation(),
                        placeholder = "MMYY"
                    )
                    VellumTextField(
                        value = balance, 
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                balance = it
                                balanceError = null
                            } else {
                                balanceError = "Invalid amount"
                            }
                        }, 
                        label = "Initial Balance", 
                        modifier = Modifier.weight(1f),
                        isError = balanceError != null,
                        supportingText = balanceError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                
                Text("Card Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) }
                        )
                    }
                }

                Text("Card Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("#1A1A1A", "#1565C0", "#2E7D32", "#C62828", "#6A1B9A").forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(color))
                                .clickable { selectedColor = color }
                                .then(if (selectedColor == color) Modifier.background(Color.White.copy(alpha = 0.3f)) else Modifier)
                        )
                    }
                }
            }
        },
        confirmButton = {
            VellumButton(
                onClick = {
                    if (isFormValid) {
                        val formattedExpiry = expiry.take(2) + "/" + expiry.drop(2)
                        val balanceCents = (balance.toDoubleOrNull()?.let { (it * 100 + 0.5).toLong() } ?: 0L)
                        onConfirm(name, number, selectedType, formattedExpiry, balanceCents, selectedColor)
                    }
                },
                text = "Add Card",
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
