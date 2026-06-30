package com.example.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    fun formatMinutesToDuration(minutes: Int): String {
        val absMinutes = Math.abs(minutes)
        val hours = absMinutes / 60
        val remainingMins = absMinutes % 60
        val sign = if (minutes > 0) "+" else if (minutes < 0) "-" else ""
        
        return when {
            hours > 0 -> "$sign${hours}h ${remainingMins}m"
            else -> "$sign${remainingMins}m"
        }
    }

    fun formatTime(millis: Long?): String {
        if (millis == null) return "--:--"
        val instant = Instant.ofEpochMilli(millis)
        val time = LocalTime.ofInstant(instant, ZoneId.systemDefault())
        return time.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    fun formatDateToHuman(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr)
            date.format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy"))
        } catch (e: Exception) {
            dateStr
        }
    }
}
