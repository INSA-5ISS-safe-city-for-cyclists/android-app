package com.insa.iss.safecityforcyclists.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.insa.iss.safecityforcyclists.Constants
import com.insa.iss.safecityforcyclists.R
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class DangerZonesViewModel(application: Application) : AndroidViewModel(application) {
    private val features = MutableLiveData<FeatureCollection?>()

    var onlyDangerousZones = true

    private val useDangerReportsRemoteServer =
        getApplication<Application>().resources.getBoolean(R.bool.useDangerReportsRemoteServer)

    fun getFeatures(): LiveData<FeatureCollection?> {
        return features
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeRequest(): FeatureCollection {
        return withContext(Dispatchers.IO) {
            val url = if (useDangerReportsRemoteServer) {
                if (onlyDangerousZones) {
                    Constants.API_ZONES_ENDPOINT + "?dangerous=true"
                } else {
                    // TODO Remove this line used to show all zone (time_filter = false)
                    Constants.API_ZONES_ENDPOINT + "?time_filter=false"
                }
            } else {
                "https://maplibre.org/maplibre-gl-js-docs/assets/earthquakes.geojson"
            }
            println(url)
            try {
                return@withContext FeatureCollection.fromJson(URL(url).readText())
            } catch (e: Exception) {
                println("Could not connect to $url")
                return@withContext FeatureCollection.fromFeatures(ArrayList<Feature>())
            }
        }
    }

    fun initData() {
        viewModelScope.launch {
            features.value = makeRequest()
        }
    }
}