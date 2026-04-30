package com.vellum.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    onSave: (Double, TransactionType, String, String) -> Unit,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.Expense) }
    var selectedCategory by remember { mutableStateOf("Food") }
    var note by remember { mutableStateOf("") }

    val categories = listOf("Food", "Transport", "Shopping", "Bills")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Add Transaction", fontWeight = FontWeight.Bold) } },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("✕", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = { Spacer(Modifier.width(48.dp)) }, // To center the title
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type Toggle
            Surface(
                modifier = Modifier.width(240.dp).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = Background
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    TypeToggleButton(
                        text = "Expense",
                        selected = type == TransactionType.Expense,
                        onClick = { type = TransactionType.Expense },
                        modifier = Modifier.weight(1f)
                    )
                    TypeToggleButton(
                        text = "Income",
                        selected = type == TransactionType.Income,
                        onClick = { type = TransactionType.Income },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Text("AMOUNT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text("$", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Start
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(IntrinsicSize.Min),
                    decorationBox = { innerTextField ->
                        if (amount.isEmpty()) {
                            Text("0.00", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = TextSecondary.copy(alpha = 0.3f))
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            Text("CATEGORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(16.dp))
            
            // Category Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEach { category ->
                    CategoryChip(
                        name = category,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        modifier = Modifier.width(100.dp)
                    )
                }
                // Add more button
                Surface(
                    modifier = Modifier.size(44.dp).clickable { },
                    shape = CircleShape,
                    color = Background
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Background,
                    focusedBorderColor = Primary
                )
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (amountDouble > 0) {
                        onSave(amountDouble, type, selectedCategory, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TypeToggleButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .background(
                if (selected) Color.White else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) (if (text == "Expense") Expense else Income) else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun CategoryChip(name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Primary.copy(alpha = 0.1f) else Background,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                color = if (selected) Primary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
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
