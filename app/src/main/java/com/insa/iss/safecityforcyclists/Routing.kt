package com.insa.iss.safecityforcyclists

import android.app.Activity
import android.graphics.Color
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.MultiLineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfJoins
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread




class Routing(
    private val loadedMapStyle: Style,
    private val mapboxMap: MapboxMap,
    private val activity: Activity,
    private val symbolLayerId: String,
    private val dangerReportsViewModel: DangerReportsViewModel,
    private val location: Location,
    private val routeViewModel: RouteViewModel
) {
    companion object {
        private const val ROUTE_SOURCE = "route_source"
        private const val ROUTE_LAYER = "route_layer"
        private const val BBOX_SOURCE = "bbox_source"
        private const val BBOX_LAYER = "bbox_layer"
        private const val DANGER_SOURCE = "danger_source"
        private const val DANGER_LAYER = "danger_layer"
        private const val MIN_DANGER_DISTANCE = 50 // in meters
    }
    private var routeBounds: DoubleArray? = null

    var startSymbol: Symbol? = null
        set(value) {
            field = value
            calculateRoute(startSymbol?.latLng, endSymbol?.latLng)
        }
    var endSymbol: Symbol? = null
        set(value) {
            field = value
            calculateRoute(startSymbol?.latLng, endSymbol?.latLng)
        }

    private fun getApiCall(start: LatLng, end: LatLng): String {
        return "${activity.getString(R.string.geoapify_routing_url)}?waypoints=${start.latitude},${start.longitude}${
            URLEncoder.encode(
                "|",
                "utf-8"
            )
        }${end.latitude},${end.longitude}&mode=bicycle&apiKey=${activity.getString(R.string.geoapify_access_token)}"
    }

    private fun getGeoJsonData(start: LatLng, end: LatLng) {
        thread {
            val response = URL(getApiCall(start, end)).readText()
            activity.runOnUiThread {
                routeViewModel.routeGeoJson.value = FeatureCollection.fromJson(response)
                renderRoute()
            }
        }
    }

    private fun detectDangerousZones() {
        // Need to check if close to danger

        val dangerFeatures = dangerReportsViewModel.getFeatures().value
        if (dangerFeatures != null) {
            // Create a bounding box around the path with turf-bbox and turf-bbox-polygon
            routeBounds = TurfMeasurement.bbox(routeViewModel.routeGeoJson.value)
            // Make the bounding box a bit bigger than the path (add some padding)
            for (i in routeBounds?.indices!!) {
                if (i < 2) {
                    routeBounds!![i] -= 0.0008
                } else {
                    routeBounds!![i] += 0.0008
                }
                println(routeBounds!![i])
            }
            // Find all the danger reports inside the box with turf-points-within-polygon
            val polygon = TurfMeasurement.bboxPolygon(routeBounds!!)
            showPathBoundingBox(polygon)
            val pointsInBox = TurfJoins.pointsWithinPolygon(
                dangerFeatures,
                FeatureCollection.fromFeature(polygon)
            )
            println("Points in box")
            println(pointsInBox)
            // For each danger in the box, find the closest point to the line with turf-nearest-point-on-line
            val pointsFeatures = pointsInBox.features()
            val routePoints =
                (routeViewModel.routeGeoJson.value?.features()?.get(0)?.geometry() as MultiLineString).coordinates()[0]
            if (pointsFeatures != null && routePoints != null) {
                val nearestPoints: Array<Feature?> = arrayOfNulls(pointsFeatures.size)
                for ((i, f) in pointsFeatures.withIndex()) {
                    val point = f.geometry() as Point
                    nearestPoints[i] = TurfMisc.nearestPointOnLine(point, routePoints)
                }
                println("Nearest points")
                // If a distance is less than X meters, the path is considered dangerous
                val dangerousPoints = arrayListOf<Feature>()
                for (f in nearestPoints) {
                    println(f)
                    val distance = f?.properties()?.get("dist")?.asDouble
                    // distance is in kilometers
                    if (distance != null && distance * 1000 < MIN_DANGER_DISTANCE) {
                        dangerousPoints.add(f)
                        println("=============== DANGER ===============")
                        println(f)
                        println("======================================")
                    }
                }
                println(dangerousPoints)
                if (dangerousPoints.size > 0) {
                    loadedMapStyle.addSource(
                        // Point to GeoJSON data. This example visualizes all M1.0+ earthquakes from
                        // 12/22/15 to 1/21/16 as logged by USGS' Earthquake hazards program.
                        GeoJsonSource(
                            DANGER_SOURCE,
                            FeatureCollection.fromFeatures(dangerousPoints)
                        )
                    )
                    val dangerLayer = SymbolLayer(DANGER_LAYER, DANGER_SOURCE)
                    dangerLayer.setProperties(
                        iconImage(MainActivity.WARNING_ICON),
                        iconSize(0.8f),
                    )
                    loadedMapStyle.addLayer(dangerLayer)
                }
            }
        }
    }

    private fun showPathBoundingBox(polygon: Feature) {
        // Convert the bounding box to a polygon and display it to the map
        // TODO remove after debug
        val polygonSource = GeoJsonSource(
            BBOX_SOURCE,
            polygon
        )
        loadedMapStyle.addSource(polygonSource)
        val polygonLayer = FillLayer(BBOX_LAYER, BBOX_SOURCE)
        polygonLayer.setProperties(
            fillColor(Color.parseColor("#550000ff")),
        )
        loadedMapStyle.removeLayer(BBOX_LAYER)
        loadedMapStyle.addLayerBelow(polygonLayer, symbolLayerId)
    }

    private fun renderRoute() {
        // Add the route geojson as data source
        val route = GeoJsonSource(
            ROUTE_SOURCE,
            routeViewModel.routeGeoJson.value
        )
        loadedMapStyle.addSource(route)
        detectDangerousZones()

        // Add the layer rendering the route
        val routeLayer = LineLayer(ROUTE_LAYER, ROUTE_SOURCE)
        routeLayer.setProperties(
            lineColor(Color.parseColor("#2669CC")),
            lineWidth(6f),
            lineCap(LINE_CAP_ROUND),
            lineJoin(LINE_JOIN_ROUND)
        )
        loadedMapStyle.addLayerBelow(routeLayer, symbolLayerId)
        // Fit camera to route
        if (routeBounds != null) {
            val latLngBounds = LatLngBounds.Builder()
                .include(LatLng(routeBounds!![1], routeBounds!![0]))
                .include(LatLng(routeBounds!![3], routeBounds!![2]))
                .build()
            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0, 250, 0, 200), 1000)
        }
    }

    private fun calculateRoute(start: LatLng?, end: LatLng?) {
        if (loadedMapStyle.getSource(ROUTE_SOURCE) != null) {
            loadedMapStyle.removeSource(ROUTE_SOURCE)
        }
        if (loadedMapStyle.getLayer(ROUTE_LAYER) != null) {
            loadedMapStyle.removeLayer(ROUTE_LAYER)
        }
        if (loadedMapStyle.getSource(BBOX_SOURCE) != null) {
            loadedMapStyle.removeSource(BBOX_SOURCE)
        }
        if (loadedMapStyle.getLayer(BBOX_LAYER) != null) {
            loadedMapStyle.removeLayer(BBOX_LAYER)
        }
        if (loadedMapStyle.getSource(DANGER_SOURCE) != null) {
            loadedMapStyle.removeSource(DANGER_SOURCE)
        }
        if (loadedMapStyle.getLayer(DANGER_LAYER) != null) {
            loadedMapStyle.removeLayer(DANGER_LAYER)
        }
        routeViewModel.routeGeoJson.value = null
        if (start != null && end != null) {
            getGeoJsonData(start, end)
        } else if (end != null && location.lastLocation != null) {
            getGeoJsonData(LatLng(location.lastLocation!!.latitude, location.lastLocation!!.longitude) ,end)
        }
    }

}