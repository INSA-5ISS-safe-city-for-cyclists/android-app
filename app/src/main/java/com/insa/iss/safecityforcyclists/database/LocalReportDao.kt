package com.insa.iss.safecityforcyclists.database

import androidx.room.*

// See doc here: https://developer.android.com/training/data-storage/room/accessing-data#kotlin

@Dao
interface LocalReportDao {
    @Query("SELECT * FROM local_reports")
    fun getReports(): List<LocalReport>

    @Query("SELECT * FROM local_reports WHERE sync = 0 AND (distance <= :maxDistance0 OR (distance <= :maxDistance1 AND (object_speed - bicycle_speed) > :minSpeed0_1) OR (distance <= :maxDistance2 AND (object_speed - bicycle_speed) > :minSpeed1_2))")
    fun getUnsyncedReports(
        minSpeed0_1: Double,
        minSpeed1_2: Double,
        maxDistance0: Double,
        maxDistance1: Double,
        maxDistance2: Double
    ): List<LocalReport>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReports(reports: List<LocalReport>)

    @Update
    fun updateReports(reports: List<LocalReport>)

    @Delete
    fun deleteReports(reports: List<LocalReport>)

    @Query("DELETE FROM local_reports WHERE id IN (:ids)")
    fun deleteReportsById(ids: List<Int>)

    @Query("UPDATE local_reports SET sync = 1 WHERE id IN (:ids)")
    fun syncReportsById(ids: List<Int>)

    @Query("UPDATE local_reports SET sync = 0 WHERE id IN (:ids)")
    fun unsyncReportsById(ids: List<Int>)
}