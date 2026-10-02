package com.spendtag.app.feature.friends

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendtag.app.core.database.dao.FriendWithBalance
import com.spendtag.app.core.database.entity.FriendEntity
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus
import com.spendtag.app.core.split.WhatsAppReminderHelper
import com.spendtag.app.data.repository.PaymentRepository
import com.spendtag.app.data.repository.SplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val splitRepository: SplitRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val totalOwed: StateFlow<Long> = splitRepository.totalOwedAcrossAllFriends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val friendsList: StateFlow<List<FriendWithBalance>> = combine(
        splitRepository.friendsWithBalances,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.name.contains(query, ignoreCase = true) || (it.upiId?.contains(query, ignoreCase = true) == true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addFriend(name: String, upiId: String?, phone: String?) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                splitRepository.addFriend(name, upiId, phone)
            }
        }
    }

    /**
     * Records a manual cash or UPI reimbursement settlement for a friend.
     */
    fun recordSettlement(friendId: Long, amountPaise: Long, note: String?) {
        viewModelScope.launch {
            // Create a settlement payment entity with direction = IN
            val settlementPayment = PaymentEntity(
                amountPaise = amountPaise,
                direction = PaymentDirection.IN,
                upiId = null,
                refNo = null,
                source = PaymentSource.MANUAL,
                createdAt = System.currentTimeMillis(),
                categoryId = 11L, // Income / Transfer
                myShare = 0L,     // 0 personal expense
                status = PaymentStatus.TAGGED,
                note = note ?: "Reimbursement Settlement"
            )
            val paymentId = paymentRepository.insertIfUnique(settlementPayment) ?: return@launch
            splitRepository.recordSettlement(
                friendId = friendId,
                paymentId = paymentId,
                amountPaise = amountPaise,
                note = note
            )
        }
    }

    /**
     * Creates and launches the WhatsApp reminder intent.
     */
    fun sendWhatsAppReminder(
        context: Context,
        friend: FriendWithBalance,
        myUpiId: String? = null,
        myName: String? = null
    ) {
        val intent = WhatsAppReminderHelper.createReminderIntent(
            context = context,
            friendName = friend.name,
            phoneNumber = friend.phoneNumber,
            expenseDescription = null,
            amountPaise = friend.balancePaise,
            receiverUpiId = myUpiId,
            receiverName = myName
        )
        context.startActivity(Intent.createChooser(intent, "Send WhatsApp Reminder"))
    }
}
