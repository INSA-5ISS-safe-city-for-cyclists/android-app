package com.insa.iss.safecityforcyclists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mapbox.geojson.Feature

class WaypointBottomSheetDialog(private val feature: Feature, private val onRouteFromClicked: (feature: Feature) -> Unit, private val onRouteToClicked: (feature: Feature) -> Unit) : BottomSheetDialogFragment() {
    private var title: TextView? = null
    private var fromButton: Button? = null
    private var toButton: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.waypoint_bottom_sheet_layout,
            container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = view.findViewById(R.id.title) as TextView
        fromButton = view.findViewById(R.id.fromButton) as Button
        toButton = view.findViewById(R.id.toButton) as Button
        title?.text = feature.properties()?.get("name")?.asString

        fromButton?.setOnClickListener {
            onRouteFromClicked(feature)
            dismiss()
        }
        toButton?.setOnClickListener {
            onRouteToClicked(feature)
            dismiss()
        }
    }
}