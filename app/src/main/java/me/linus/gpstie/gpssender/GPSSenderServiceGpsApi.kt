package me.linus.gpstie.gpssender

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import me.linus.gpstie.LocationReceiver

class GPSSenderServiceGpsApi(val locationManager: LocationManager,
                             val server: GTServer) {

    companion object {
        val GPS_STATUS_DONT_SEND = -11
    }

    // For registering by Activities reserved:
    var assignableLocationReceiver: LocationReceiver? = null
    var assignableLocationListener: LocationListener? = null
    // ---------------------

    // Last state:
    var lastStatus: Int = GPS_STATUS_DONT_SEND
    var lastLocation: Location? = null
    // ---------------------

    // Internal checks for GPS-Updates in connection with given server instance
    val internalLocationListener = object: LocationListener {

        override fun onProviderDisabled(provider: String?) {
            assignableLocationListener?.onProviderDisabled(provider)
            //!! showDialogGpsMissing()
            assignableLocationReceiver?.resetLocation()
            updateStatus(LocationProvider.TEMPORARILY_UNAVAILABLE)
        }

        override fun onProviderEnabled(provider: String) {
            assignableLocationListener?.onProviderEnabled(provider)
            updateStatus(LocationProvider.AVAILABLE)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            assignableLocationListener?.onStatusChanged(provider, status, extras)
            updateStatus(status)
        }

        override fun onLocationChanged(location: Location?) {
            if(location == null) return
            assignableLocationListener?.onLocationChanged(location)

            lastLocation = location
            assignableLocationReceiver?.updateLocation(location)
            server.updateLocation(location)
        }

        fun updateStatus(status: Int){
            lastStatus = status
            server.updateStatus(status)
            assignableLocationReceiver?.updateStatus(status)
        }

    }

    /**
     * (De)Registers internalLocationListener (saves energy when no client connected)
     */
    var gpsEnabled: Boolean = false
        @SuppressLint("MissingPermission")
        set(value) {
            if(value == field) return
            when(value) {
                true -> {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, 0L, 0F, internalLocationListener)
                }
                false -> {
                    locationManager.removeUpdates(internalLocationListener)
                    assignableLocationReceiver?.resetLocation()
                    assignableLocationReceiver?.resetStatus()
                    lastStatus = GPS_STATUS_DONT_SEND
                    lastLocation = null
                }
            }
            field = value
        }
}