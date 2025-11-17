package io.github.gustavlindberg99.weather

import org.json.JSONArray
import org.json.JSONObject

/**
 * Extension function adding the properties of `other` to `this`. If a property exists in both, merges the two if they're objects, keeps the longest array if they're arrays, and keeps the value from `other` if they're primitives.
 *
 * @param other The JSON object to merge `this` with.
 */
fun JSONObject.mergeWith(other: JSONObject) {
    for (key in other.keys()) {
        val thisValue = if (this.has(key)) this[key] else null
        val otherValue = other[key]
        if (thisValue is JSONObject && otherValue is JSONObject) {
            thisValue.mergeWith(otherValue)
        }
        else if (thisValue is JSONArray && otherValue is JSONArray && thisValue.length() > otherValue.length()) {
            //For our purposes, we want to keep the longest array, since some APIs can make forecasts farther into the future than others, and arrays will often collide if it's for example the array containing the hours that the API responded with
            continue
        }
        else {
            this.put(key, otherValue)
        }
    }
}