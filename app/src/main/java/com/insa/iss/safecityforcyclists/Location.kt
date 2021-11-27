package com.insa.iss.safecityforcyclists

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private val locationManager = activity.getSystemService(LOCATION_SERVICE) as LocationManager

    private val gpsOffDrawable =
        ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_baseline_gps_off_24, null)
    private val gpsNotFixedDrawable = ResourcesCompat.getDrawable(
        activity.resources,
        R.drawable.ic_baseline_gps_not_fixed_24,
        null
    )
    private val gpsFixedDrawable =
        ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_baseline_gps_fixed_24, null)

    private var locationComponent: LocationComponent? = null
    private var lastLocation: android.location.Location? = null
    private var signalLost = false

    private val gpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action!! == "android.location.PROVIDERS_CHANGED") {
                updateGpsButton()
            }
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            // If 2 locations next to each other are the same, consider the signal lost
            signalLost = lastLocation?.equals(location) == true
            lastLocation = location
            println("" + location.longitude + ":" + location.latitude)
            if (signalLost) {
                println("GPS signal lost !")
                updateGpsButton()
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun enableLocationComponent() {
        updateGpsButton()

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, locationListener)

        gpsButton.setOnClickListener {
            if (!PermissionsManager.areLocationPermissionsGranted(activity)) {
                permissionsManager.requestLocationPermissions(activity)
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (lastLocation != null && !signalLost) {
                    locationComponent?.cameraMode = CameraMode.TRACKING
                } else {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,  {
                        locationComponent?.cameraMode = CameraMode.TRACKING
                    }, null)
                    Toast.makeText(activity, "Location unknown, searching...", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(activity, "Location not enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateGpsButton() {
        if (PermissionsManager.areLocationPermissionsGranted(activity) && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !signalLost) {
            gpsButton.setImageDrawable(gpsNotFixedDrawable)
            // Get an instance of the component
            locationComponent = mapboxMap?.locationComponent!!
            // Activate with options
            val locationComponentOptions = LocationComponentOptions.builder(activity)
                .trackingGesturesManagement(true)
                .enableStaleState(false)
                .build()

            locationComponent?.activateLocationComponent(
                LocationComponentActivationOptions
                    .builder(activity, loadedMapStyle)
                    .locationComponentOptions(locationComponentOptions)
                    .build()
            )


            // Enable to make component visible
            locationComponent?.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent?.cameraMode = CameraMode.TRACKING
            // Set the component's render mode
            locationComponent?.renderMode = RenderMode.COMPASS

            locationComponent?.addOnCameraTrackingChangedListener(object :
                OnCameraTrackingChangedListener {
                override fun onCameraTrackingDismissed() {
                    gpsButton.setImageDrawable(gpsNotFixedDrawable)
                }
                override fun onCameraTrackingChanged(currentMode: Int) {
                    gpsButton.setImageDrawable(gpsFixedDrawable)
                }
            })
        } else {
            gpsButton.setImageDrawable(gpsOffDrawable)
            locationComponent?.isLocationComponentEnabled = false
            if (!PermissionsManager.areLocationPermissionsGranted(activity)) {
                permissionsManager.requestLocationPermissions(activity)
            }
        }
    }


    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

    fun onResume() {
        activity.registerReceiver(gpsSwitchStateReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    fun onDestroy() {
        activity.unregisterReceiver(gpsSwitchStateReceiver)
    }
}