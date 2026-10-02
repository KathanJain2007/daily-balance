package com.spendtag.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.common.DateTimeUtils
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentUiModel
import com.spendtag.app.ui.theme.AmountLargeStyle
import com.spendtag.app.ui.theme.CategoryPalette
import com.spendtag.app.ui.theme.ExpenseRedLight
import com.spendtag.app.ui.theme.SpendTagBottomSheetShape
import com.spendtag.app.ui.theme.SpendTagCardShape
import com.spendtag.app.ui.theme.SpendTagChipShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentDetailBottomSheet(
    payment: PaymentUiModel,
    onDismiss: () -> Unit,
    onChangeCategory: (Long) -> Unit,
    onSaveAlias: (String) -> Unit,
    onSaveNote: (String) -> Unit,
    onOpenSplitSheet: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var aliasText by remember { mutableStateOf(payment.payeeName) }
    var noteText by remember { mutableStateOf(payment.note ?: "") }
    var isEditingAlias by remember { mutableStateOf(false) }

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
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Category Icon & Formatted Amount
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(payment.category.color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = payment.category.icon,
                    contentDescription = payment.category.name,
                    tint = payment.category.color,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = CurrencyFormatter.formatPaise(payment.amountPaise),
                style = AmountLargeStyle,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Payee / Alias Editor
            if (isEditingAlias) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = aliasText,
                        onValueChange = { aliasText = it },
                        label = { Text("Payee Name / Alias") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = SpendTagCardShape
                    )
                    IconButton(
                        onClick = {
                            isEditingAlias = false
                            onSaveAlias(aliasText)
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Check, contentDescription = "Save Alias")
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = aliasText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { isEditingAlias = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit payee alias",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Info Details Card (UPI ID, Ref No, Timestamp, Source)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SpendTagCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (payment.upiId != null) {
                        DetailItemRow(label = "UPI ID", value = payment.upiId)
                    }
                    if (payment.refNo != null) {
                        DetailItemRow(label = "Ref / UTR No.", value = payment.refNo)
                    }
                    DetailItemRow(label = "Date & Time", value = DateTimeUtils.formatShortDateTime(payment.createdAt))
                    DetailItemRow(label = "Source", value = payment.source.name)
                    if (payment.isSplit) {
                        DetailItemRow(
                            label = "Personal Share",
                            value = CurrencyFormatter.formatPaise(payment.mySharePaise)
                        )
                    }
                }
            }

            // Split Bill Action Button (Outgoing payments)
            if (payment.direction == PaymentDirection.OUT) {
                Button(
                    onClick = onOpenSplitSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = SpendTagCardShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Rounded.CallSplit, contentDescription = "Split")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (payment.isSplit) "Edit Bill Split (${CurrencyFormatter.formatPaise(payment.mySharePaise)} my share)"
                               else "Split this bill with friends",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Change Category
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryPalette.defaultCategories.forEach { category ->
                        val isSelected = category.id == payment.categoryId
                        Surface(
                            shape = SpendTagChipShape,
                            color = if (isSelected) category.color else category.color.copy(alpha = 0.12f),
                            contentColor = if (isSelected) Color.White else category.color,
                            modifier = Modifier.clickable { onChangeCategory(category.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isSelected) Color.White else category.color
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Note Text Box
            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    onSaveNote(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes (optional)") },
                shape = SpendTagCardShape,
                maxLines = 3
            )

            // Delete Button (if manual)
            if (payment.source == PaymentSource.MANUAL && onDelete != null) {
                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = SpendTagCardShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRedLight)
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Delete Expense")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
