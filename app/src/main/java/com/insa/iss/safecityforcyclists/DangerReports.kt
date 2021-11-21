package com.insa.iss.safecityforcyclists

import android.app.Activity
import android.graphics.Color
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import java.net.URISyntaxException
import java.net.URL
import kotlin.concurrent.thread

class DangerReports(private val loadedMapStyle: Style, private val activity: Activity) {
    companion object {
        private const val CLUSTERS_SOURCE = "clusters"
        private const val UNCLUSTERED_LAYER = "unclustered-points"
    }

    var dangerReportsGeoJson: FeatureCollection? = null
        private set

    fun getGeoJsonData() {
        thread {
            val response =
                URL("https://maplibre.org/maplibre-gl-js-docs/assets/earthquakes.geojson").readText()
            dangerReportsGeoJson = FeatureCollection.fromJson(response)
            activity.runOnUiThread {
                loadGeoJsonSource()
                addUnclusteredLayer()
                addClusteredLayers()
            }
        }
    }

    private fun loadGeoJsonSource() {
        // Add a new source from the GeoJSON data and set the 'cluster' option to true.
        try {
            loadedMapStyle.addSource(
                // Point to GeoJSON data. This example visualizes all M1.0+ earthquakes from
                // 12/22/15 to 1/21/16 as logged by USGS' Earthquake hazards program.
                GeoJsonSource(
                    CLUSTERS_SOURCE,
                    dangerReportsGeoJson,
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                )
            )
        } catch (uriSyntaxException: URISyntaxException) {
            println("Check the URL %s" + uriSyntaxException.message)
        }
    }

    private fun addUnclusteredLayer() {
        //Creating a marker layer for single data points
        val unclustered = SymbolLayer(UNCLUSTERED_LAYER, CLUSTERS_SOURCE)
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
            val circles = CircleLayer("cluster-$i", CLUSTERS_SOURCE)
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
        val count = SymbolLayer("count", CLUSTERS_SOURCE)
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