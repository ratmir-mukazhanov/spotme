package pt.estga.spotme.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    fun formatDate(time: Long): String {
        return if (time == 0L) {
            "Ainda em andamento"
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(time))
        }
    }
}