package com.insa.iss.safecityforcyclists.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class DangerZonesViewModel(application: Application) : AndroidViewModel(application) {
    private val features = MutableLiveData<FeatureCollection?>()


    fun getFeatures(): LiveData<FeatureCollection?> {
        return features
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeRequest(): FeatureCollection {
        return withContext(Dispatchers.IO) {
            return@withContext FeatureCollection.fromJson(URL("https://maplibre.org/maplibre-gl-js-docs/assets/earthquakes.geojson").readText())
        }
    }

    fun initData() {
        viewModelScope.launch {
            features.value = makeRequest()
        }
    }
}