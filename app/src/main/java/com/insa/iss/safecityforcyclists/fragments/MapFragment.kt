package com.insa.iss.safecityforcyclists.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.*
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.insa.iss.safecityforcyclists.MainActivity
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.bluetooth.BluetoothHandler
import com.insa.iss.safecityforcyclists.debug.fragments.DebugDatabaseFragment
import com.insa.iss.safecityforcyclists.location.Location
import com.insa.iss.safecityforcyclists.reports.DangerReports
import com.insa.iss.safecityforcyclists.reports.DangerReportsViewModel
import com.insa.iss.safecityforcyclists.reports.DangerZones
import com.insa.iss.safecityforcyclists.reports.DangerZonesViewModel
import com.insa.iss.safecityforcyclists.routing.RouteViewModel
import com.insa.iss.safecityforcyclists.routing.Routing
import com.insa.iss.safecityforcyclists.search.SearchResultsViewModel
import com.insa.iss.safecityforcyclists.selection.PinSelector
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.utils.BitmapUtils
import java.util.*


class MapFragment : Fragment(R.layout.map_fragment) {
    companion object {
        const val REMOTE_REPORTS_ID = "REMOTE_REPORTS"
        const val LOCAL_REPORTS_ID = "LOCAL_REPORTS"
        const val LOCAL_REPORTS_UNSYNC_ID = "LOCAL_REPORTS_UNSYNC"
    }

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private var routing: Routing? = null
    private var dangerReports: DangerReports? = null
    private var dangerZones: DangerZones? = null
    private var location: Location? = null
    private var pinSelector: PinSelector? = null
    private val searchResultsViewModel: SearchResultsViewModel by activityViewModels()
    private val routeViewModel: RouteViewModel by activityViewModels()
    private val dangerReportsViewModel: DangerReportsViewModel by activityViewModels()
    private val dangerZonesViewModel: DangerZonesViewModel by activityViewModels()
    private var uploadFAB: FloatingActionButton? = null
    private var debugDatabaseFragment: DebugDatabaseFragment? = null
    private var toggleDebugButton: Button? = null

