package me.linus.gpstie

import android.location.Location
import org.json.JSONObject

/**
 * Data to display in GPSDataFragment
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
    }

}