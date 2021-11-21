package com.insa.iss.safecityforcyclists

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.location.permissions.PermissionsListener
import com.mapbox.mapboxsdk.location.permissions.PermissionsManager
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

class Location(
    private val loadedMapStyle: Style,
    private val mapboxMap: MapboxMap?,
    private val activity: Activity,
    private var gpsButton: FloatingActionButton
) : PermissionsListener {
    private var permissionsManager: PermissionsManager? = null

    private val gpsOffDrawable =
        ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_baseline_gps_off_24, null)
    private val gpsNotFixedDrawable = ResourcesCompat.getDrawable(
        activity.resources,
        R.drawable.ic_baseline_gps_not_fixed_24,
        null
    )
    private val gpsFixedDrawable =
        ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_baseline_gps_fixed_24, null)

    @SuppressLint("MissingPermission")
    fun enableLocationComponent() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            gpsButton.setImageDrawable(gpsFixedDrawable)
            // Get an instance of the component
            val locationComponent: LocationComponent = mapboxMap?.locationComponent!!
            // Activate with options
            val locationComponentOptions = LocationComponentOptions.builder(activity)
                .trackingGesturesManagement(true)
                .build()

            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions
                    .builder(activity, loadedMapStyle)
                    .locationComponentOptions(locationComponentOptions)
                    .build()
            )

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING
            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS

            locationComponent.addOnCameraTrackingChangedListener(object :
                OnCameraTrackingChangedListener {
                override fun onCameraTrackingDismissed() {
                    gpsButton.setImageDrawable(gpsNotFixedDrawable)
                }
                override fun onCameraTrackingChanged(currentMode: Int) {
                    gpsButton.setImageDrawable(gpsFixedDrawable)
                }
            })
            gpsButton.setOnClickListener {
                locationComponent.cameraMode = CameraMode.TRACKING
            }
        } else {
            gpsButton.setImageDrawable(gpsOffDrawable)
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(activity)
        }
    }


    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        permissionsManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String?>?) {
        Toast.makeText(activity, R.string.user_location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent()
        } else {
            Toast.makeText(
                activity,
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            )
                .show()
        }
    }
}