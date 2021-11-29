package com.insa.iss.safecityforcyclists.debug.fragments

import androidx.fragment.app.Fragment
import com.insa.iss.safecityforcyclists.R

import android.view.View
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.insa.iss.safecityforcyclists.reports.LocalDangerReportsViewModel

class DebugDatabaseFragment : Fragment(R.layout.database_debug) {

    private val localDangerReportsViewModel: LocalDangerReportsViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val addReportFab: View = view.findViewById(R.id.addReportFAB)
        addReportFab.setOnClickListener { _ ->
            val dialog = DeubgAddReportDialogFragment()
            dialog.show(parentFragmentManager, "Dialog")
        }

        val viewReportFab: View = view.findViewById(R.id.viewReportFAB)
        viewReportFab.setOnClickListener { _ ->
            val result = viewRecord()
            val builder = AlertDialog.Builder(view.context)
            builder.setTitle("View reports")
            builder.setMessage(result)
            builder.show()
        }

        val deleteReportFab: View = view.findViewById(R.id.deleteReportFAB)
        deleteReportFab.setOnClickListener { _ ->
            val dialog = DebugDeleteReportDialogFragment()
            dialog.show(parentFragmentManager, "Dialog")
        }

        val syncAllReportsFab: View = view.findViewById(R.id.syncAllReportsFAB)
        syncAllReportsFab.setOnClickListener { _ ->
            localDangerReportsViewModel.syncReportsById((0..500).toList())
        }

        val unsyncAllReportsFab: View = view.findViewById(R.id.unsyncAllReportsFAB)
        unsyncAllReportsFab.setOnClickListener { _ ->
            localDangerReportsViewModel.unsyncReportsById((0..500).toList())
        }

        super.onViewCreated(view, savedInstanceState)
    }

    //method for read records from database in ListView
    private fun viewRecord(): String {
        val localReports = localDangerReportsViewModel.getFeatures().value
        var result = ""
        localReports?.features()?.iterator()?.forEach { feature ->
            result += "*${feature.properties()}\n"
        }
        println(result)
        return result
    }
}