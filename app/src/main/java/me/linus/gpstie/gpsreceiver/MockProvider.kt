package me.linus.gpstie.gpsreceiver

import android.content.Context
import android.location.LocationManager
import android.location.LocationProvider
import me.linus.gpstie.GpsLocation

class MockProvider(val provider: String, val context: Context) {

    fun getLocationManager(): LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    init {
        var locationManager = getLocationManager()
        locationManager.addTestProvider(provider, true, false, false, false, true, true, true, 0, 1000)

    }

    fun setEnabled(enabled: Boolean) =
        when(enabled){
            true -> {
                val locationManager = getLocationManager()
                locationManager.setTestProviderEnabled(provider, enabled)
                locationManager.setTestProviderStatus(provider, LocationProvider.AVAILABLE,
                        null, System.currentTimeMillis())
            }
            false -> {
                val locationManager = getLocationManager()
                locationManager.clearTestProviderEnabled(provider)
                locationManager.clearTestProviderLocation(provider)
                locationManager.clearTestProviderStatus(provider)
            }
        }

    fun updateLocation(location: GpsLocation) =
            getLocationManager().setTestProviderLocation(provider, location)

    fun updateStatus(status: Int, time: Long) =
            getLocationManager().setTestProviderStatus(provider, status, null, time)

    fun remove() = getLocationManager().removeTestProvider(provider)

}