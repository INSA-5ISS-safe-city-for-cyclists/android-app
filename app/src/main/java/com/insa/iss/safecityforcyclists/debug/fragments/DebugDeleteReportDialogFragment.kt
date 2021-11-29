package com.insa.iss.safecityforcyclists.debug.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.reports.LocalDangerReportsViewModel

class DebugDeleteReportDialogFragment : DialogFragment() {

    lateinit var customView: View
    private val localDangerReportsViewModel: LocalDangerReportsViewModel by activityViewModels()

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            customView = inflater.inflate(R.layout.dialog_delete_debug_report, null)
            builder.setView(customView)
                // Add action buttons
                .setPositiveButton(
                    "Delete"
                ) { _, _ ->
                    deleteReport()
                }
                .setNegativeButton(
                    "Cancel"
                ) { _, _ ->
                    getDialog()?.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun deleteReport() {
        val from_id =
            (customView.findViewById(R.id.from_id) as EditText).text.toString().toIntOrNull() ?: 0
        val to_id =
            (customView.findViewById(R.id.to_id) as EditText).text.toString().toIntOrNull() ?: 0
        localDangerReportsViewModel.deleteReportsById((from_id..to_id).toList())
    }
}