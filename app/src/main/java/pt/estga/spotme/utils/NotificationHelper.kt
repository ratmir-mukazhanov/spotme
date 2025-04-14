package pt.estga.spotme.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

object NotificationHelper {

    fun createChannel(context: Context, channelId: String, channelName: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (notificationManager == null) {
                Log.e("Notification", "NotificationManager é null!")
                return
            }

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = description
            }

            notificationManager.createNotificationChannel(channel)
            Log.d("Notification", "Notification Channel criado com sucesso!")
        }
    }
}
