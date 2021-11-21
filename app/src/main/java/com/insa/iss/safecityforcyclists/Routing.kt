package com.insa.iss.safecityforcyclists

import android.content.Context
import android.graphics.Color
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import java.net.URI
import java.net.URLEncoder

class Routing(private val loadedMapStyle: Style, private val context: Context, private val symbolLayerId: String) {
    companion object {
        private const val ROUTE_SOURCE = "route_source"
        private const val ROUTE_LAYER = "route_layer"
    }

    var startSymbol: Symbol? = null
        set(value) {
            field = value
            renderRoute(startSymbol?.latLng, endSymbol?.latLng)
        }
    var endSymbol: Symbol? = null
        set(value) {
            field = value
            renderRoute(startSymbol?.latLng, endSymbol?.latLng)
        }

    private fun getApiCall(start: LatLng, end: LatLng): String {
        return "${context.getString(R.string.geoapify_routing_url)}?waypoints=${start.latitude},${start.longitude}${URLEncoder.encode("|", "utf-8")}${end.latitude},${end.longitude}&mode=bicycle&apiKey=${context.getString(R.string.geoapify_access_token)}"
    }

    private fun generateRoute(start: LatLng, end: LatLng): GeoJsonSource {
        return GeoJsonSource(
            ROUTE_SOURCE,
            URI(getApiCall(start, end))
        )
    }

    private fun renderRoute(start: LatLng?, end: LatLng?) {
        if (loadedMapStyle.getSource(ROUTE_SOURCE) != null) {
            loadedMapStyle.removeSource(ROUTE_SOURCE)
        }
        if (loadedMapStyle.getLayer(ROUTE_LAYER) != null) {
            loadedMapStyle.removeLayer(ROUTE_LAYER)
        }
        if (start != null && end != null) {
            val route = generateRoute(start, end)
            println(route)
            // Add the route geojson as data source
            loadedMapStyle.addSource(route)
            // Add the layer rendering the route
            val routeLayer = LineLayer(ROUTE_LAYER, ROUTE_SOURCE)
            routeLayer.setProperties(
                lineColor(Color.parseColor("#2669CC")),
                lineWidth(6f),
                lineCap(LINE_CAP_ROUND),
                lineJoin(LINE_JOIN_ROUND)
            )
            loadedMapStyle.addLayerBelow(routeLayer, symbolLayerId)
        }
    }

}