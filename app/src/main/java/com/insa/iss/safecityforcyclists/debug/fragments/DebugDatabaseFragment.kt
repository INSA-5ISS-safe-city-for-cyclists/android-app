package com.insa.iss.safecityforcyclists.debug.fragments

import android.os.Bundle
import android.view.View
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.database.LocalReport
import com.insa.iss.safecityforcyclists.reports.DangerReportsViewModel
import com.insa.iss.safecityforcyclists.reports.DangerZonesViewModel
import com.mapbox.mapboxsdk.maps.MapboxMap
import java.util.*

class DebugDatabaseFragment : Fragment(R.layout.database_debug) {

    private val dangerZonesViewModel: DangerZonesViewModel by activityViewModels()
    private val dangerReportsViewModel: DangerReportsViewModel by activityViewModels()
    private var container: View? = null
    private var quickRemoveButton: FloatingActionButton? = null
    private var deleteReportFAB: FloatingActionButton? = null

    var mapboxMap: MapboxMap? = null
    var debugEnabled: Boolean = false
        set(value) {
            field = value
            setViewVisible(value)
        }

    private fun setViewVisible(visible: Boolean) {
        container?.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }

    }

    private fun setRemoveButtonState(enabled: Boolean) {
        quickRemoveButton?.isEnabled = enabled
        deleteReportFAB?.isEnabled = enabled
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        container = view.findViewById(R.id.debugContainer)
        quickRemoveButton = view.findViewById(R.id.quickRemoveFAB)
        deleteReportFAB = view.findViewById(R.id.deleteReportFAB)
        setViewVisible(debugEnabled)

        dangerReportsViewModel.getFeatures().value?.features()?.size?.let { size ->
            setRemoveButtonState(size > 0)
        }

        dangerReportsViewModel.getFeatures().observe(viewLifecycleOwner, { featureCollection ->
            featureCollection?.features()?.size?.let { size ->
                setRemoveButtonState(size > 0)
            }
        })

        // Add dummy report in the center of the screen
        view.findViewById<FloatingActionButton>(R.id.quickAddFAB).setOnClickListener {
            mapboxMap?.let {
                dangerReportsViewModel.addLocalReports(
                    listOf(
                        LocalReport(
                            timestamp = Date().time / 1000,
                            distance = 50.0, // cm
                            objectSpeed = 50.0, // km/h
                            bicycleSpeed = 20.0, // km/h
                            latitude = it.cameraPosition.target.latitude,
                            longitude = it.cameraPosition.target.longitude,
                            sync = false
                        )
                    )
                )
            }
        }

        // Remove last report
        quickRemoveButton?.setOnClickListener {
            dangerReportsViewModel.getFeatures().value?.features()?.let { features ->
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

        // Show only dangerous zones button

        val toggleDangerousZones: ToggleButton = view.findViewById(R.id.toggleButton)
        toggleDangerousZones.setOnCheckedChangeListener { _, isChecked ->
            dangerZonesViewModel.onlyDangerousZones = isChecked
            dangerZonesViewModel.initData()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    //method for read records from database in ListView
    private fun viewRecord(): String {
        val localReports = dangerReportsViewModel.getFeatures().value
        var result = ""
        localReports?.features()?.iterator()?.forEach { feature ->
            result += "*${feature.properties()}\n"
        }
        println(result)
        return result
    }
}