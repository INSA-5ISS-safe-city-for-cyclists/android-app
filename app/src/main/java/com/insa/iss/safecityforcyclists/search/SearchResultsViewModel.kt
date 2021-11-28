package com.insa.iss.safecityforcyclists.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Feature

class SearchResultsViewModel: ViewModel() {

    var dataSet = MutableLiveData<List<Feature>>()
    var selected = MutableLiveData<Int?>()

}