package com.insa.iss.safecityforcyclists.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocalReport::class], version = 1)
abstract class LocalReportDatabase: RoomDatabase() {
    abstract fun localReportDao(): LocalReportDao
}