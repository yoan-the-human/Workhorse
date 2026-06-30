package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class WorkDayRepository(private val workDayDao: WorkDayDao) {

    val allDaysAsc: Flow<List<WorkDay>> = workDayDao.getAllDaysAsc()
    val allDaysDesc: Flow<List<WorkDay>> = workDayDao.getAllDaysDesc()

    suspend fun getDay(date: String): WorkDay? {
        return workDayDao.getDay(date)
    }

    suspend fun insertDay(day: WorkDay) {
        workDayDao.insertDay(day)
    }

    suspend fun insertDays(days: List<WorkDay>) {
        workDayDao.insertDays(days)
    }

    suspend fun deleteAllDays() {
        workDayDao.deleteAllDays()
    }

    suspend fun deleteDay(day: WorkDay) {
        workDayDao.deleteDay(day)
    }

    suspend fun checkAndPrepopulateDummyData() {
        val currentDays = allDaysAsc.first()
        if (currentDays.isEmpty()) {
            val dummyDays = createDummyData()
            insertDays(dummyDays)
        }
    }

    private fun createDummyData(): List<WorkDay> {
        val zoneId = ZoneId.systemDefault()
        
        fun getMillis(dateStr: String, hour: Int, minute: Int): Long {
            return LocalDate.parse(dateStr)
                .atTime(LocalTime.of(hour, minute))
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }

        return listOf(
            // Day 1: 2026-06-16 (Tuesday)
            // Started 08:45 (-15), Ended 18:15 (-15), Break 45 mins (-15)
            // Total Debt: -45 mins (Light Blue)
            WorkDay(
                date = "2026-06-16",
                actualStartMillis = getMillis("2026-06-16", 8, 45),
                actualEndMillis = getMillis("2026-06-16", 18, 15),
                totalBreakMinutes = 45,
                isNonWorkDay = false
            ),
            // Day 2: 2026-06-17 (Wednesday)
            // Started 08:30 (-30), Ended 18:00 (0), Break 50 mins (-10)
            // Total Debt: -40 mins (Light Blue)
            WorkDay(
                date = "2026-06-17",
                actualStartMillis = getMillis("2026-06-17", 8, 30),
                actualEndMillis = getMillis("2026-06-17", 18, 0),
                totalBreakMinutes = 50,
                isNonWorkDay = false
            ),
            // Day 3: 2026-06-18 (Thursday)
            // Started 09:10 (+10), Ended 17:50 (+10), Break 75 mins (+15)
            // Total Debt: +35 mins (Red)
            WorkDay(
                date = "2026-06-18",
                actualStartMillis = getMillis("2026-06-18", 9, 10),
                actualEndMillis = getMillis("2026-06-18", 17, 50),
                totalBreakMinutes = 75,
                isNonWorkDay = false
            ),
            // Day 4: 2026-06-19 (Friday)
            // Started 08:55 (-5), Ended 18:30 (-30), Break 60 mins (0)
            // Total Debt: -35 mins (Light Blue)
            WorkDay(
                date = "2026-06-19",
                actualStartMillis = getMillis("2026-06-19", 8, 55),
                actualEndMillis = getMillis("2026-06-19", 18, 30),
                totalBreakMinutes = 60,
                isNonWorkDay = false
            ),
            // Day 5: 2026-06-20 (Saturday) -> Non-work Day
            WorkDay(
                date = "2026-06-20",
                isNonWorkDay = true
            ),
            // Day 6: 2026-06-21 (Sunday) -> Non-work Day
            WorkDay(
                date = "2026-06-21",
                isNonWorkDay = true
            ),
            // Day 7: 2026-06-22 (Monday)
            // Started 09:00 (0), Ended 18:05 (-5), Break 40 mins (-20)
            // Total Debt: -25 mins (Light Blue)
            WorkDay(
                date = "2026-06-22",
                actualStartMillis = getMillis("2026-06-22", 9, 0),
                actualEndMillis = getMillis("2026-06-22", 18, 5),
                totalBreakMinutes = 40,
                isNonWorkDay = false
            ),
            // Day 8: 2026-06-23 (Tuesday)
            // Started 09:05 (+5), Ended 18:00 (0), Break 55 mins (-5)
            // Total Debt: 0 mins (Light Blue)
            WorkDay(
                date = "2026-06-23",
                actualStartMillis = getMillis("2026-06-23", 9, 5),
                actualEndMillis = getMillis("2026-06-23", 18, 0),
                totalBreakMinutes = 55,
                isNonWorkDay = false
            ),
            // Day 9: 2026-06-24 (Wednesday)
            // Started 08:40 (-20), Ended 17:45 (+15), Break 90 mins (+30)
            // Total Debt: +25 mins (Red)
            WorkDay(
                date = "2026-06-24",
                actualStartMillis = getMillis("2026-06-24", 8, 40),
                actualEndMillis = getMillis("2026-06-24", 17, 45),
                totalBreakMinutes = 90,
                isNonWorkDay = false
            ),
            // Day 10: 2026-06-25 (Thursday)
            // Started 08:50 (-10), Ended 18:20 (-20), Break 50 mins (-10)
            // Total Debt: -40 mins (Light Blue)
            WorkDay(
                date = "2026-06-25",
                actualStartMillis = getMillis("2026-06-25", 8, 50),
                actualEndMillis = getMillis("2026-06-25", 18, 20),
                totalBreakMinutes = 50,
                isNonWorkDay = false
            )
        )
    }
}
