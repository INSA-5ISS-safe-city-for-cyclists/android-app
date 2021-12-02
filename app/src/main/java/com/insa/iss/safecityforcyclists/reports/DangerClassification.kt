package com.insa.iss.safecityforcyclists.reports

import org.json.JSONObject
import org.json.JSONObject.NULL

class DangerClassification(jsonObject: JSONObject) {
    var minDistance: Double? = null
    var minSpeed: Double? = null

    init {
        jsonObject.getDouble("distance").let {
            if (it != NULL) {
                minDistance = it
            }
        }
        jsonObject.getDouble("speed").let {
            if (it != NULL) {
                minSpeed = it
            }
        }
    }
}