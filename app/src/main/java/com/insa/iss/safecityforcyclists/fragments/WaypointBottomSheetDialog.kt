package com.insa.iss.safecityforcyclists.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.insa.iss.safecityforcyclists.R
import com.mapbox.geojson.Feature

class WaypointBottomSheetDialog(private val feature: Feature, private val onRouteFromClicked: () -> Unit, private val onRouteToClicked: () -> Unit, private val onDismiss: () -> Unit) : BottomSheetDialogFragment() {
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
            onRouteFromClicked()
            dismiss()
        }
        toButton?.setOnClickListener {
            onRouteToClicked()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss()
    }
}