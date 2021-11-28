package com.insa.iss.safecityforcyclists.database

import androidx.room.*

// See doc here: https://developer.android.com/training/data-storage/room/accessing-data#kotlin

@Dao
interface LocalReportDao {
    @Query("SELECT * FROM local_reports")
    fun getReports(): List<LocalReport>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReports(reports: List<LocalReport>)

    @Update
    fun updateReports(reports: List<LocalReport>)

    @Delete
    fun deleteReports(reports: List<LocalReport>)
}