package com.insa.iss.safecityforcyclists.reports

import com.mapbox.mapboxsdk.style.expressions.Expression
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONObject.NULL

class DangerClassification(jsonObject: JSONObject?) {
    companion object {
        const val defaultMaxDistance = 200.0
        const val defaultMinSpeed = 30.0
    }

    private var maxDistance: Double = defaultMaxDistance
    private var minSpeed: Double = defaultMinSpeed

    init {
        try {
            jsonObject?.getDouble("max_distance")?.let {
                maxDistance = if (it != NULL) {
                    println("Found max distance: $it")
                    it
                } else {
                    defaultMaxDistance
                }
            }
            jsonObject?.getDouble("min_speed")?.let {
                minSpeed = if (it != NULL) {
                    println("Found min speed: $it")
                    it
                } else {
                    defaultMinSpeed
                }
            }
        } catch (e: JSONException) {
            println(e)
        }
    }

    constructor() : this(null)

    fun isDangerous(object_speed: Expression, bicycle_speed: Expression, distance: Expression): Expression {
        return Expression.any(
            Expression.gt(Expression.literal(maxDistance), distance),
            Expression.gt(Expression.subtract(object_speed, bicycle_speed), Expression.literal(minSpeed))
        )
    }
}