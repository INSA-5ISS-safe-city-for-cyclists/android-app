package com.insa.iss.safecityforcyclists

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.location.permissions.PermissionsListener
import com.mapbox.mapboxsdk.location.permissions.PermissionsManager
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

class Location(private val loadedMapStyle: Style, private val mapboxMap: MapboxMap?, private val activity: Activity): PermissionsListener {
    private var permissionsManager: PermissionsManager? = null

    @SuppressLint("MissingPermission")
    fun enableLocationComponent() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            // Get an instance of the component
            val locationComponent: LocationComponent = mapboxMap?.locationComponent!!
            // Activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(activity, loadedMapStyle).build()
            )
            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING
            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
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
            Toast.makeText(activity, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG)
                .show()
        }
    }
}