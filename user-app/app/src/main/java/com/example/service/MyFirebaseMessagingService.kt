package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_FLYERS = "dealfinder_flyers"
        const val CHANNEL_PROMOTIONS = "dealfinder_promotions"
        const val CHANNEL_GENERAL = "dealfinder_general"

        /**
         * Revokes the current device's FCM token from Firestore on logout or account deletion.
         */
        fun revokeTokenForCurrentUser(token: String) {
            if (token.isBlank()) return
            com.example.data.FirebaseManager.getDb()
                .collection("fcmTokens")
                .document(token)
                .delete()
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Owner-scoped registration without email/PII; matches firestore.rules allowlist.
        com.example.data.FirebaseManager.getDb()
            .collection("fcmTokens").document(token)
            .set(
                mapOf(
                    "id" to token,
                    "userId" to uid,
                    "status" to "active",
                    "deviceType" to "Android",
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "DealFinder Update"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val route = remoteMessage.data["route"] ?: "home"
        val alertType = remoteMessage.data["alertType"] ?: "general"

        sendNotification(title, body, route, alertType)
    }

    private fun sendNotification(title: String, body: String, route: String, alertType: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("route", route)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val (channelId, channelName) = when (alertType) {
            "flyer_launch", "retailer_update" -> CHANNEL_FLYERS to "New Flyers & Store Updates"
            "promotion", "coupon" -> CHANNEL_PROMOTIONS to "Promotions & Coupons"
            else -> CHANNEL_GENERAL to "General Updates"
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
}
