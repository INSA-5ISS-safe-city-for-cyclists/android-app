package com.insa.iss.safecityforcyclists.fragments

import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.routing.RouteViewModel
import com.mapbox.turf.TurfConversion
import java.util.*

class RouteSummaryFragment : Fragment(R.layout.route_summary) {

    private var timeText: TextView? = null
    private var distanceText: TextView? = null
    private val routeViewModel: RouteViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val slideIn = Slide()
        slideIn.slideEdge = Gravity.BOTTOM
        enterTransition = slideIn
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        timeText = view.findViewById(R.id.timeText)
        distanceText = view.findViewById(R.id.distanceText)

        val properties = routeViewModel.routeGeoJson.value?.features()?.get(0)?.properties()

        val distance = properties?.get("distance")?.asDouble
        if (distance != null) {
            distanceText?.text = distanceToString(distance)
        } else {
            distanceText?.text = "ERROR"
        }

        val time = properties?.get("time")?.asDouble
        println(time)
        if (time != null) {
            timeText?.text = timeToString(time)
        } else {
            timeText?.text = "ERROR"
        }
    }

    private fun timeToString(time: Double): String {
        val date = Date()
        date.time = (time * 1000).toLong()
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("UTC")
        calendar.time = date
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        var stringDate = ""
        if (hours > 0) {
            stringDate += "$hours h"
        }
        if (minutes > 0) {
            if (stringDate.isNotEmpty()) {
                stringDate += " "
            }
            stringDate += "$minutes min"
        }
        if (seconds > 0) {
            if (stringDate.isNotEmpty()) {
                stringDate += " "
            }
            stringDate += "$seconds sec"
        }
        return stringDate
    }

    private fun distanceToString(distance: Double): String {
        var convertedDistance = distance
        val converted = distance > 1000
        if (converted) {
            convertedDistance = TurfConversion.convertLength(distance, "meters", "kilometers")
        }
        var stringDistance = String.format(" %.2f", convertedDistance)
        val stringSplit = stringDistance.split(".")
        if (stringSplit[1] == "00") {
            stringDistance = stringSplit[0]
        }
        stringDistance += if (converted) {
            " km"
        } else {
            " m"
        }
        return stringDistance
    }
}