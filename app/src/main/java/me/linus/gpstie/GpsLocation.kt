package me.linus.gpstie

import android.location.Location
import org.json.JSONObject

/**
 * Own implementation of android.location.Location
 */
class GpsLocation(latitude: Double, longitude: Double, accuracy: Float,
                       altitude: Double, bearing: Float, speed: Float,
                       provider: String, elapsedRealtimeNanos: Long, time: Long,
                       val hasAccuracy: Boolean, val hasAltitude: Boolean,
                       val hasBearing: Boolean, val hasSpeed: Boolean): Location(provider) {

    init {
        super.setLatitude(latitude)
        super.setLongitude(longitude)
        super.setAccuracy(accuracy)
        super.setAltitude(altitude)
        super.setBearing(bearing)
        super.setSpeed(speed)
        super.setProvider(provider)
        super.setElapsedRealtimeNanos(elapsedRealtimeNanos)
        super.setTime(time)
    }

    companion object {

        /**
         * Copy data of Location into a GpsLocation
         */
        fun fromLocation(loc: Location): GpsLocation =
                GpsLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        altitude = loc.altitude,
                        bearing = loc.bearing,
                        speed = loc.speed,
                        provider = loc.provider,
                        elapsedRealtimeNanos = loc.elapsedRealtimeNanos,
                        time = loc.time,
                        hasAccuracy = loc.hasAccuracy(),
                        hasAltitude = loc.hasAltitude(),
                        hasBearing = loc.hasBearing(),
                        hasSpeed = loc.hasSpeed()
                )

        /**
         * Creates GpsLocation from JsonObject
         */
        fun fromJson(jsonLoc: JSONObject): GpsLocation =
                GpsLocation(
                        latitude = jsonLoc.getDouble("latitude"),
                        longitude = jsonLoc.getDouble("longitude"),
                        accuracy = jsonLoc.getDouble("accuracy").toFloat(),
                        altitude = jsonLoc.getDouble("altitude"),
                        bearing = jsonLoc.getDouble("bearing").toFloat(),
                        speed = jsonLoc.getDouble("speed").toFloat(),
                        provider = jsonLoc.getString("provider"),
                        elapsedRealtimeNanos = jsonLoc.getLong("elapsedRealtimeNanos"),
                        time = jsonLoc.getLong("time"),
                        hasAccuracy = jsonLoc.getBoolean("hasAccuracy"),
                        hasAltitude = jsonLoc.getBoolean("hasAltitude"),
                        hasBearing = jsonLoc.getBoolean("hasBearing"),
                        hasSpeed = jsonLoc.getBoolean("hasSpeed")
                )

        /**
         * Creates a JsonObjects from given Location
         */
        fun toJson(location: Location): JSONObject {
            val jsonObj = JSONObject()

            jsonObj.put("latitude", location.latitude)
            jsonObj.put("longitude", location.longitude)
            jsonObj.put("accuracy", location.accuracy)
            jsonObj.put("altitude", location.altitude)
            jsonObj.put("bearing", location.bearing)
            jsonObj.put("speed", location.speed)
            jsonObj.put("provider", location.provider)
            jsonObj.put("elapsedRealtimeNanos", location.elapsedRealtimeNanos)
            jsonObj.put("time", location.time)
            jsonObj.put("hasAccuracy", location.hasAccuracy())
            jsonObj.put("hasAltitude", location.hasAltitude())
            jsonObj.put("hasBearing", location.hasBearing())
            jsonObj.put("hasSpeed", location.hasSpeed())

            return jsonObj
        }
    }

}