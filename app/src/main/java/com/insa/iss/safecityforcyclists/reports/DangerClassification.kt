package com.insa.iss.safecityforcyclists.reports

import com.mapbox.mapboxsdk.style.expressions.Expression
import org.json.JSONObject
import org.json.JSONObject.NULL

class DangerClassification(jsonObject: JSONObject) {
    companion object {
        const val defaultMaxDistance = 200.0
        const val defaultMinSpeed = 30.0
    }

    var maxDistance: Double = defaultMaxDistance
    var minSpeed: Double = defaultMinSpeed

    init {
        jsonObject.getDouble("max_distance").let {
            maxDistance = if (it != NULL) {
                it
            } else {
                defaultMaxDistance
            }
        }
        jsonObject.getDouble("min_speed").let {
            minSpeed = if (it != NULL) {
                it
            } else {
                defaultMinSpeed
            }
        }
    }

    fun isDangerous(object_speed: Expression, bicycle_speed: Expression, distance: Expression): Expression {
        return Expression.any(
            Expression.gt(Expression.literal(maxDistance), distance),
            Expression.gt(Expression.subtract(object_speed, bicycle_speed), Expression.literal(minSpeed))
        )
    }
}