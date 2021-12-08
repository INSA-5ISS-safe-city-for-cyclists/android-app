package com.insa.iss.safecityforcyclists.reports

import androidx.fragment.app.Fragment
import com.insa.iss.safecityforcyclists.fragments.MapFragment.Companion.LOCAL_REPORTS_ID
import com.insa.iss.safecityforcyclists.fragments.MapFragment.Companion.REMOTE_REPORTS_ID
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource


class DangerZones(
    private val loadedMapStyle: Style,
    fragment: Fragment,
    private val viewModel: DangerZonesViewModel
) {
    companion object {
        const val HEATMAP_MAX_ZOOM = 14f
        const val HEATMAP_TRANSITION_LENGTH = 2f
    }

    init {
        loadGeoJsonSource()
        addHeatmapLayer()
        addCircleLayer()

        viewModel.getFeatures().observe(fragment.viewLifecycleOwner, {
            loadGeoJsonSource()
        })
    }

    private fun loadGeoJsonSource() {
        val features = viewModel.getFeatures().value
        if (features != null) {
            val source = loadedMapStyle.getSource(REMOTE_REPORTS_ID) as GeoJsonSource?
            if (source == null) {
                loadedMapStyle.addSource(
                    GeoJsonSource(
                        REMOTE_REPORTS_ID,
                        features
                    )
                )
                println("Added initial geojson with $features")
            } else {
                source.setGeoJson(features)
                println("updated geojson with $features")
            }
        }
    }

    private fun addHeatmapLayer() {
        val layer = HeatmapLayer("heatmap", REMOTE_REPORTS_ID)
        layer.maxZoom = HEATMAP_MAX_ZOOM
        layer.sourceLayer = REMOTE_REPORTS_ID
        layer.setProperties(
            // Color ramp for heatmap.  Domain is 0 (low) to 1 (high).
            // Begin color ramp at 0-stop with a 0-transparency color
            // to create a blur-like effect.
            heatmapColor(
                interpolate(
                    linear(), heatmapDensity(),
                    literal(0), rgba(204, 208, 132, 0),
                    literal(0.2), rgb(208, 178, 103),
                    literal(0.4), rgb(208, 138, 69),
                    literal(0.6), rgb(208, 100, 46),
                    literal(0.8), rgb(208, 58, 31),
                    literal(1), rgb(149, 14, 14)
                )
            ),  // Increase the heatmap weight based on frequency and property magnitude
            heatmapWeight(
                interpolate(
                    linear(), get("mag"),
                    stop(0, 0),
                    stop(6, 1)
                )
            ),  // Increase the heatmap color weight weight by zoom level
            // heatmap-intensity is a multiplier on top of heatmap-weight
            heatmapIntensity(
                interpolate(
                    linear(), zoom(),
                    stop(0, 1),
                    stop(HEATMAP_MAX_ZOOM, 3)
                )
            ),  // Adjust the heatmap radius by zoom level
            heatmapRadius(
                interpolate(
                    linear(), zoom(),
                    stop(0, 2),
                    stop(HEATMAP_MAX_ZOOM, 20)
                )
            ),  // Transition from heatmap to circle layer by zoom level
            heatmapOpacity(
                interpolate(
                    linear(), zoom(),
                    stop(HEATMAP_MAX_ZOOM - HEATMAP_TRANSITION_LENGTH, 1),
                    stop(HEATMAP_MAX_ZOOM, 0)
                )
            )
        )
        loadedMapStyle.addLayerBelow(layer, LOCAL_REPORTS_ID)
    }

    private fun addCircleLayer() {
        val circleLayer = CircleLayer(REMOTE_REPORTS_ID, REMOTE_REPORTS_ID)
        circleLayer.setProperties( // Size circle radius by earthquake magnitude and zoom level
            circleRadius(
                interpolate(
                    exponential(2), zoom(),
                    literal(HEATMAP_MAX_ZOOM - HEATMAP_TRANSITION_LENGTH), literal(2),
                    literal(20), literal(200)
                )
            ),  // Color circle by earthquake magnitude
            circleColor(
                interpolate(
                    linear(), get("mag"),
                    literal(1), rgb(208, 185, 12),
                    literal(2), rgb(208, 155, 32),
                    literal(3), rgb(208, 129, 32),
                    literal(4), rgb(208, 99, 27),
                    literal(5), rgb(239, 63, 19),
                    literal(6), rgb(178, 36, 36)
                )
            ),  // Transition from heatmap to circle layer by zoom level
            circleOpacity(
                interpolate(
                    linear(), zoom(),
                    stop(HEATMAP_MAX_ZOOM - HEATMAP_TRANSITION_LENGTH, 0),
                    stop(HEATMAP_MAX_ZOOM - HEATMAP_TRANSITION_LENGTH / 2, 0.7)
                )
            ),
            circleStrokeColor("white"),
            circleStrokeWidth(1.0f)
        )
        loadedMapStyle.addLayerBelow(circleLayer, "heatmap")
    }
}