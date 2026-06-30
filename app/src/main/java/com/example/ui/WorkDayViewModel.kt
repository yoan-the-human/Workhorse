package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.WorkDay
import com.example.data.WorkDayRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WorkDayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkDayRepository

    val allDaysAsc: StateFlow<List<WorkDay>>
    val allDaysDesc: StateFlow<List<WorkDay>>
    val todayWorkDay: StateFlow<WorkDay?>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WorkDayRepository(db.workDayDao())

        allDaysAsc = repository.allDaysAsc.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allDaysDesc = repository.allDaysDesc.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        todayWorkDay = repository.allDaysAsc.map { list ->
            val todayStr = LocalDate.now().toString()
            list.find { it.date == todayStr } ?: WorkDay(date = todayStr)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WorkDay(date = LocalDate.now().toString())
        )

        // Run pre-population on startup
        viewModelScope.launch {
            repository.checkAndPrepopulateDummyData()
        }
    }

    // --- Core Actions ---

    fun startWorkday(date: String = LocalDate.now().toString()) {
        viewModelScope.launch {
            val existing = repository.getDay(date) ?: WorkDay(date = date)
            if (existing.actualStartMillis == null) {
                repository.insertDay(existing.copy(
                    actualStartMillis = System.currentTimeMillis(),
                    isNonWorkDay = false
                ))
            }
        }
    }

    fun endWorkday(date: String = LocalDate.now().toString()) {
        viewModelScope.launch {
            val existing = repository.getDay(date) ?: WorkDay(date = date)
            if (existing.actualEndMillis == null) {
                repository.insertDay(existing.copy(
                    actualEndMillis = System.currentTimeMillis()
                ))
            }
        }
    }

    fun toggleBreak(date: String = LocalDate.now().toString()) {
        viewModelScope.launch {
            val existing = repository.getDay(date) ?: WorkDay(date = date)
            val now = System.currentTimeMillis()
            if (existing.activeBreakStartMillis == null) {
                // Going outside
                repository.insertDay(existing.copy(
                    activeBreakStartMillis = now,
                    isNonWorkDay = false
                ))
            } else {
                // Back to desk
                val durationMs = now - existing.activeBreakStartMillis
                val durationMins = (durationMs / 60000).toInt()
                repository.insertDay(existing.copy(
                    totalBreakMinutes = existing.totalBreakMinutes + durationMins,
                    activeBreakStartMillis = null
                ))
            }
        }
    }


    fun deleteDayRecord(date: String) {
        viewModelScope.launch {
            val existing = repository.getDay(date)
            if (existing != null) {
                repository.deleteDay(existing)
            }
        }
    }

    fun saveDailyTargets(
        date: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        viewModelScope.launch {
            val existing = repository.getDay(date) ?: WorkDay(date = date)
            val defStart = startHour * 60 + startMinute
            val defEnd = endHour * 60 + endMinute
            repository.insertDay(existing.copy(
                defaultStartMinutes = defStart,
                defaultEndMinutes = defEnd
            ))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllDays()
        }
    }

    // --- Export & Import ---

    fun shareBackup(context: Context) {
        viewModelScope.launch {
            try {
                val list = allDaysAsc.first()
                val jsonArray = JSONArray()
                for (day in list) {
                    if (day.checkIfNonWorkDay()) continue // skip rest days
                    val obj = JSONObject().apply {
                        put("date", day.date)
                        put("start", day.getStartDebt())
                        put("end", day.getEndDebt())
                        put("middle", day.getBreakDebt())
                    }
                    jsonArray.put(obj)
                }

                val jsonStr = jsonArray.toString(2)

                // Write to cache file
                val cacheFile = File(context.cacheDir, "workhorse_backup.json")
                cacheFile.writeText(jsonStr)

                // Get share URI using FileProvider
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Workhorse Data Backup")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share Workhorse Backup")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun importBackup(jsonStr: String): Boolean {
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<WorkDay>()
            val zoneId = ZoneId.systemDefault()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val date = obj.getString("date")
                val startDebt = obj.optInt("start", 0)
                val endDebt = obj.optInt("end", 0)
                val breakDebt = obj.optInt("middle", 0)

                val localDate = LocalDate.parse(date)

                // Base target start is 10:00 (600 mins), end is 18:00 (1080 mins)
                val defStart = 10 * 60
                val defEnd = 18 * 60

                val actualStartMins = defStart + startDebt
                val actualEndMins = defEnd - endDebt

                val startMillis = localDate.atTime(LocalTime.of((actualStartMins / 60).coerceIn(0, 23), (actualStartMins % 60).coerceIn(0, 59)))
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()

                val endMillis = localDate.atTime(LocalTime.of((actualEndMins / 60).coerceIn(0, 23), (actualEndMins % 60).coerceIn(0, 59)))
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()

                val breaks = 60 + breakDebt

                list.add(
                    WorkDay(
                        date = date,
                        actualStartMillis = startMillis,
                        actualEndMillis = endMillis,
                        defaultStartMinutes = defStart,
                        defaultEndMinutes = defEnd,
                        totalBreakMinutes = breaks.coerceAtLeast(0),
                        isNonWorkDay = false
                    )
                )
            }

            if (list.isNotEmpty()) {
                repository.deleteAllDays()
                repository.insertDays(list)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- Stats & Computations ---

    // Generates details for the last 228 work days (skipping rest days)
    fun getGridSquares(allDays: List<WorkDay>): List<GridSquare> {
        val result = mutableListOf<GridSquare>()
        val workdaysOnly = allDays.filter { day ->
            !day.checkIfNonWorkDay() && (day.actualStartMillis != null || day.actualEndMillis != null || day.totalBreakMinutes > 0)
        }.sortedBy { it.date }

        val recentWorkdays = if (workdaysOnly.size > 228) {
            workdaysOnly.takeLast(228)
        } else {
            workdaysOnly
        }

        for (day in recentWorkdays) {
            val status = if (day.getTotalDebt() <= 0) {
                SquareStatus.OVERWORKED // Light Blue
            } else {
                SquareStatus.OWES_TIME // Red
            }
            result.add(GridSquare(day.date, status, day.getTotalDebt()))
        }
        return result
    }

    fun calculateStats(allDays: List<WorkDay>): Stats {
        // Chronologically ordered list of active workdays
        val workdays = allDays.filter { !it.checkIfNonWorkDay() && (it.actualStartMillis != null || it.actualEndMillis != null || it.totalBreakMinutes > 0) }
            .sortedBy { it.date }

        if (workdays.isEmpty()) return Stats()

        // 1. Morning overwork average (actualStart - defaultStart)
        val morningDays = workdays.filter { it.actualStartMillis != null }
        val avgMorning = if (morningDays.isNotEmpty()) {
            morningDays.map { it.getStartDebt() }.average().toInt()
        } else {
            0
        }
        val recentWorkdays = if (workdays.size > 228) workdays.takeLast(228) else workdays
        val recentMorningDays = recentWorkdays.filter { it.actualStartMillis != null }
        val avgMorningRecent = if (recentMorningDays.isNotEmpty()) {
            recentMorningDays.map { it.getStartDebt() }.average().toInt()
        } else {
            0
        }
        val diffMorning = avgMorningRecent - avgMorning

        // 2. Evening overwork average (defaultEnd - actualEnd)
        val eveningDays = workdays.filter { it.actualEndMillis != null }
        val avgEvening = if (eveningDays.isNotEmpty()) {
            eveningDays.map { it.getEndDebt() }.average().toInt()
        } else {
            0
        }
        val recentEveningDays = recentWorkdays.filter { it.actualEndMillis != null }
        val avgEveningRecent = if (recentEveningDays.isNotEmpty()) {
            recentEveningDays.map { it.getEndDebt() }.average().toInt()
        } else {
            0
        }
        val diffEvening = avgEveningRecent - avgEvening

        // 3. Middle break debt average (totalBreakMinutes - 60)
        val avgMiddle = if (workdays.isNotEmpty()) {
            workdays.map { it.getBreakDebt() }.average().toInt()
        } else {
            0
        }
        val avgMiddleRecent = if (recentWorkdays.isNotEmpty()) {
            recentWorkdays.map { it.getBreakDebt() }.average().toInt()
        } else {
            0
        }
        val diffMiddle = avgMiddleRecent - avgMiddle

        // 4. Overall average daily overwork/debt
        val avgTotal = if (workdays.isNotEmpty()) {
            workdays.map { it.getTotalDebt() }.average().toInt()
        } else {
            0
        }
        val avgTotalRecent = if (recentWorkdays.isNotEmpty()) {
            recentWorkdays.map { it.getTotalDebt() }.average().toInt()
        } else {
            0
        }
        val diffTotal = avgTotalRecent - avgTotal

        // --- Streaks Calculations ---
        var consecutiveSinceDay1 = 0
        var hitFirstRed = false
        for (day in workdays) {
            val isLightBlue = day.getTotalDebt() <= 0
            if (isLightBlue) {
                if (!hitFirstRed) {
                    consecutiveSinceDay1++
                }
            } else {
                hitFirstRed = true
            }
        }

        // All-time record of light blue days
        var maxLightBlueStreak = 0
        var currentLightBlueStreak = 0

        // Start streaks (startDebt <= 0)
        var maxStartStreak = 0
        var currentStartStreak = 0

        // End streaks (endDebt <= 0)
        var maxEndStreak = 0
        var currentEndStreak = 0

        // Middle streaks (breakDebt <= 0)
        var maxMiddleStreak = 0
        var currentMiddleStreak = 0

        for (day in workdays) {
            // Light blue streak
            if (day.getTotalDebt() <= 0) {
                currentLightBlueStreak++
                if (currentLightBlueStreak > maxLightBlueStreak) {
                    maxLightBlueStreak = currentLightBlueStreak
                }
            } else {
                currentLightBlueStreak = 0
            }

            // Start streak
            if (day.getStartDebt() <= 0) {
                currentStartStreak++
                if (currentStartStreak > maxStartStreak) {
                    maxStartStreak = currentStartStreak
                }
            } else {
                currentStartStreak = 0
            }

            // End streak
            if (day.getEndDebt() <= 0) {
                currentEndStreak++
                if (currentEndStreak > maxEndStreak) {
                    maxEndStreak = currentEndStreak
                }
            } else {
                currentEndStreak = 0
            }

            // Middle streak
            if (day.getBreakDebt() <= 0) {
                currentMiddleStreak++
                if (currentMiddleStreak > maxMiddleStreak) {
                    maxMiddleStreak = currentMiddleStreak
                }
            } else {
                currentMiddleStreak = 0
            }
        }

        return Stats(
            avgMorning = avgMorning,
            avgEvening = avgEvening,
            avgMiddle = avgMiddle,
            avgTotal = avgTotal,
            lightBlueSinceDay1 = consecutiveSinceDay1,
            maxLightBlueStreak = maxLightBlueStreak,
            currentLightBlueStreak = currentLightBlueStreak,
            maxStartStreak = maxStartStreak,
            currentStartStreak = currentStartStreak,
            maxEndStreak = maxEndStreak,
            currentEndStreak = currentEndStreak,
            maxMiddleStreak = maxMiddleStreak,
            currentMiddleStreak = currentMiddleStreak,
            diffMorning = diffMorning,
            diffEvening = diffEvening,
            diffMiddle = diffMiddle,
            diffTotal = diffTotal
        )
    }
}

class WorkDayViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkDayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkDayViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class GridSquare(
    val date: String,
    val status: SquareStatus,
    val debtMinutes: Int
)

enum class SquareStatus {
    EMPTY,
    NON_WORK,
    OVERWORKED, // Light Blue
    OWES_TIME  // Red
}

data class Stats(
    val avgMorning: Int = 0,
    val avgEvening: Int = 0,
    val avgMiddle: Int = 0,
    val avgTotal: Int = 0,
    val lightBlueSinceDay1: Int = 0,
    val maxLightBlueStreak: Int = 0,
    val currentLightBlueStreak: Int = 0,
    val maxStartStreak: Int = 0,
    val currentStartStreak: Int = 0,
    val maxEndStreak: Int = 0,
    val currentStartStreakVal: Int = 0, // wait, keep to match requested
    val currentEndStreak: Int = 0,
    val maxMiddleStreak: Int = 0,
    val currentMiddleStreak: Int = 0,
    val diffMorning: Int = 0,
    val diffEvening: Int = 0,
    val diffMiddle: Int = 0,
    val diffTotal: Int = 0
)
