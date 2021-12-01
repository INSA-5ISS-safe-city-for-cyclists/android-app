package com.insa.iss.safecityforcyclists.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.insa.iss.safecityforcyclists.R
import com.mapbox.geojson.Feature

class DangerReportBottomSheetDialog(private val feature: Feature, private val onDismiss: () -> Unit) : BottomSheetDialogFragment() {

    private var title: TextView? = null
    private var subtitle: TextView? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.bottom_sheet_layout,
            container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = view.findViewById(R.id.title) as TextView
        subtitle = view.findViewById(R.id.subtitle) as TextView
        title?.text = feature.properties()?.get("id")?.asString
        subtitle?.text = "$feature"
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss()
    }
}