package com.spendtag.app.core.model

/**
 * Lifecycle status of a payment.
 * PENDING  = Detected via SMS/Notification, awaiting user category tag.
 * TAGGED   = Categorized by user or confident auto-tag rule.
 * UNSORTED = Explicitly skipped or archived without a category.
 */
enum class PaymentStatus {
    PENDING,
    TAGGED,
    UNSORTED
}
