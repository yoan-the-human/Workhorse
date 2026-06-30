package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDayDao {
    @Query("SELECT * FROM work_days ORDER BY date ASC")
    fun getAllDaysAsc(): Flow<List<WorkDay>>

    @Query("SELECT * FROM work_days ORDER BY date DESC")
    fun getAllDaysDesc(): Flow<List<WorkDay>>

    @Query("SELECT * FROM work_days WHERE date = :date LIMIT 1")
    suspend fun getDay(date: String): WorkDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: WorkDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<WorkDay>)

    @Query("DELETE FROM work_days")
    suspend fun deleteAllDays()

    @Delete
    suspend fun deleteDay(day: WorkDay)
}
