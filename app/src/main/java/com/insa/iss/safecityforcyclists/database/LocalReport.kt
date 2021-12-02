package com.insa.iss.safecityforcyclists.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// See doc here: https://developer.android.com/training/data-storage/room/defining-data#kotlin

@Entity(tableName = "local_reports")
data class LocalReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "distance") val distance: Double,
    @ColumnInfo(name = "object_speed") val objectSpeed: Double,
    @ColumnInfo(name = "bicycle_speed") val bicycleSpeed: Double,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "sync") val sync: Boolean,
)