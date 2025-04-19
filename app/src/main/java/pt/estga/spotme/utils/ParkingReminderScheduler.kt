package pt.estga.spotme.utils

import ParkingReminderWorker
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object ParkingReminderScheduler {

    fun scheduleReminder(context: Context, delayMillis: Long, message: String) {
        val data = Data.Builder()
            .putString("message", message)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ParkingReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
