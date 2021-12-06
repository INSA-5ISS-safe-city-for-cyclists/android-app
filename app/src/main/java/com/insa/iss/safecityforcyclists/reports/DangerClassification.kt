package com.insa.iss.safecityforcyclists.reports

import org.json.JSONObject
import org.json.JSONObject.NULL
import java.lang.Exception

class DangerClassification(jsonObject: JSONObject) {
    companion object {
        const val defaultMaxDistance = 100.0
        const val defaultMinSpeed = 30.0
    }

    var maxDistance: Double? = null
    var minSpeed: Double? = null

    init {
        try {
            jsonObject.getDouble("distance").let {
                maxDistance = if (it != NULL) {
                    it
                } else {
                    defaultMaxDistance
                }
            }
            jsonObject.getDouble("speed").let {
                minSpeed = if (it != NULL) {
                    it
                } else {
                    defaultMinSpeed
                }
            }
        } catch (e: Exception) {
            maxDistance = defaultMaxDistance
            minSpeed = defaultMinSpeed
        }
    }
}