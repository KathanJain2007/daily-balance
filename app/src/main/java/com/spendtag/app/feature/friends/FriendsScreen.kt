package com.spendtag.app.feature.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.database.dao.FriendWithBalance
import com.spendtag.app.ui.theme.AmountLargeStyle
import com.spendtag.app.ui.theme.AmountMediumStyle
import com.spendtag.app.ui.theme.AmountRowStyle
import com.spendtag.app.ui.theme.IncomeGreenLight
import com.spendtag.app.ui.theme.SpendTagCardShape
import com.spendtag.app.ui.theme.SpendTagHeroCardShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val totalOwed by viewModel.totalOwed.collectAsState()
    val friends by viewModel.friendsList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddFriendDialog by remember { mutableStateOf(false) }
    var selectedFriendForSettlement by remember { mutableStateOf<FriendWithBalance?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends & Splits", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddFriendDialog = true }) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Friend")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFriendDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Friend")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card: Total Owed
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SpendTagHeroCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CallSplit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Total Owed to You",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = CurrencyFormatter.formatPaise(totalOwed),
                            style = AmountLargeStyle,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        val owingFriendsCount = friends.count { it.balancePaise > 0 }
                        Text(
                            text = if (owingFriendsCount == 0) "All splits are settled up!"
                                   else "Across $owingFriendsCount friends with pending shares",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 2. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search friends by name or UPI...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = SpendTagCardShape,
                    singleLine = true
                )
            }

            // 3. Friends List
            if (friends.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SpendTagCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No friends added yet.\nSplit bills on any payment or tap '+' to add a friend.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(friends, key = { it.id }) { friend ->
                    FriendCardItem(
                        friend = friend,
                        onRemind = { viewModel.sendWhatsAppReminder(context, friend) },
                        onSettle = { selectedFriendForSettlement = friend }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Dialog: Add Friend
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onConfirm = { name, upiId, phone ->
                viewModel.addFriend(name, upiId, phone)
                showAddFriendDialog = false
            }
        )
    }

    // Dialog: Record Settlement
    selectedFriendForSettlement?.let { friend ->
        RecordSettlementDialog(
            friend = friend,
            onDismiss = { selectedFriendForSettlement = null },
            onConfirm = { amountPaise, note ->
                viewModel.recordSettlement(friend.id, amountPaise, note)
                selectedFriendForSettlement = null
            }
        )
    }
}

@Composable
fun FriendCardItem(
    friend: FriendWithBalance,
    onRemind: () -> Unit,
    onSettle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SpendTagCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Friend Avatar / Initial
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = friend.name.firstOrNull()?.uppercase() ?: "F",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column {
                        Text(
                            text = friend.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (friend.upiId != null) {
                            Text(
                                text = friend.upiId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Balance Chip
                if (friend.balancePaise > 0L) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Owes you",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = CurrencyFormatter.formatPaise(friend.balancePaise),
                            style = AmountMediumStyle,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Settled",
                            tint = IncomeGreenLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Settled",
                            style = MaterialTheme.typography.labelMedium,
                            color = IncomeGreenLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Action Buttons (Only when money is owed)
            if (friend.balancePaise > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRemind,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp")
                    }

                    Button(
                        onClick = onSettle,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settle")
                    }
                }
            }
        }
    }
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, upiId: String?, phone: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (e.g. rahul@okaxis)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (for WhatsApp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, upiId.ifBlank { null }, phone.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RecordSettlementDialog(
    friend: FriendWithBalance,
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, note: String?) -> Unit
) {
    val maxRupees = friend.balancePaise / 100
    var amountRupeesText by remember { mutableStateOf(maxRupees.toString()) }
    var note by remember { mutableStateOf("Settlement from ${friend.name}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Settlement", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${friend.name} owes you ${CurrencyFormatter.formatPaise(friend.balancePaise)}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = amountRupeesText,
                    onValueChange = { amountRupeesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Amount Settled (₹)") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Mode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amountPaise = (amountRupeesText.toLongOrNull() ?: 0L) * 100L
            Button(
                onClick = { onConfirm(amountPaise, note) },
                enabled = amountPaise > 0L
            ) {
                Text("Confirm Settlement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
