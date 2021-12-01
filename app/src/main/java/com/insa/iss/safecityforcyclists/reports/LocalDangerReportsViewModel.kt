package com.insa.iss.safecityforcyclists.reports

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.database.LocalReportDatabase
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalDangerReportsViewModel(application: Application) : DangerReportsViewModel(application) {
    private var db: LocalReportDatabase? = null

    init {
        db = Room.databaseBuilder(
            getApplication(),
            LocalReportDatabase::class.java, "safe-city-for-cyclists"
        ).build()
    }

    override fun addFeature(f: Feature) {
        super.addFeature(f)
//        db?.localReportDao()?.insertReports()
    }


    override fun removeFeature(f: Feature) {
        super.removeFeature(f)
//        db?.localReportDao()?.deleteReports()
    }

    override fun initData() {
        viewModelScope.launch {
            val localReports = getReports()
            val featureList = ArrayList<Feature>()
            if (localReports != null) {
                for (report in localReports) {
                    val f = Feature.fromJson("{ " +
                            "\"type\": \"Feature\", " +
                            "\"properties\": { " +
                            "\"id\": ${report.id}, " +
                            "\"bicycle_speed\": ${report.bicycleSpeed}, " +
                            "\"object_speed\": ${report.objectSpeed}, " +
                            "\"distance\": ${report.distance}, " +
                            "\"sync\": ${report.sync}, " +
                            "\"timestamp\": ${report.timestamp} " +
                            "}, " +
                            "\"geometry\": { " +
                            "\"type\": \"Point\", " +
                            "\"coordinates\": [ ${report.longitude}, ${report.latitude}, 0.0 ] " +
                            "} }")
                    featureList.add(f)
                }
            }
            features.value = FeatureCollection.fromFeatures(featureList)
        }
    }

    private suspend fun getReports(): List<LocalReport>? {
        return withContext(Dispatchers.IO) {
            return@withContext db?.localReportDao()?.getReports()
        }
    }

    fun addReports(reports: List<LocalReport>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.insertReports(reports)!!
                return@withContext initData()
            }
        }
    }

    fun deleteReportsById(ids: List<Int>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.deleteReportsById(ids)!!
                return@withContext initData()
            }
        }
    }

    fun syncReportsById(ids: List<Int>){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.syncReportsById(ids)!!
                return@withContext initData()
            }
        }
    }

    fun unsyncReportsById(ids: List<Int>){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.unsyncReportsById(ids)!!
                return@withContext initData()
            }
        }
    }
}