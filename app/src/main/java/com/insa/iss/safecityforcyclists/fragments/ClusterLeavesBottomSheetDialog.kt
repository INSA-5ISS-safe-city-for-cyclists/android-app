package com.insa.iss.safecityforcyclists.fragments

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection


class ClusterLeavesBottomSheetDialog(private val features: FeatureCollection, onItemPressedCallback: (bottomSheet: FeatureListBottomSheetDialog, item: Feature, position: Int) -> Unit) :
    FeatureListBottomSheetDialog(onItemPressedCallback) {

    override fun getTitle(): String {
        return "Cluster Leaves"
    }

    override fun onActionButtonClicked() {
        dismiss()
    }

    override fun getActionButtonText(): String {
        return ""
    }

    override fun updateDataset() {
        features.features()?.let {
            uploadListAdapter.dataSet = it
        }
    }
}