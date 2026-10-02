package com.spendtag.app.core.model

/**
 * Direction of monetary movement.
 * OUT = Expense / Debit / Outflow
 * IN  = Income / Refund / Friend Settlement / Inflow
 */
enum class PaymentDirection {
    OUT,
    IN
}
