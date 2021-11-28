package com.insa.iss.safecityforcyclists.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection

abstract class DangerReportsViewModel(application: Application) : AndroidViewModel(application) {
    protected val features = MutableLiveData<FeatureCollection?>()

    fun getFeatures(): LiveData<FeatureCollection?> {
        return features
    }

    open fun addFeature(f: Feature) {
        features.value?.features()?.add(f)
    }

    open fun removeFeature(f: Feature) {
        features.value?.features()?.remove(f)
    }

    abstract fun initData()
}