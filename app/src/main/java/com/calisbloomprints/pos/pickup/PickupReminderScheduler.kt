package com.calisbloomprints.pos.pickup

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.calisbloomprints.pos.data.db.SaleWithItems

class PickupReminderScheduler(
    private val context: Context,
) {
    fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun schedule(saleWithItems: SaleWithItems): Boolean {
        val pickupAt = saleWithItems.sale.pickupAt ?: return true
        val reminderAt = saleWithItems.sale.pickupReminderAt ?: return true
        val triggerAt = reminderAt.coerceAtLeast(System.currentTimeMillis() + MIN_DELAY_MS)

        val intent = Intent(context, PickupReminderReceiver::class.java).apply {
            putExtra(PickupReminderReceiver.EXTRA_SALE_ID, saleWithItems.sale.id)
            putExtra(PickupReminderReceiver.EXTRA_RECEIPT_NUMBER, saleWithItems.sale.receiptNumber)
            putExtra(PickupReminderReceiver.EXTRA_CUSTOMER_NAME, saleWithItems.sale.customerName)
            putExtra(PickupReminderReceiver.EXTRA_PICKUP_AT, pickupAt)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            saleWithItems.sale.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        return hasNotificationPermission()
    }

    private companion object {
        const val MIN_DELAY_MS = 10_000L
    }
}
