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

    lateinit var server: GTServer

    var lastGpsState: Int = GPS_STATE_DONT_SEND

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gt_gpssender_layout)

        supportActionBar?.title = "GPS-Sender"

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
    @SuppressLint("MissingPermission")
    fun setupUi() {
        // Display Ip-Address:
        val refreshIp = {
            uiServerAddress.text = getLocalIp() ?: "Schalte Wlan oder Hotspot an!"
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
        val locationService = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val context = this

        val locationListener = object: LocationListener {

            override fun onProviderDisabled(provider: String?) {
                if (provider == null || !provider.contains("gps")) return
                // GPS turned off or was off

                val dialogBuilder = AlertDialog.Builder(context)

                dialogBuilder.setTitle("GPS ist aus")
                dialogBuilder.setMessage("Bitte aktiviere GPS.")

                dialogBuilder.setPositiveButton("Standort-Einstellungen öffnen", { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                })

                dialogBuilder.show()

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

        locationService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0F, locationListener)

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
                        wakeLock.release()
                    }

            override fun onClientConnected(clientConnection: GTConnection, server: GTServer) {
                if(lastGpsState != GPS_STATE_DONT_SEND)
                    server.updateStatus(lastGpsState, singleReceiver = clientConnection)
            }
        })

        uiServerStartStop.setOnClickListener {
            when(uiServerStartStop.isChecked){
                true -> {
                    if(getLocalIp() == null){
                        uiServerStartStop.isChecked = false
                        Toast.makeText(context, "Du musst mit einem W-Lan verbunden sein oder " +
                                "einen Hotspot aktiv haben!", Toast.LENGTH_LONG).show()
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
    fun complainAboutGpsAndQuit() {
        val dialogBuilder = AlertDialog.Builder(this)

        dialogBuilder.setTitle("GPS ist nötig!")
        dialogBuilder.setMessage("Damit du deinen Standort teilen kannst, muss die App " +
                "rechte für das GPS haben!")

        // Perform check again
        dialogBuilder.setNeutralButton("Ok", { _, _ -> returnToMainActivity() })
        dialogBuilder.setCancelable(false)

        // Show dialog
        dialogBuilder.show()
    }

}