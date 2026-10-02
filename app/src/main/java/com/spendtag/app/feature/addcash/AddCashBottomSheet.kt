package com.spendtag.app.feature.addcash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.ui.theme.AmountLargeStyle
import com.spendtag.app.ui.theme.CategoryDefinition
import com.spendtag.app.ui.theme.CategoryPalette
import com.spendtag.app.ui.theme.SpendTagBottomSheetShape
import com.spendtag.app.ui.theme.SpendTagCardShape
import com.spendtag.app.ui.theme.SpendTagChipShape

/**
 * Rapid Add Cash Expense Bottom Sheet.
 * Features a dedicated tactile numeric keypad and instant category chips (Under 5 seconds).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCashBottomSheet(
    onDismiss: () -> Unit,
    onSaveExpense: (amountPaise: Long, categoryId: Long, note: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountInput by remember { mutableStateOf("0") }
    var selectedCategory by remember { mutableStateOf(CategoryPalette.FoodAndDining) }
    var noteInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SpendTagBottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Add Cash Expense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Large Number Display
            Text(
                text = "₹$amountInput",
                style = AmountLargeStyle,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 42.sp
            )

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPalette.defaultCategories.forEach { category ->
                    val isSelected = category.id == selectedCategory.id
                    Surface(
                        shape = SpendTagChipShape,
                        color = if (isSelected) category.color else category.color.copy(alpha = 0.12f),
                        contentColor = if (isSelected) Color.White else category.color,
                        modifier = Modifier.clickable { selectedCategory = category }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.name,
                                modifier = Modifier.size(15.dp),
                                tint = if (isSelected) Color.White else category.color
                            )
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Note Field
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                placeholder = { Text("What was this for? (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = SpendTagCardShape
            )

            // Large Tactile Keypad (3x4)
            TactileNumericKeypad(
                onKeyPress = { key ->
                    amountInput = when (key) {
                        "BACK" -> {
                            if (amountInput.length <= 1) "0"
                            else amountInput.dropLast(1)
                        }
                        "." -> {
                            if (amountInput.contains(".")) amountInput
                            else "$amountInput."
                        }
                        else -> { // Digit
                            if (amountInput == "0") key
                            else {
                                // Limit to 2 decimal places
                                val dotIndex = amountInput.indexOf(".")
                                if (dotIndex != -1 && amountInput.length - dotIndex > 2) {
                                    amountInput
                                } else {
                                    amountInput + key
                                }
                            }
                        }
                    }
                }
            )

            // Save CTA Button
            val amountPaise = CurrencyFormatter.parseToPaise(amountInput)
            Button(
                onClick = {
                    if (amountPaise > 0L) {
                        onSaveExpense(amountPaise, selectedCategory.id, noteInput.ifBlank { null })
                        onDismiss()
                    }
                },
                enabled = amountPaise > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = SpendTagCardShape
            ) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Expense (₹$amountInput)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun TactileNumericKeypad(
    onKeyPress: (String) -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "BACK")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(SpendTagCardShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onKeyPress(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "BACK") {
                            Icon(
                                imageVector = Icons.Rounded.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
