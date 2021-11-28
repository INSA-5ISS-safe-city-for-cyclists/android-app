package com.insa.iss.safecityforcyclists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection

abstract class DangerReportsViewModel: ViewModel() {
    protected val features = MutableLiveData<FeatureCollection?>()

    fun getFeatures(): LiveData<FeatureCollection?> {
        return features
    }

    fun addFeature(f: Feature) {
        features.value?.features()?.add(f)
    }

    fun removeFeature(f: Feature) {
        features.value?.features()?.remove(f)
    }

    abstract fun initData()
}