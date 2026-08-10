package com.calisbloomprints.pos.pickup

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.calisbloomprints.pos.MainActivity
import com.calisbloomprints.pos.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PickupReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createChannel(context)
        if (!hasNotificationPermission(context)) return

        val saleId = intent.getLongExtra(EXTRA_SALE_ID, 0L)
        val receiptNumber = intent.getStringExtra(EXTRA_RECEIPT_NUMBER).orEmpty()
        val customerName = intent.getStringExtra(EXTRA_CUSTOMER_NAME).orEmpty()
        val pickupAt = intent.getLongExtra(EXTRA_PICKUP_AT, 0L)
        val customerLabel = customerName.ifBlank { receiptNumber.ifBlank { "Customer order" } }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            saleId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Pickup reminder")
            .setContentText("$customerLabel pickup at ${dateLabel(pickupAt)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$customerLabel has a pickup scheduled at ${dateLabel(pickupAt)}."),
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(saleId.toInt(), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pickup reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for customer pickup orders."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun dateLabel(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(timestamp))
    }

    companion object {
        const val EXTRA_SALE_ID = "sale_id"
        const val EXTRA_RECEIPT_NUMBER = "receipt_number"
        const val EXTRA_CUSTOMER_NAME = "customer_name"
        const val EXTRA_PICKUP_AT = "pickup_at"
        private const val CHANNEL_ID = "pickup_reminders"
    }
}
