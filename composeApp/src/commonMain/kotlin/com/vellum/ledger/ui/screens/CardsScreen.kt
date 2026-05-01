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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.domain.CardType
import com.vellum.ledger.domain.LedgerCard
import com.vellum.ledger.ui.components.VellumTextField
import com.vellum.ledger.ui.components.VellumButton
import com.vellum.ledger.ui.components.ExpiryDateTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    cards: List<LedgerCard>,
    onAddCard: (String, String, CardType, String, Double, String) -> Unit,
    onDeleteCard: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Cards", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { paddingValues ->
        if (cards.isEmpty()) {
            EmptyCardsState { showAddDialog = true }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(cards) { card ->
                    CreditCardItem(card = card, onDelete = { onDeleteCard(card.id) })
                }
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
fun CreditCardItem(card: LedgerCard, onDelete: () -> Unit) {
    val cardColor = parseHexColor(card.hexColor)
    
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
                    "**** **** **** ${card.cardNumber}",
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

fun parseHexColor(hex: String): Color {
    return try {
        val colorHex = hex.removePrefix("#")
        val colorLong = colorHex.toLong(16)
        if (colorHex.length == 6) {
            Color(0xFF000000 or colorLong)
        } else {
            Color(colorLong)
        }
    } catch (e: Exception) {
        Color.Gray
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
    onConfirm: (String, String, CardType, String, Double, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CardType.Visa) }
    var selectedColor by remember { mutableStateOf("#1A1A1A") }

    var numberError by remember { mutableStateOf<String?>(null) }
    var expiryError by remember { mutableStateOf<String?>(null) }
    var balanceError by remember { mutableStateOf<String?>(null) }

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
                        onConfirm(name, number, selectedType, formattedExpiry, balance.toDoubleOrNull() ?: 0.0, selectedColor)
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
