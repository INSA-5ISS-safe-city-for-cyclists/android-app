package com.insa.iss.safecityforcyclists.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.database.LocalReportDatabase
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class DangerZonesViewModel(application: Application) : AndroidViewModel(application) {
    private val features = MutableLiveData<FeatureCollection?>()

    private val useDangerReportsRemoteServer =
        getApplication<Application>().resources.getBoolean(R.bool.useDangerReportsRemoteServer)

    fun getFeatures(): LiveData<FeatureCollection?> {
        return features
    }
    private var db: LocalReportDatabase? = null
    init {
        db = Room.databaseBuilder(
            getApplication(),
            LocalReportDatabase::class.java, "safe-city-for-cyclists"
        ).build()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeRequest(): FeatureCollection {
        return withContext(Dispatchers.IO) {
            val url = if (useDangerReportsRemoteServer) {
                getApplication<Application>().resources.getString(R.string.server_uri) + "reports/geojson?dangerous=true"
            } else {
                "https://maplibre.org/maplibre-gl-js-docs/assets/earthquakes.geojson"
            }
            return@withContext FeatureCollection.fromJson(URL(url).readText())
        }
    }

    fun initData() {
        viewModelScope.launch {
            features.value = makeRequest()
        }
    }
}