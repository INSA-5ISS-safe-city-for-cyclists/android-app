package com.insa.iss.safecityforcyclists.reports

import android.app.Application
import androidx.room.Room
import com.insa.iss.safecityforcyclists.database.LocalReportDatabase
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection

class LocalDangerReportsViewModel(application: Application) : DangerReportsViewModel(application) {
    private var db: LocalReportDatabase? = null

    init {
        db = Room.databaseBuilder(
            getApplication(),
            LocalReportDatabase::class.java, "database-name"
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
        val localReports = db?.localReportDao()?.getReports()
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
                        "\"coordinates\": [ ${report.latitude}, ${report.longitude}, 0.0 ] " +
                        "} }")
                featureList.add(f)
            }
        }
        features.value = FeatureCollection.fromFeatures(featureList)
    }
}