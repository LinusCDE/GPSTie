package me.linus.gpstie.activity.gpssender

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import me.linus.gpstie.MyActivityBase
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.LocationReceiver
import me.linus.gpstie.R
import me.linus.gpstie.fragment.GPSInfoDetailsFragment
import java.net.NetworkInterface

class ActivityGPSSender: MyActivityBase() {

    companion object {

        val REQUEST_CODE_GPS_PERM = 111
        val GPS_STATE_DONT_SEND = -11

    }

    // ------------------------
    // UI-Elements
    // Server:
    lateinit var uiServerStartStop: ToggleButton
    lateinit var uiServerAddress: TextView
    lateinit var uiServerStatus: TextView
    // GPS:
    lateinit var gpsLocationReceiver: LocationReceiver
    // ------------------------

    var locationService: LocationManager? = null
    lateinit var locationListener: LocationListener

    lateinit var server: GTServer

    var lastGpsState: Int = GPS_STATE_DONT_SEND

    var gpsEnabled: Boolean = false
        @SuppressLint("MissingPermission")
        set(value) {
            if(value == field) return
            when(value) {
                true -> {
                    locationService?.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, 0L, 0F, locationListener)
                }
                false -> {
                    locationService?.removeUpdates(locationListener)
                    gpsLocationReceiver.resetLocation()
                    gpsLocationReceiver.resetStatus()
                    lastGpsState = GPS_STATE_DONT_SEND
                }
            }
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gt_gpssender_layout)

        setTitle(R.string.gt_gs_title_app_name)

        uiServerStartStop = findViewById(R.id.gt_gs_server_start_stop) as ToggleButton
        uiServerAddress = findViewById(R.id.gt_gs_server_address) as TextView
        uiServerStatus = findViewById(R.id.gt_gs_server_status) as TextView

        // Check if app has GPS-Permission
        checkGpsPermission()
    }

    fun loadFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.gt_gs_gps_data, fragment)
        fragmentTransaction.commit()
    }

    /**
     * Gets called when ensured to have gps permission
     * Here the UI should be set up
     */
    fun setupUi() {
        // Display Ip-Address:
        val refreshIp = {
            uiServerAddress.text = getLocalIp() ?:
                    resources.getString(R.string.gt_gs_label_enable_wifi_ap)
        }

        // Handle Ip-Changes:
        registerReceiver(object: BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                val oldIp: String = uiServerAddress.text.toString()
                val newIp = getLocalIp()

                if(oldIp == newIp) return

                refreshIp()
            }

        }, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        refreshIp()

        // GPS:
        // Source: https://developer.android.com/guide/topics/location/strategies.html#Updates
        locationService = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object: LocationListener {

            override fun onProviderDisabled(provider: String?) {
                if (provider == null || !provider.contains("gps")) return
                // GPS turned off or was off

                showDialogGpsMissing()
                gpsLocationReceiver.resetLocation()

                if(provider?.contains("gps"))
                    updateStatus(LocationProvider.TEMPORARILY_UNAVAILABLE)
            }

            override fun onProviderEnabled(provider: String) {
                if(provider?.contains("gps"))
                    updateStatus(LocationProvider.AVAILABLE)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) =
                updateStatus(status)

            override fun onLocationChanged(location: Location?) {
                if(location == null) return

                gpsLocationReceiver.updateLocation(GpsLocation.fromLocation(location))
                server.updateLocation(location)
            }

            fun updateStatus(status: Int){
                lastGpsState = status
                server.updateStatus(status)
                gpsLocationReceiver.updateStatus(status)
            }

        }

        // Create and load GPS-Data fragment
        gpsLocationReceiver = GPSInfoDetailsFragment()
        loadFragment(gpsLocationReceiver as Fragment)

        // Server setup:
        server = GTServer(object: GTServer.GTServerListener {
            override fun onServerStatusChanged(status: String) =
                runOnUiThread { uiServerStatus.text = status }

            override fun onServerStarted() =
                    runOnUiThread {
                        uiServerStartStop.isChecked = true
                        wakeLock.acquire()
                    }

            override fun onServerStopped() =
                    runOnUiThread {
                        uiServerStartStop.isChecked = false
                        try { wakeLock.release() }catch (e: Exception) { }
                    }

            override fun onClientConnected(clientConnection: GTConnection, server: GTServer) {
                if(lastGpsState != GPS_STATE_DONT_SEND)
                    server.updateStatus(lastGpsState, singleReceiver = clientConnection)
                if(server.getClients().size > 0 && !gpsEnabled)
                    runOnUiThread { gpsEnabled = true } // Fails if not done in UI-Thread
            }

            override fun onClientDisonnected(clientConnection: GTConnection, server: GTServer) {
                if(server.getClients().size == 0 && gpsEnabled)
                    runOnUiThread { gpsEnabled = false } // Fails if not done in UI-Thread
            }
        })

        uiServerStartStop.setOnClickListener {
            when(uiServerStartStop.isChecked){
                true -> {
                    if(getLocalIp() == null){
                        uiServerStartStop.isChecked = false
                        Toast.makeText(this, R.string.gt_gs_message_wifi_ap_missing,
                                Toast.LENGTH_LONG).show()
                    }else
                        server.start()
                }
                false -> server.stop()
            }
        }
    }

    /**
     * Get Local Ip in WiFi or AP
     * Thanks to https://stackoverflow.com/a/15060411/3949509 for this solution
     */
    fun getLocalIp(): String? {
        for(networkInterface in NetworkInterface.getNetworkInterfaces()){
            if(networkInterface.name.contains("wlan"))
                for(ipAddress in networkInterface.inetAddresses)
                    if(ipAddress.address.size == 4 /* = is IPv4 */
                            && !ipAddress.isLoopbackAddress /* = is not 127.0.0.1 */)
                        return ipAddress.hostAddress
        }

        return null // =  No WiFi connected or no (detected) AP active
    }

    /**
     * Check if GPS-Permission is given
     */
    fun checkGpsPermission() {
        val gpsPermStr: String = Manifest.permission.ACCESS_FINE_LOCATION

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Permission needs to be checked at runtime
            when(checkSelfPermission(gpsPermStr)) {
                PackageManager.PERMISSION_GRANTED -> setupUi()
                PackageManager.PERMISSION_DENIED ->
                    requestPermissions(arrayOf(gpsPermStr), REQUEST_CODE_GPS_PERM)
            }
        }else{
            when(checkCallingOrSelfPermission(gpsPermStr)) {
                PackageManager.PERMISSION_GRANTED -> setupUi()
                PackageManager.PERMISSION_DENIED -> complainAboutGpsAndQuit()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        if(requestCode == REQUEST_CODE_GPS_PERM){
            when(grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> setupUi()
                PackageManager.PERMISSION_DENIED -> complainAboutGpsAndQuit()
            }
        }
    }

    /**
     * Complains and quits this activity when checkGpsPermission()
     * detected a lack of the permission to use GPS.
     */
    fun complainAboutGpsAndQuit() { // could also be named "showDialogNoGpsPerms()"
        val dialogBuilder = AlertDialog.Builder(this)

        dialogBuilder.setTitle(R.string.gt_gs_title_nogpsperms)
        dialogBuilder.setMessage(R.string.gt_gs_message_nogpsperms)
        dialogBuilder.setNeutralButton(R.string.gt_gs_button_nogpsperms,
                { _, _ -> returnToMainActivity() }) // Back to main screen

        dialogBuilder.setCancelable(false)
        dialogBuilder.show()
    }

    fun showDialogGpsMissing() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(R.string.gt_gs_title_gps_missing)
        dialogBuilder.setMessage(R.string.gt_gs_message_gps_missing)
        dialogBuilder.setPositiveButton(R.string.gt_gs_button_gps_missing, { _, _ ->
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        })

        dialogBuilder.show()
    }

}