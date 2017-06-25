package me.linus.gpstie.activity.gpsreceiver

import android.content.Context
import android.location.Location
import android.location.LocationManager
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
            true -> getLocationManager().setTestProviderEnabled(provider, enabled)
            false -> {
                val locationManager = getLocationManager()
                locationManager.clearTestProviderEnabled(provider)
                locationManager.clearTestProviderLocation(provider)
                locationManager.clearTestProviderStatus(provider)
            }
        }

    fun updateLocation(location: GpsLocation) =
            getLocationManager().setTestProviderLocation(provider, location)

    fun remove() = getLocationManager().removeTestProvider(provider)

}