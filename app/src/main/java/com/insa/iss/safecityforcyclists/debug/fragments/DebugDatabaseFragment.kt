package com.insa.iss.safecityforcyclists.debug.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.reports.LocalDangerReportsViewModel

class DebugDatabaseFragment : Fragment(R.layout.database_debug) {

    private val localDangerReportsViewModel: LocalDangerReportsViewModel by activityViewModels()
    private var quickRemoveButton: FloatingActionButton? = null
    private var deleteReportFAB: FloatingActionButton? = null

    private fun setRemoveButtonState(enabled: Boolean) {
        quickRemoveButton?.isEnabled = enabled
        deleteReportFAB?.isEnabled = enabled
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        quickRemoveButton = view.findViewById(R.id.quickRemoveFAB)
        deleteReportFAB = view.findViewById(R.id.deleteReportFAB)

        localDangerReportsViewModel.getFeatures().value?.features()?.size?.let { size ->
            setRemoveButtonState(size > 0)
        }

        localDangerReportsViewModel.getFeatures().observe(viewLifecycleOwner, { featureCollection ->
            featureCollection?.features()?.size?.let { size ->
                setRemoveButtonState(size > 0)
            }
        })

        // Add dummy report
        view.findViewById<FloatingActionButton>(R.id.quickAddFAB).setOnClickListener {
            var size = localDangerReportsViewModel.getFeatures().value?.features()?.size
            if (size == null) {
                size = 0
            }
            localDangerReportsViewModel.addReports(
                listOf(
                    LocalReport(
                        timestamp = size + 1,
                        distance = (size + 2).toDouble(),
                        objectSpeed = (size + 3).toDouble(),
                        bicycleSpeed =(size + 4).toDouble(),
                        latitude = 43.6020 + (size).toDouble() * 0.01,
                        longitude = 1.4530 + (size).toDouble() * 0.01,
                        sync = false
                    )
                )
            )
        }

        // Remove last report
        quickRemoveButton?.setOnClickListener {
            localDangerReportsViewModel.getFeatures().value?.features()?.let { features ->
                if (features.size > 0) {
                    features.last().properties()?.get("id")?.asInt?.let { id ->
                        localDangerReportsViewModel.deleteReportsById(listOf(id))
                    }
                }
            }
        }

        view.findViewById<FloatingActionButton>(R.id.addReportFAB).setOnClickListener {
            val dialog = DeubgAddReportDialogFragment()
            dialog.show(parentFragmentManager, "Dialog")
        }

        deleteReportFAB?.setOnClickListener {
            val dialog = DebugDeleteReportDialogFragment()
            dialog.show(parentFragmentManager, "Dialog")
        }

        view.findViewById<FloatingActionButton>(R.id.viewReportFAB).setOnClickListener {
            val result = viewRecord()
            val builder = AlertDialog.Builder(view.context)
            builder.setTitle("View reports")
            builder.setMessage(result)
            builder.show()
        }

        view.findViewById<FloatingActionButton>(R.id.syncAllReportsFAB).setOnClickListener {
            localDangerReportsViewModel.syncReportsById((0..500).toList())
        }

        view.findViewById<FloatingActionButton>(R.id.unsyncAllReportsFAB).setOnClickListener {
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