package com.insa.iss.safecityforcyclists.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.database.LocalReportDatabase
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class DangerReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val features = MutableLiveData<FeatureCollection?>()
    private val dangerClassification = MutableLiveData<DangerClassification?>()
    private var db: LocalReportDatabase? = null

    // TODO remove earthquakes and this variable
    private val useDangerReportsRemoteServer =
        getApplication<Application>().resources.getBoolean(R.bool.useDangerReportsRemoteServer)

    init {
        db = Room.databaseBuilder(
            getApplication(),
            LocalReportDatabase::class.java, "safe-city-for-cyclists"
        ).build()
    }

    fun getFeatures(): LiveData<FeatureCollection?> {
        return features
    }

    fun getDangerClassification(): LiveData<DangerClassification?> {
        return dangerClassification
    }

    fun getLocalFeaturesAsJson(pretty: Boolean = false): String {
        val gson: Gson = if (pretty) {
            GsonBuilder().setPrettyPrinting().create()
        } else {
            Gson()
        }
        return gson.toJson(features.value)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeClassificationRequest(): DangerClassification {
        return withContext(Dispatchers.IO) {
            val url: String =
                getApplication<Application>().resources.getString(R.string.server_uri) + "criteria"
            return@withContext DangerClassification(JSONObject(URL(url).readText()))
        }
    }

    fun initData() {
        viewModelScope.launch {
            // TODO use danger classification
            // Danger classification
            if (useDangerReportsRemoteServer) {
                dangerClassification.value = makeClassificationRequest()
            } else {
                dangerClassification.value = DangerClassification(JSONObject())
            }

            // Local Data
            val localReports = getUnsyncedReports()
            val featureList = ArrayList<Feature>()
            if (localReports != null) {
                for (report in localReports) {
                    val f = Feature.fromJson(
                        "{ " +
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
                                "} }"
                    )
                    featureList.add(f)
                }
            }
            features.value = FeatureCollection.fromFeatures(featureList)
        }
    }

//    private suspend fun getReports(): List<LocalReport>? {
//        return withContext(Dispatchers.IO) {
//            return@withContext db?.localReportDao()?.getReports()
//        }
//    }

    private suspend fun getUnsyncedReports(): List<LocalReport>? {
        return withContext(Dispatchers.IO) {
            return@withContext db?.localReportDao()?.getUnsyncedReports()
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

    fun syncLocalReportsById(ids: List<Int>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.syncReportsById(ids)!!
                return@withContext initData()
            }
        }
    }

    fun unsyncLocalReportsById(ids: List<Int>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db?.localReportDao()?.unsyncReportsById(ids)!!
                return@withContext initData()
            }
        }
    }
}