package pt.estga.spotme.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateFormatter {
    fun formatDate(time: Long): String {
        return if (time == 0L) {
            "Ainda em andamento"
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            sdf.format(Date(time))
        }
    }

    fun formatTime(time: Long): String {
        return if (time == 0L) {
            "Ainda em andamento"
        } else {
            val sdf = SimpleDateFormat("HH:mm'h'", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            sdf.format(Date(time))
        }
    }

    fun formatDateShort(time: Long): String {
        return if (time == 0L) {
            "Ainda em andamento"
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            sdf.format(Date(time))
        }
    }
}