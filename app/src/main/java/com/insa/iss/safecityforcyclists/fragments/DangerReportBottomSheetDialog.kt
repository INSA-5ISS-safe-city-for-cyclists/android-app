package com.insa.iss.safecityforcyclists.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.insa.iss.safecityforcyclists.R
import com.mapbox.geojson.Feature
import java.util.*

class DangerReportBottomSheetDialog(
    private val feature: Feature,
    private val onDismiss: () -> Unit
) : BottomSheetDialogFragment() {

    private var title: TextView? = null
    private var subtitle: TextView? = null
    private var deleteButton: Button? = null


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

    private fun buildText(key: String, text: String, unit: String): String {
        feature.properties()?.let { p ->
            p.get(key)?.asFloat?.let {
                var value = String.format(" %.2f", it)
                val stringSplit = value.split(".")
                if (stringSplit[1] == "00") {
                    value = stringSplit[0]
                }
                return "$text: $value $unit\n"
            }
        }
        return ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = view.findViewById(R.id.title)
        subtitle = view.findViewById(R.id.subtitle)
        deleteButton = view.findViewById(R.id.deleteButton)

        feature.properties()?.let { p ->
            p.get("timestamp")?.asLong?.let {
                val date = Date()
                date.time = it
                title?.text = DateFormat.getLongDateFormat(requireContext()).format(date) + ", " + DateFormat.getTimeFormat(requireContext()).format(date)
            }
            var subtitleText = buildText("bicycle_speed", "Bicycle speed", "km/h")
            subtitleText += buildText("object_speed", "Object speed", "km/h")
            subtitleText += buildText("distance", "Distance", "m")
            subtitle?.text = subtitleText
            p.get("sync")?.asBoolean?.let {
                if (!it) {
                    deleteButton?.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss()
    }
}