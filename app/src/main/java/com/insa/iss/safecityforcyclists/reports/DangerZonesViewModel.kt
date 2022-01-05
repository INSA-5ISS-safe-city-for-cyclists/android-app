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
    var selectedTime: String? = null
        set(value) {
            field = value
            initData()
        }

    private val useDangerReportsRemoteServer =
        getApplication<Application>().resources.getBoolean(R.bool.useDangerReportsRemoteServer)

    fun getFeatures(): LiveData<FeatureCollection?> {
        return features
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeRequest(): FeatureCollection {
        return withContext(Dispatchers.IO) {
            var timeArg = ""
            selectedTime?.let {
                val hours = it.split(":")[0]
                val minutes = it.split(":")[1]
                timeArg = "&hours=$hours&minutes=$minutes"
            }
            val url = if (useDangerReportsRemoteServer) {
                if (onlyDangerousZones) {
                    Constants.API_ZONES_ENDPOINT + "?dangerous=true$timeArg"
                } else {
                    // TODO Remove this line used to show all zone (time_filter = false)
                    Constants.API_ZONES_ENDPOINT + "?time_filter=false$timeArg"
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