package me.linus.gpstie.gpsreceiver

import android.content.Context
import android.location.LocationManager
import android.location.LocationProvider
import me.linus.gpstie.GpsLocation

/**
 * Fakes location for most Apps in Android
 * For this to work this app needs:
 * - The ACCESS_MOCK_LOCATION-Permission
 * - To be set as mocking app in the Developer-Options in the Android-Settings
 */
class MockProvider(val provider: String, val context: Context) {

    /**
     * Gets LocationManager
     */
    fun getLocationManager(): LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Registers this Mock-Provider
     */
    init {
        getLocationManager().
                addTestProvider(provider, true, false, false, false, true, true, true, 0, 1000)

    }

    /**
     * En- or disables this Mock-Provider
     */
    fun setEnabled(enabled: Boolean) =
            // Yes, this syntax could be easyly replaced with if-else. But it looks dope ;)
            // For more about this topic see https://www.devrant.io/rants/674857
            when(enabled){
                true -> getLocationManager().run {
                    setTestProviderEnabled(provider, enabled)
                    setTestProviderStatus(
                            provider, LocationProvider.AVAILABLE, null, System.currentTimeMillis())
                }
                false -> getLocationManager().run {
                    clearTestProviderEnabled(provider)
                    clearTestProviderLocation(provider)
                    clearTestProviderStatus(provider)
                }
            }

    /**
     * Sends new location
     */
    fun updateLocation(location: GpsLocation) =
            getLocationManager().setTestProviderLocation(provider, location)

    /**
     * Sends new status
     */
    fun updateStatus(status: Int, time: Long) =
            getLocationManager().setTestProviderStatus(provider, status, null, time)

    /**
     * Removes this provider. Should get invoked when app or service is shutting down
     */
    fun remove() = getLocationManager().removeTestProvider(provider)

}