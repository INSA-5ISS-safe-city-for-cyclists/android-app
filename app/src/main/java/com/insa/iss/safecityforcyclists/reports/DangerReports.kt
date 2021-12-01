package com.insa.iss.safecityforcyclists.reports

import android.graphics.Color
import androidx.fragment.app.Fragment
import com.insa.iss.safecityforcyclists.MainActivity
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class DangerReports(private val loadedMapStyle: Style, fragment: Fragment, private val viewModel: DangerReportsViewModel, private val id: String, debug: Boolean = false) {

    init {
        loadGeoJsonSource()
        if (debug) {
            addUnclusteredLayerDebug()
        } else {
            addUnclusteredLayer()
        }
        addClusteredLayers()
        viewModel.getFeatures().observe(fragment.viewLifecycleOwner, {
            loadGeoJsonSource()
        })
    }

    private fun loadGeoJsonSource() {
        val features = viewModel.getFeatures()
        println(id)
        if (features.value != null) {
            val source = loadedMapStyle.getSource(id) as GeoJsonSource?
            if (source == null) {
                loadedMapStyle.addSource(
                    GeoJsonSource(
                        id,
                        features.value,
                        GeoJsonOptions()
                            .withCluster(true)
                            .withClusterMaxZoom(14)
                            .withClusterRadius(50)
                    )
                )
                println("Added initial geojson with ${features.value}")
            } else {
                source.setGeoJson(features.value)
                println("updated geojson with ${features.value}")
            }
        }
    }

    private fun addUnclusteredLayerDebug() {
        //Creating a marker layer for single data points
        val unclustered = SymbolLayer(id, id)
        unclustered.setProperties(
            iconImage(MainActivity.MARKER_ICON),
            iconSize(
                Expression.division(
                    Expression.get("mag"), Expression.literal(4.0f)
                )
            )
        )
        unclustered.setFilter(Expression.has("mag"))
        loadedMapStyle.addLayer(unclustered)

    }

    private fun addUnclusteredLayer() {
        //Creating a marker layer for single data points
        val unclustered = SymbolLayer(id, id)
        unclustered.setProperties(
            iconImage(MainActivity.MARKER_ICON),
            iconSize(
                Expression.division(
                    Expression.get("object_speed"), Expression.literal(4.0f)
                )
            )
        )
        loadedMapStyle.addLayer(unclustered)
    }

    private fun addClusteredLayers() {
        // Use the earthquakes GeoJSON source to create three layers: One layer for each cluster category.
        // Each point range gets a different fill color.
        val layers = arrayOf(
            intArrayOf(150, 30, Color.parseColor("#51bbd6")),
            intArrayOf(20, 25, Color.parseColor("#f1f075")),
            intArrayOf(0, 20, Color.parseColor("#f28cb1"))
        )
        for (i in layers.indices) {
            //Add clusters' circles
            val circles = CircleLayer("${id}_cluster_$i", id)
            circles.setProperties(
                circleColor(layers[i][2]),
                circleRadius(layers[i][1].toFloat())
            )
            val pointCount: Expression = Expression.toNumber(Expression.get("point_count"))

            // Add a filter to the cluster layer that hides the circles based on "point_count"
            circles.setFilter(
                if (i == 0) Expression.all(
                    Expression.has("point_count"),
                    Expression.gte(pointCount, Expression.literal(layers[i][0]))
                ) else Expression.all(
                    Expression.has("point_count"),
                    Expression.gte(pointCount, Expression.literal(layers[i][0])),
                    Expression.lt(pointCount, Expression.literal(layers[i - 1][0]))
                )
            )
            loadedMapStyle.addLayer(circles)
        }

        //Add the count labels
        val count = SymbolLayer("${id}_count", id)
        count.setProperties(
            textField(Expression.toString(Expression.get("point_count"))),
            textSize(12f),
            textColor(Color.WHITE),
            textIgnorePlacement(true),
            textAllowOverlap(true)
        )
        loadedMapStyle.addLayer(count)
    }
}