package com.insa.iss.safecityforcyclists.debug.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.reports.DangerReportsViewModel

class DebugAddReportDialogFragment : DialogFragment() {

    private val dangerReportsViewModel: DangerReportsViewModel by activityViewModels()

    lateinit var customView: View

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            customView = inflater.inflate(R.layout.dialog_add_debug_report, null)
            builder.setView(customView)
                // Add action buttons
                .setPositiveButton(
                    "Add"
                ) { _, _ ->
                    saveReport()
                }
                .setNegativeButton(
                    "Cancel"
                ) { _, _ ->
                    dialog?.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun saveReport() {
        val timestamp =
            (customView.findViewById(R.id.timestamp) as EditText).text.toString().toLongOrNull() ?: 0
        val distance =
            (customView.findViewById(R.id.distance) as EditText).text.toString().toDoubleOrNull() ?: 0.0
        val objectSpeed =
            (customView.findViewById(R.id.object_speed) as EditText).text.toString().toDoubleOrNull() ?: 0.0
        val bicycleSpeed =
            (customView.findViewById(R.id.bicycle_speed) as EditText).text.toString().toDoubleOrNull() ?: 0.0
        val latitude =
            (customView.findViewById(R.id.latitude) as EditText).text.toString().toDoubleOrNull() ?: 0.0
        val longitude =
            (customView.findViewById(R.id.longitude) as EditText).text.toString().toDoubleOrNull() ?: 0.0
        val sync = (customView.findViewById(R.id.sync) as CheckBox).isChecked

        dangerReportsViewModel.addLocalReports(
            listOf(
                LocalReport(
                    timestamp = timestamp,
                    distance = distance,
                    objectSpeed = objectSpeed,
                    bicycleSpeed = bicycleSpeed,
                    latitude = latitude,
                    longitude = longitude,
                    sync = sync
                )
            )
        )
    }
}