package com.spendtag.app.core.database

import androidx.room.TypeConverter
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus

/**
 * Room Type Converters for enums.
 * Storing enums as standardized uppercase strings guarantees readability and forward schema stability.
 */
class SpendTagTypeConverters {

    @TypeConverter
    fun fromPaymentDirection(direction: PaymentDirection?): String? = direction?.name

    @TypeConverter
    fun toPaymentDirection(value: String?): PaymentDirection? =
        value?.let { enumValueOf<PaymentDirection>(it) }

    @TypeConverter
    fun fromPaymentSource(source: PaymentSource?): String? = source?.name

    @TypeConverter
    fun toPaymentSource(value: String?): PaymentSource? =
        value?.let { enumValueOf<PaymentSource>(it) }

    @TypeConverter
    fun fromPaymentStatus(status: PaymentStatus?): String? = status?.name

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? =
        value?.let { enumValueOf<PaymentStatus>(it) }
}