    private lateinit var bluetoothHandler: BluetoothHandler
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    private fun makeCustomGeoapifyStyle(): Style.Builder {
        val builder = Style.Builder()
        val inputStream = resources.openRawResource(R.raw.osm_bright_cycleways)
        val jsonString: String = Scanner(inputStream).useDelimiter("\\A").next()
        return builder.fromJson(jsonString)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireActivity())
        dangerReportsViewModel.initData()
        dangerZonesViewModel.initData()

        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                startScanning()
            } else {
                Toast.makeText(activity, R.string.bluetooth_permission_not_granted, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openUploadModal() {
        val bottomSheet = UploadSummaryBottomSheetDialog { sheet, item, position ->
            println("Pressed $item at position $position")
            pinSelector?.showReportModal(item, {
                openUploadModal()
            }, true)
            sheet.dismiss()
        }
        bottomSheet.show(
            requireActivity().supportFragmentManager,
            "ModalBottomSheet"
        )
    }

    private fun startScanning() {
        bluetoothHandler.startScanning()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // Get the MapBox context

        // Debug database
        val debugEnabled = resources.getBoolean(R.bool.debug)
        if (debugEnabled) {
            val databaseDebugView: FragmentContainerView =
                view.findViewById(R.id.database_debug_fragment)
            databaseDebugView.visibility = View.VISIBLE
            debugDatabaseFragment = databaseDebugView.getFragment()
            debugDatabaseFragment?.debugEnabled = debugEnabled
            toggleDebugButton = view.findViewById(R.id.debugButton)
            toggleDebugButton?.visibility = View.VISIBLE
            toggleDebugButton?.setOnClickListener {
                debugDatabaseFragment?.debugEnabled = !debugDatabaseFragment!!.debugEnabled
                toggleDebugButton?.text = if (debugDatabaseFragment?.debugEnabled == true) {
                    resources.getText(R.string.disable_debug)
                } else {
                    resources.getText(R.string.enable_debug)
                }
            }
        }

        setupMap(view, savedInstanceState)
        uploadFAB = view.findViewById(R.id.uploadFAB)
        uploadFAB?.setOnClickListener {
           openUploadModal()
        }

        searchResultsViewModel.selected.observe(viewLifecycleOwner, { selected ->
            println("selection changed to $selected")
            println("selected item: ${selected?.let { searchResultsViewModel.dataSet.value?.get(it) }}")
            if (selected != null) {
                val item = searchResultsViewModel.dataSet.value?.get(selected)
                if (item != null) {
                    pinSelector?.showWaypointModal(item)
                    val p = (item.geometry() as Point)
                    val position = CameraPosition.Builder()
                        .target(LatLng(p.latitude(), p.longitude()))
                        .zoom(17.0)
                        .build()
                    mapboxMap?.animateCamera(
                        CameraUpdateFactory
                            .newCameraPosition(position), 7000
                    )
                }
            }
        })

        val dateButton = view.findViewById<ImageButton>(R.id.dateButton)
        val dateChip = view.findViewById<Chip>(R.id.dateChip)
        dateButton.setOnClickListener {
            val timePicker = TimePickerFragment { _, hour, minute ->
                run {
                    dateChip.visibility = View.VISIBLE
                    val time = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                    dateChip.text = time
                    dangerZonesViewModel.selectedTime = time

                }
            }
            timePicker.show(requireActivity().supportFragmentManager, "timePicker")
        }
        dateChip.setOnClickListener {
            dateChip.visibility = View.INVISIBLE
            dangerZonesViewModel.selectedTime = null
        }
    }

    private fun setupMap(view: View, savedInstanceState: Bundle?) {
        // Create map view
        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { map ->
            mapboxMap = map
            map.setStyle(makeCustomGeoapifyStyle()) { style ->
                // Map fully loaded in this scope.
                // Update attributions position
                map.uiSettings.attributionGravity = Gravity.TOP
                map.uiSettings.setAttributionMargins(15, 250, 0, 0)
                map.uiSettings.setCompassMargins(0, 300, 50, 0)
                map.setMaxZoomPreference(20.0)

                addSymbolImages(style)

                // Init classes
                symbolManager = SymbolManager(mapView!!, map, style)

                dangerReports =
                    DangerReports(style, this, dangerReportsViewModel)
                dangerZones =
                    DangerZones(style, this, dangerZonesViewModel)

                location = Location(style, map, requireActivity(), view.findViewById(R.id.gpsFAB))
                location?.onResume()
                location?.enableLocationComponent()

                routing = Routing(
                    style,
                    map,
                    requireActivity(),
                    symbolManager?.layerId!!,
                    dangerZonesViewModel,
                    location!!,
                    routeViewModel
                )

                pinSelector =
                    PinSelector(requireActivity(), mapView!!, map, style, routing, symbolManager)

                // Setup bluetooth handler when all is good

                bluetoothHandler = BluetoothHandler.getInstance(
                    requireActivity() as AppCompatActivity,
                    view.findViewById(R.id.bleFAB),
                    dangerReportsViewModel,
                    startForResult,
                    location!!
                )

                debugDatabaseFragment?.let {
                    it.mapboxMap = map
                }
            }
        }
        routeViewModel.routeGeoJson.observe(viewLifecycleOwner, { routeGeoJson ->
            if (routeGeoJson != null) {
                requireActivity().supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    add<RouteSummaryFragment>(R.id.summary_fragment_container_view)
                }
            } else {
                requireActivity().supportFragmentManager.findFragmentById(R.id.summary_fragment_container_view)
                    ?.let {
                        requireActivity().supportFragmentManager.commit {
                            remove(it)
                        }
                    }
            }
        })
    }

    private fun addSymbolImages(style: Style) {
        val markerIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_marker_icon, null)
        val localMarkerIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_marker_icon_local, null)
        val localMarkerUnsyncIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_marker_icon_local_unsync, null)
        val waypointIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_waypoint_icon, null)
        val destinationIconDrawable =
            ResourcesCompat.getDrawable(
                this.resources,
                R.drawable.ic_destination_icon,
                null
            )
        val warningIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_warning_icon, null)

        style.addImage(
            MainActivity.MARKER_ICON,
            BitmapUtils.getBitmapFromDrawable(markerIconDrawable)!!,
        )
        style.addImage(
            MainActivity.LOCAL_MARKER_ICON,
            BitmapUtils.getBitmapFromDrawable(localMarkerIconDrawable)!!,
        )
        style.addImage(
            MainActivity.LOCAL_MARKER_UNSYNC_ICON,
            BitmapUtils.getBitmapFromDrawable(localMarkerUnsyncIconDrawable)!!,
        )
        style.addImage(
            MainActivity.WAYPOINT_ICON,
            BitmapUtils.getBitmapFromDrawable(waypointIconDrawable)!!
        )
        style.addImage(
            MainActivity.DESTINATION_ICON,
            BitmapUtils.getBitmapFromDrawable(destinationIconDrawable)!!
        )
        style.addImage(
            MainActivity.WARNING_ICON,
            BitmapUtils.getBitmapFromDrawable(warningIconDrawable)!!
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        location?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        location?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        location?.onDestroy()
    }
}