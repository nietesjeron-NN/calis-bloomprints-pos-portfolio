package com.calisbloomprints.pos.data.db

import androidx.room.TypeConverter
import com.calisbloomprints.pos.data.model.PaymentMethod
import com.calisbloomprints.pos.data.model.ProductCategory
import com.calisbloomprints.pos.data.model.ReceiptWidth

class PosTypeConverters {
    @TypeConverter
    fun categoryToString(value: ProductCategory): String = value.name

    @TypeConverter
    fun stringToCategory(value: String): ProductCategory = ProductCategory.valueOf(value)

    @TypeConverter
    fun paymentMethodToString(value: PaymentMethod): String = value.name

    @TypeConverter
    fun stringToPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun receiptWidthToString(value: ReceiptWidth): String = value.name

    @TypeConverter
    fun stringToReceiptWidth(value: String): ReceiptWidth = ReceiptWidth.valueOf(value)
}
