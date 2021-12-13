package com.insa.iss.safecityforcyclists.reports

import android.graphics.Color
import androidx.fragment.app.Fragment
import com.insa.iss.safecityforcyclists.MainActivity.Companion.LOCAL_MARKER_ICON
import com.insa.iss.safecityforcyclists.MainActivity.Companion.LOCAL_MARKER_UNSYNC_ICON
import com.insa.iss.safecityforcyclists.fragments.MapFragment.Companion.LOCAL_REPORTS_ID
import com.insa.iss.safecityforcyclists.fragments.MapFragment.Companion.LOCAL_REPORTS_UNSYNC_ID
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class DangerReports(
    private val loadedMapStyle: Style,
    fragment: Fragment,
    private val viewModel: DangerReportsViewModel
) {
    companion object {
        const val ICON_SIZE = 1f
    }

    init {
        loadGeoJsonSource()
        addUnclusteredLayer()
        addUnclusteredLayerUnsync()
        addClusteredLayers()

        viewModel.getFeatures().observe(fragment.viewLifecycleOwner, {
            loadGeoJsonSource()
        })
    }

    private fun loadGeoJsonSource() {
        val features = viewModel.getFeatures().value
        if (features != null) {
            val source = loadedMapStyle.getSource(LOCAL_REPORTS_ID) as GeoJsonSource?
            if (source == null) {
                loadedMapStyle.addSource(
                    GeoJsonSource(
                        LOCAL_REPORTS_ID,
                        features,
                        GeoJsonOptions()
                            .withCluster(true)
                            .withClusterMaxZoom(18)
                            .withClusterRadius(30)
                    )
                )
                println("Added initial geojson with $features")
            } else {
                source.setGeoJson(features)
                println("updated geojson with $features")
            }
        }
    }

    private fun getIconSize(): Expression {
        // Make the icon size increase with zoom level
        return interpolate(
            exponential(2), zoom(),
            literal(10), literal(0.5f),
            literal(20), literal(1f)
        )
    }

    private fun getIconOffset(): Array<Float> {
        // Icon is 41px tall, so move it 20px for the pin to be on the base
        return arrayOf(0f, -20f)
    }

    private fun addUnclusteredLayer() {
        //Creating a marker layer for single data points
        val unclustered = SymbolLayer(LOCAL_REPORTS_ID, LOCAL_REPORTS_ID)
        unclustered.setProperties(
            iconImage(LOCAL_MARKER_ICON),
            iconSize(getIconSize()),
            iconOffset(getIconOffset())
        )
        unclustered.setFilter(
            all(
                has("sync"),
                eq(get("sync"), true)
            )
        )
        loadedMapStyle.addLayer(unclustered)
    }

    private fun addUnclusteredLayerUnsync() {
        //Creating a marker layer for single data points
        val unclustered = SymbolLayer(LOCAL_REPORTS_UNSYNC_ID, LOCAL_REPORTS_ID)
        unclustered.setProperties(
            iconImage(LOCAL_MARKER_UNSYNC_ICON),
            iconSize(getIconSize()),
            iconOffset(getIconOffset())
        )
        unclustered.setFilter(
            all(
                has("sync"),
                eq(get("sync"), false)
            )
        )
        loadedMapStyle.addLayer(unclustered)
    }

    private fun addClusteredLayers() {
        // Use the earthquakes GeoJSON source to create three layers: One layer for each cluster category.
        // Each point range gets a different fill color.
        val layers = arrayOf(
            intArrayOf(150, 30, Color.parseColor("#cc2617")),
            intArrayOf(20, 25, Color.parseColor("#e56930")),
            intArrayOf(0, 20, Color.parseColor("#e5bd47"))
        )
        for (i in layers.indices) {
            //Add clusters' circles
            val circles = CircleLayer("reports_cluster_$i", LOCAL_REPORTS_ID)
            circles.setProperties(
                circleColor(layers[i][2]),
                circleRadius(layers[i][1].toFloat())
            )
            val pointCount: Expression = toNumber(get("point_count"))

            // Add a filter to the cluster layer that hides the circles based on "point_count"
            circles.setFilter(
                if (i == 0) all(
                    has("point_count"),
                    gte(pointCount, literal(layers[i][0]))
                ) else all(
                    has("point_count"),
                    gte(pointCount, literal(layers[i][0])),
                    lt(pointCount, literal(layers[i - 1][0]))
                )
            )
            loadedMapStyle.addLayer(circles)
        }

        //Add the count labels
        val count = SymbolLayer("reports_count", LOCAL_REPORTS_ID)
        count.setProperties(
            textField(toString(get("point_count"))),
            textSize(12f),
            textColor(Color.WHITE),
            textIgnorePlacement(true),
            textAllowOverlap(true)
        )
        loadedMapStyle.addLayer(count)
    }
}