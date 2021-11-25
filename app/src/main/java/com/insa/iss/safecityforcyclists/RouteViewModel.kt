package com.insa.iss.safecityforcyclists

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.FeatureCollection

class RouteViewModel: ViewModel() {
    var routeGeoJson = MutableLiveData<FeatureCollection?>()
}