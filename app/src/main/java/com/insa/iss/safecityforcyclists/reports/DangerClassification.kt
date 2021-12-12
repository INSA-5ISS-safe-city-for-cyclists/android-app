package com.insa.iss.safecityforcyclists.reports

import com.insa.iss.safecityforcyclists.database.LocalReport
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONObject.NULL

class DangerClassification(jsonObject: JSONObject?) {
    companion object {
        const val defaultMaxDistance = 200.0
        const val defaultMinSpeed = 30.0

        const val dangerCode = "1"
        const val safeCode = "0"
    }

    var maxDistance: Double = defaultMaxDistance
        private set
    var minSpeed: Double = defaultMinSpeed
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
        } catch (e: JSONException) {
            println(e)
        }
    }

    private fun isDangerous(report: LocalReport): Boolean {
        val relativeSpeed = report.objectSpeed - report.bicycleSpeed
        return (relativeSpeed >= minSpeed || report.distance <= maxDistance)
    }

    fun getDangerCode(report: LocalReport): String {
        val code = if (isDangerous(report)) dangerCode else safeCode
        println("Danger code of report : $report = $code")
        return code
    }

    constructor() : this(null)
}