package com.insa.iss.safecityforcyclists

import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class RemoteDangerReportsViewModel : DangerReportsViewModel() {

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeRequest(): FeatureCollection {
        return withContext(Dispatchers.IO) {
            return@withContext FeatureCollection.fromJson(URL("https://maplibre.org/maplibre-gl-js-docs/assets/earthquakes.geojson").readText())
        }
    }

    override fun initData() {
        viewModelScope.launch {
            features.value = makeRequest()
        }
    }
}