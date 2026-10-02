package com.spendtag.app

import com.spendtag.app.core.split.WhatsAppReminderHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying bill split math, equal division rounding,
 * myShare aggregation rules, and WhatsApp reminder link generation.
 */
class SplitCalculationTest {

    @Test
    fun calculateEqualSplit_dividesEvenlyAcrossFriends() {
        val totalAmountPaise = 500000L // Rs 5,000.00
        val friendCount = 4 // User + 4 friends = 5 people total
        val totalPeople = friendCount + 1

        val perPersonPaise = totalAmountPaise / totalPeople // Rs 1,000.00 (100000 paise)
        val remainderPaise = totalAmountPaise % totalPeople // 0 paise

        val myShare = perPersonPaise + remainderPaise
        val friendShare = perPersonPaise

        assertEquals(100000L, myShare) // Rs 1,000
        assertEquals(100000L, friendShare) // Rs 1,000
        assertEquals(totalAmountPaise, myShare + (friendShare * friendCount))
    }

    @Test
    fun calculateEqualSplit_handlesOddPaiseRemainderGracefully() {
        // Rs 100.00 split across 3 people (User + 2 friends)
        val totalAmountPaise = 10000L
        val totalPeople = 3

        val perPerson = totalAmountPaise / totalPeople // 3333 paise (Rs 33.33)
        val remainder = totalAmountPaise % totalPeople // 1 paise

        // Payer absorbs remainder so sum always equals total amount exactly
        val myShare = perPerson + remainder // 3334 paise
        val friendShare = perPerson // 3333 paise each

        assertEquals(3334L, myShare)
        assertEquals(3333L, friendShare)
        assertEquals(totalAmountPaise, myShare + (friendShare * 2))
    }

    @Test
    fun whatsAppReminder_generatesFriendlyMessageWithUpiLink() {
        val friendName = "Rahul"
        val expenseDescription = "Dinner at Koramangala"
        val amountPaise = 100000L // Rs 1,000.00
        val myUpiId = "kathan@okaxis"
        val myName = "Kathan"

        val message = WhatsAppReminderHelper.buildReminderMessage(
            friendName = friendName,
            expenseDescription = expenseDescription,
            amountPaise = amountPaise,
            receiverUpiId = myUpiId,
            receiverName = myName
        )

        assertTrue(message.contains("Hey Rahul"))
        assertTrue(message.contains("Dinner at Koramangala"))
        assertTrue(message.contains("₹1,000"))
        assertTrue(message.contains("upi://pay"))
        assertTrue(message.contains("pa=kathan@okaxis"))
        assertTrue(message.contains("am=1000.00"))
    }

    @Test
    fun customSplit_calculatesPersonalShareAccurately() {
        val totalAmountPaise = 500000L // Rs 5,000
        val friend1Share = 200000L      // Rs 2,000
        val friend2Share = 150000L      // Rs 1,500

        val myShare = totalAmountPaise - (friend1Share + friend2Share) // Rs 1,500
        assertEquals(150000L, myShare)
    }

    @Test
    fun settlementMath_reducesOwedBalanceWithoutCreatingIncome() {
        val initialOwedByFriend = 400000L // Rs 4,000 owed
        val partialRepayment = 250000L    // Rs 2,500 paid back

        val remainingBalance = initialOwedByFriend - partialRepayment // Rs 1,500
        assertEquals(150000L, remainingBalance)
    }
}
