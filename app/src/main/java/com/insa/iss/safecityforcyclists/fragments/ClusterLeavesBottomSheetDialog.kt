package com.insa.iss.safecityforcyclists.fragments

import com.mapbox.geojson.Feature


class ClusterLeavesBottomSheetDialog(private val features: List<Feature>, onItemPressedCallback: (bottomSheet: FeatureListBottomSheetDialog, item: Feature, position: Int) -> Unit) :
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
        uploadListAdapter.dataSet = features
    }
}