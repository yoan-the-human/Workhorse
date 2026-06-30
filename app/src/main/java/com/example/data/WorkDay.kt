package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Entity(tableName = "work_days")
data class WorkDay(
    @PrimaryKey val date: String, // Format: "yyyy-MM-dd"
    val actualStartMillis: Long? = null,
    val actualEndMillis: Long? = null,
    val defaultStartMinutes: Int = 9 * 60, // 09:00 -> 540 minutes
    val defaultEndMinutes: Int = 18 * 60, // 18:00 -> 1080 minutes
    val totalBreakMinutes: Int = 0,
    val activeBreakStartMillis: Long? = null,
    val isNonWorkDay: Boolean = false
) {
    fun checkIfNonWorkDay(): Boolean {
        if (actualStartMillis != null) return false
        
        val todayStr = LocalDate.now().toString()
        if (date == todayStr) {
            val nowHour = LocalTime.now().hour
            return nowHour >= 18
        }
        
        return try {
            val d = LocalDate.parse(date)
            d.isBefore(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }

    // Calculates the minutes from the start of the day for a millisecond timestamp
    private fun getMinutesOfDay(millis: Long): Int {
        val instant = Instant.ofEpochMilli(millis)
        val localTime = LocalTime.ofInstant(instant, ZoneId.systemDefault())
        return localTime.hour * 60 + localTime.minute
    }

    // Debt calculations in minutes
    fun getStartDebt(): Int {
        if (checkIfNonWorkDay() || actualStartMillis == null) return 0
        val actualStartMins = getMinutesOfDay(actualStartMillis)
        return actualStartMins - defaultStartMinutes
    }

    fun getEndDebt(): Int {
        if (checkIfNonWorkDay() || actualEndMillis == null) return 0
        val actualEndMins = getMinutesOfDay(actualEndMillis)
        return defaultEndMinutes - actualEndMins
    }

    fun getBreakDebt(): Int {
        if (checkIfNonWorkDay()) return 0
        if (actualStartMillis == null && actualEndMillis == null) return 0
        
        // Calculate breaks including active break if any
        val currentActiveBreakMins = if (activeBreakStartMillis != null) {
            val durationMs = System.currentTimeMillis() - activeBreakStartMillis
            (durationMs / 60000).toInt()
        } else {
            0
        }
        val totalBreaks = totalBreakMinutes + currentActiveBreakMins
        return totalBreaks - 60
    }

    fun getTotalDebt(): Int {
        if (checkIfNonWorkDay()) return 0
        if (actualStartMillis == null && actualEndMillis == null) return 0
        return getStartDebt() + getEndDebt() + getBreakDebt()
    }
}
