package com.insa.iss.safecityforcyclists.reports

import com.insa.iss.safecityforcyclists.database.LocalReport
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONObject.NULL
import kotlin.math.abs

class DangerClassification(jsonObject: JSONObject?) {
    companion object {
        const val defaultMinDistanceThreshold = 50.0
        const val defaultMinSpeedThreshold = 5.0
        const val defaultMinSpeed0_1 = 30.0
        const val defaultMinSpeed1_2 = 80.0
        const val defaultMaxDistance0 = 100.0
        const val defaultMaxDistance1 = 350.0
        const val defaultMaxDistance2 = 700.0

        const val dangerCode = "1"
        const val safeCode = "0"
    }
    var minDistanceThreshold: Double = defaultMinDistanceThreshold
        private set
    var minSpeedThreshold: Double = defaultMinSpeedThreshold
        private set

    var minSpeed0_1: Double = defaultMinSpeed0_1
        private set
    var minSpeed1_2: Double = defaultMinSpeed1_2
        private set
    var maxDistance0: Double = defaultMaxDistance0
        private set
    var maxDistance1: Double = defaultMaxDistance1
        private set
    var maxDistance2: Double = defaultMaxDistance2
        private set

    init {
        try {
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
            jsonObject?.getDouble("min_speed_0_1")?.let {
                minSpeed0_1 = if (it != NULL) {
                    println("Found min speed (0-1): $it")
                    it
                } else {
                    println("Using default min speed (0-1): $defaultMinSpeed0_1")
                    defaultMinSpeed0_1
                }
            }
            jsonObject?.getDouble("min_speed_1_2")?.let {
                minSpeed1_2 = if (it != NULL) {
                    println("Found min speed (1-2): $it")
                    it
                } else {
                    println("Using default min speed (1-1): $defaultMinSpeed1_2")
                    defaultMinSpeed1_2
                }
            }
            jsonObject?.getDouble("max_distance_0")?.let {
                maxDistance0 = if (it != NULL) {
                    println("Found max distance 0: $it")
                    it
                } else {
                    println("Using default max distance 0: $defaultMaxDistance0")
                    defaultMaxDistance0
                }
            }
            jsonObject?.getDouble("max_distance_1")?.let {
                maxDistance1 = if (it != NULL) {
                    println("Found max distance 1: $it")
                    it
                } else {
                    println("Using default max distance 1: $defaultMaxDistance1")
                    defaultMaxDistance1
                }
            }
            jsonObject?.getDouble("max_distance_2")?.let {
                maxDistance2 = if (it != NULL) {
                    println("Found max distance 2: $it")
                    it
                } else {
                    println("Using default max distance 2: $defaultMaxDistance2")
                    defaultMaxDistance2
                }
            }
        } catch (e: JSONException) {
            println(e)
        }
    }

    private fun isDangerous(report: LocalReport): Boolean {
        val relativeSpeed = abs(report.objectSpeed - report.bicycleSpeed)
        val absoluteSpeed = abs(report.objectSpeed);
        return absoluteSpeed >= minSpeedThreshold && relativeSpeed >= minSpeedThreshold && report.distance >= minDistanceThreshold &&
                (report.distance <= maxDistance0) ||
                (report.distance <= maxDistance1 && relativeSpeed >= minSpeed0_1) ||
                (report.distance <= maxDistance2 && relativeSpeed >= minSpeed1_2)
    }

    fun getDangerCode(report: LocalReport): String {
        val code = if (isDangerous(report)) dangerCode else safeCode
        println("Danger code of report : $report = $code")
        return code
    }

    constructor() : this(null)
}