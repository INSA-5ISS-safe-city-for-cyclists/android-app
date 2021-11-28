package com.insa.iss.safecityforcyclists.routing

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.FeatureCollection

class RouteViewModel: ViewModel() {
    var routeGeoJson = MutableLiveData<FeatureCollection?>()
}