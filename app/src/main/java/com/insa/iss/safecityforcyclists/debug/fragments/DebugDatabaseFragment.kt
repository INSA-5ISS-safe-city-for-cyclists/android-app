package com.insa.iss.safecityforcyclists.debug.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.reports.DangerReportsViewModel
import java.util.*

class DebugDatabaseFragment : Fragment(R.layout.database_debug) {

    private val dangerReportsViewModel: DangerReportsViewModel by activityViewModels()
    private var quickRemoveButton: FloatingActionButton? = null
    private var deleteReportFAB: FloatingActionButton? = null

    private fun setRemoveButtonState(enabled: Boolean) {
        quickRemoveButton?.isEnabled = enabled
        deleteReportFAB?.isEnabled = enabled
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        quickRemoveButton = view.findViewById(R.id.quickRemoveFAB)
        deleteReportFAB = view.findViewById(R.id.deleteReportFAB)

        dangerReportsViewModel.getLocalFeatures().value?.features()?.size?.let { size ->
            setRemoveButtonState(size > 0)
        }

        dangerReportsViewModel.getLocalFeatures().observe(viewLifecycleOwner, { featureCollection ->
            featureCollection?.features()?.size?.let { size ->
                setRemoveButtonState(size > 0)
            }
        })

        // Add dummy report
        view.findViewById<FloatingActionButton>(R.id.quickAddFAB).setOnClickListener {
            var size = dangerReportsViewModel.getLocalFeatures().value?.features()?.size
            if (size == null) {
                size = 0
            }
            dangerReportsViewModel.addLocalReports(
                listOf(
                    LocalReport(
                        timestamp = Date().time,
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
            dangerReportsViewModel.getLocalFeatures().value?.features()?.let { features ->
                if (features.size > 0) {
                    features.last().properties()?.get("id")?.asInt?.let { id ->
                        dangerReportsViewModel.deleteLocalReportsById(listOf(id))
                    }
                }
            }
        }

        view.findViewById<FloatingActionButton>(R.id.addReportFAB).setOnClickListener {
            val dialog = DebugAddReportDialogFragment()
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
            dangerReportsViewModel.syncLocalReportsById((0..500).toList())
        }

        view.findViewById<FloatingActionButton>(R.id.unsyncAllReportsFAB).setOnClickListener {
            dangerReportsViewModel.unsyncLocalReportsById((0..500).toList())
        }

        super.onViewCreated(view, savedInstanceState)
    }

    //method for read records from database in ListView
    private fun viewRecord(): String {
        val localReports = dangerReportsViewModel.getLocalFeatures().value
        var result = ""
        localReports?.features()?.iterator()?.forEach { feature ->
            result += "*${feature.properties()}\n"
        }
        println(result)
        return result
    }
}