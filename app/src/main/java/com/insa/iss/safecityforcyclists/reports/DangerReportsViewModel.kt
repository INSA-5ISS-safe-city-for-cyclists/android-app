package com.insa.iss.safecityforcyclists.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.database.LocalReportDatabase
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class DangerReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val remoteFeatures = MutableLiveData<FeatureCollection?>()
    private val localFeatures = MutableLiveData<FeatureCollection?>()
    private var db: LocalReportDatabase? = null

    init {
        db = Room.databaseBuilder(
            getApplication(),
            LocalReportDatabase::class.java, "safe-city-for-cyclists"
        ).build()
    }

    fun getRemoteFeatures(): LiveData<FeatureCollection?> {
        return remoteFeatures
    }

    fun getLocalFeatures():  LiveData<FeatureCollection?> {
        return localFeatures
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeRequest(): FeatureCollection {
        return withContext(Dispatchers.IO) {
            return@withContext FeatureCollection.fromJson(URL("https://maplibre.org/maplibre-gl-js-docs/assets/earthquakes.geojson").readText())
        }
    }

    fun initData() {
        viewModelScope.launch {
            // Remote Data
            remoteFeatures.value = makeRequest()
            // Local Data
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
            localFeatures.value = FeatureCollection.fromFeatures(featureList)
        }
    }

    private suspend fun getReports(): List<LocalReport>? {
        return withContext(Dispatchers.IO) {
            return@withContext db?.localReportDao()?.getReports()
        }
    }

    fun addLocalReports(reports: List<LocalReport>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.insertReports(reports)!!
                return@withContext initData()
            }
        }
    }

    fun deleteLocalReportsById(ids: List<Int>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.deleteReportsById(ids)!!
                return@withContext initData()
            }
        }
    }

    fun syncLocalReportsById(ids: List<Int>){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.syncReportsById(ids)!!
                return@withContext initData()
            }
        }
    }

    fun unsyncLocalReportsById(ids: List<Int>){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.unsyncReportsById(ids)!!
                return@withContext initData()
            }
        }
    }
}