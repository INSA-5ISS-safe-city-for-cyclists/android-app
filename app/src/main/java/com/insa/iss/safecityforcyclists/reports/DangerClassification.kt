package com.insa.iss.safecityforcyclists.reports

import com.insa.iss.safecityforcyclists.database.LocalReport
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONObject.NULL

class DangerClassification(jsonObject: JSONObject?) {
    companion object {
        const val defaultMaxDistance = 200.0
        const val defaultMinSpeed = 30.0
        const val defaultMinDistanceThreshold = 50.0
        const val defaultMinSpeedThreshold = 5.0

        const val dangerCode = "1"
        const val safeCode = "0"
    }

    var maxDistance: Double = defaultMaxDistance
        private set
    var minSpeed: Double = defaultMinSpeed
        private set
    var minDistanceThreshold: Double = defaultMinDistanceThreshold
        private set
    var minSpeedThreshold: Double = defaultMinSpeedThreshold
        private set

    init {
        try {
            jsonObject?.getDouble("max_distance")?.let {
                maxDistance = if (it != NULL) {
                    println("Found max distance: $it")
                    it
                } else {
                    println("Using default max distance: $defaultMaxDistance")
                    defaultMaxDistance
                }
            }
            jsonObject?.getDouble("min_speed")?.let {
                minSpeed = if (it != NULL) {
                    println("Found min speed: $it")
                    it
                } else {
                    println("Using default min speed: $defaultMinSpeed")
                    defaultMinSpeed
                }
            }
            jsonObject?.getDouble("min_distance_threshold")?.let {
                minDistanceThreshold = if (it != NULL) {
                    println("Found min distance threshold : $it")
                    it
                } else {
                    println("Using default min distance threshold : $defaultMinDistanceThreshold")
                    defaultMinDistanceThreshold
                }
            }
            jsonObject?.getDouble("min_speed_threshold")?.let {
                minSpeedThreshold = if (it != NULL) {
                    println("Found min speed threshold : $it")
                    it
                } else {
                    println("Using default min speed threshold : $defaultMinSpeedThreshold")
                    defaultMinSpeedThreshold
                }
            }
        } catch (e: JSONException) {
            println(e)
        }
    }

    private fun isDangerous(report: LocalReport): Boolean {
        val relativeSpeed = report.objectSpeed - report.bicycleSpeed
        return relativeSpeed >= minSpeedThreshold && report.distance >= minDistanceThreshold &&
                (relativeSpeed >= minSpeed || report.distance <= maxDistance)
    }

    fun getDangerCode(report: LocalReport): String {
        val code = if (isDangerous(report)) dangerCode else safeCode
        println("Danger code of report : $report = $code")
        return code
    }

    constructor() : this(null)
}