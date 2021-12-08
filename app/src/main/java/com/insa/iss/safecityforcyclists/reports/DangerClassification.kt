package com.insa.iss.safecityforcyclists.reports

import org.json.JSONException
import org.json.JSONObject
import org.json.JSONObject.NULL

class DangerClassification(jsonObject: JSONObject?) {
    companion object {
        const val defaultMaxDistance = 200.0
        const val defaultMinSpeed = 30.0
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

    constructor() : this(null)
}