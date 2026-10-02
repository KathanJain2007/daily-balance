package com.spendtag.app.feature.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.database.entity.FriendEntity
import com.spendtag.app.core.model.PaymentUiModel
import com.spendtag.app.ui.theme.AmountMediumStyle
import com.spendtag.app.ui.theme.AmountRowStyle
import com.spendtag.app.ui.theme.SpendTagCardShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillBottomSheet(
    payment: PaymentUiModel,
    allFriends: List<FriendEntity>,
    onDismiss: () -> Unit,
    onAddNewFriend: (String) -> Unit,
    onConfirmSplit: (mySharePaise: Long, friendShares: List<Pair<Long, Long>>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var newFriendName by remember { mutableStateOf("") }
    var isEqualSplit by remember { mutableStateOf(true) }

    // Map of FriendId -> isSelected
    val selectedFriends = remember {
        mutableStateMapOf<Long, Boolean>().apply {
            allFriends.forEach { put(it.id, false) }
        }
    }

    // Map of FriendId -> customSharePaise
    val customShares = remember {
        mutableStateMapOf<Long, Long>().apply {
            allFriends.forEach { put(it.id, 0L) }
        }
    }

    val selectedCount = selectedFriends.count { it.value }
    val totalPeople = selectedCount + 1 // friends + user

    // Compute equal shares
    val perPersonEqualPaise = if (totalPeople > 0) payment.amountPaise / totalPeople else payment.amountPaise
    val remainderEqualPaise = if (totalPeople > 0) payment.amountPaise % totalPeople else 0L

    val computedMySharePaise = if (isEqualSplit) {
        perPersonEqualPaise + remainderEqualPaise
    } else {
        val friendsSum = selectedFriends.filter { it.value }.keys.sumOf { customShares[it] ?: 0L }
        (payment.amountPaise - friendsSum).coerceAtLeast(0L)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Split Bill",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total: ${CurrencyFormatter.formatPaise(payment.amountPaise)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Your Share",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = CurrencyFormatter.formatPaise(computedMySharePaise),
                            style = AmountMediumStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Equal Split Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SpendTagCardShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Equal Split",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isEqualSplit) "Dividing evenly among $totalPeople people" else "Enter custom share per friend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = isEqualSplit,
                    onCheckedChange = { isEqualSplit = it }
                )
            }

            // Quick Add Friend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newFriendName,
                    onValueChange = { newFriendName = it },
                    placeholder = { Text("Add friend name (e.g. Rahul)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        if (newFriendName.isNotBlank()) {
                            onAddNewFriend(newFriendName.trim())
                            newFriendName = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add")
                }
            }

            // Friends Selection List
            Text(
                text = "Select friends in this split:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (allFriends.isEmpty()) {
                Text(
                    text = "No friends added yet. Type a name above to add friends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allFriends, key = { it.id }) { friend ->
                        val isSelected = selectedFriends[friend.id] ?: false

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable {
                                    val next = !isSelected
                                    selectedFriends[friend.id] = next
                                    if (next && customShares[friend.id] == 0L) {
                                        customShares[friend.id] = perPersonEqualPaise
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedFriends[friend.id] = checked
                                        if (checked && customShares[friend.id] == 0L) {
                                            customShares[friend.id] = perPersonEqualPaise
                                        }
                                    }
                                )
                                Text(
                                    text = friend.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isSelected) {
                                if (isEqualSplit) {
                                    Text(
                                        text = CurrencyFormatter.formatPaise(perPersonEqualPaise),
                                        style = AmountRowStyle,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    val currentCustom = customShares[friend.id] ?: 0L
                                    OutlinedTextField(
                                        value = if (currentCustom > 0L) (currentCustom / 100).toString() else "",
                                        onValueChange = { input ->
                                            val rupees = input.filter { it.isDigit() }.toLongOrNull() ?: 0L
                                            customShares[friend.id] = rupees * 100L
                                        },
                                        prefix = { Text("₹") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(110.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Confirm Split Button
            Button(
                onClick = {
                    val shares = mutableListOf<Pair<Long, Long>>()
                    selectedFriends.forEach { (friendId, isChecked) ->
                        if (isChecked) {
                            val shareAmount = if (isEqualSplit) perPersonEqualPaise else (customShares[friendId] ?: 0L)
                            shares.add(Pair(friendId, shareAmount))
                        }
                    }
                    onConfirmSplit(computedMySharePaise, shares)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = SpendTagCardShape,
                enabled = selectedCount > 0
            ) {
                Text(
                    text = "Save Split (${selectedCount} Friends)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
