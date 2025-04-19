import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import pt.estga.spotme.utils.ParkingNotificationHelper

class ParkingReminderWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        val message = inputData.getString("message") ?: return Result.failure()

        ParkingNotificationHelper.send(context, message)

        return Result.success()
    }
}
