package com.insa.iss.safecityforcyclists

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val MARKER_ICON = "MARKER_ICON"
    }

    private var mapView: MapView? = null
    private var symbolManager: SymbolManager? = null

    private fun makeGeoapifyStyleUrl(style: String = "osm-carto"): String {
        return "${getString(R.string.geoapify_styles_url) + style}/style.json?apiKey=${getString(R.string.geoapify_access_token)}";
    }

    private fun makeCustomGeoapifyStyle(): Style.Builder {
        val builder = Style.Builder()
        val inputStream = resources.openRawResource(R.raw.osm_bright_cycleways)
        val jsonString: String = Scanner(inputStream).useDelimiter("\\A").next()
        println(jsonString)
        return builder.fromJson(jsonString)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the MapBox context
        Mapbox.getInstance(this)

        setContentView(R.layout.activity_main)

        // Create map view
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { map ->
            map.setStyle(makeCustomGeoapifyStyle()) { style ->
                // Map fully loaded in this scope.
                // Update attributions position
                map.uiSettings.setAttributionMargins(15, 0, 0, 15)

                // Choose logo to display
                val selectedMarkerIconDrawable =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.ic_marker_icon, null)

                style.addImage(
                    MARKER_ICON,
                    BitmapUtils.getBitmapFromDrawable(selectedMarkerIconDrawable)!!
                )

                symbolManager = SymbolManager(mapView!!, map, style)

                // Add symbol at specified lat/lon.
                val newSymbol = symbolManager?.create(
                    SymbolOptions()
                        .withLatLng(LatLng(43.6020, 1.4530))
                        .withIconImage(MARKER_ICON)
                        .withIconSize(0.8f)
                )
                symbolManager?.update(newSymbol)

                // Add a listener to trigger markers clicks.
                symbolManager?.addClickListener {
                    // Display information
                    Toast.makeText(this, "Marker clicked", Toast.LENGTH_LONG).show();
                    true
                }

            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
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
    }

}