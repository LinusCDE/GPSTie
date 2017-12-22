package me.linus.gpstie.gpssender

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.support.v4.app.Fragment
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import me.linus.gpstie.MyActivityBase
import me.linus.gpstie.R
import me.linus.gpstie.GPSInfoDetailsFragment
import me.linus.gpstie.getLocalIp

class ActivityGPSSender: MyActivityBase() {

    companion object {

        val REQUEST_CODE_GPS_PERM = 111

    }

    // ------------------------
    // UI-Elements
    // Server:
    lateinit var uiServerStartStop: ToggleButton
    lateinit var uiServerAddress: TextView
    lateinit var uiServerStatus: TextView
    // GPS:
    lateinit var gpsLocationReceiver: GPSInfoDetailsFragment
    // ------------------------

    var binder: GPSSenderService.GPSSenderServiceBinder? = null
    var serviceConnection = object: ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            println("Lost connection to Service!")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as GPSSenderService.GPSSenderServiceBinder
            binder!!.registerServerListener(serverListener)
            binder!!.registerLocationReceiver(gpsLocationReceiver)
            binder!!.registerLocationListener(locationListener)
            runOnUiThread { uiServerStartStop.isChecked = binder!!.isServerRunning() }
        }

    }

    val serverListener = object: GTServer.GTServerListener {

        override fun onServerStatusChanged(status: String) =
                runOnUiThread { uiServerStatus.text = status }

        override fun onServerStarted() =
                runOnUiThread { uiServerStartStop.isChecked = true }

        override fun onServerStopped() =
                runOnUiThread { uiServerStartStop.isChecked = false }

        override fun onClientConnected(clientConnection: GTConnection, server: GTServer) {}

        override fun onClientDisconnected(clientConnection: GTConnection, server: GTServer) {}

    }

    val locationListener = object: LocationListener {

        override fun onLocationChanged(location: Location?) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) =
                showDialogGpsMissing()

    }

    lateinit var networkActionReciver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gt_gpssender_layout)

        setTitle(R.string.gt_gs_title_app_name)

        uiServerStartStop = findViewById(R.id.gt_gs_server_start_stop) as ToggleButton
        uiServerAddress = findViewById(R.id.gt_gs_server_address) as TextView
        uiServerStatus = findViewById(R.id.gt_gs_server_status) as TextView

        gpsLocationReceiver = GPSInfoDetailsFragment()
        loadFragment(gpsLocationReceiver)

        // Setup networkActionReceiver: (need to be done after getting uiServerStatus)
        networkActionReciver = object: BroadcastReceiver() {

            var lastIp = getLocalIp().also { displayAddress(it) }

            override fun onReceive(context: Context?, intent: Intent?) {
                val newIp = getLocalIp()

                if(lastIp == newIp) return
                lastIp = newIp

                displayAddress(newIp)
            }

        }

        checkGpsPermission() // Check if app has GPS-Permission
    }

    /**
     * Loads a fragment into gt_gs_gps_data (FrameHolder)
     */
    fun loadFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.gt_gs_gps_data, fragment)
        fragmentTransaction.commit()
    }

    fun displayAddress(address: String?) {
        uiServerAddress.text = address ?:
                resources.getString(R.string.gt_gs_label_enable_wifi_ap)
    }

    /**
     * Gets called when ensured to have gps permission
     * Here the UI should be set up
     */
    fun setupUi() {
        // Start Service:
        val service = Intent(this, GPSSenderService::class.java)
        startService(service)
        bindService(service, serviceConnection, 0)

        // Handle Ip-Changes:
        registerReceiver(networkActionReciver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        // GPS:
        val locationListener = object: LocationListener {

            override fun onProviderDisabled(provider: String?) = showDialogGpsMissing()

            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onLocationChanged(location: Location?) {}

        }

        // Create and load GPS-Data fragment

        uiServerStartStop.setOnClickListener {
            when(uiServerStartStop.isChecked){
                true -> {
                    if(getLocalIp() == null){
                        uiServerStartStop.isChecked = false
                        Toast.makeText(this, R.string.gt_gs_message_wifi_ap_missing,
                                Toast.LENGTH_LONG).show()
                    }else
                        binder?.startServer()
                }
                false -> binder?.stopServer()
            }
        }
    }

    /**
     * Checks if GPS-Permission is granted and requests it if not
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

    /**
     * Called if user has decided whether to give this App GPS-Permissions
     */
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

    /**
     * Shows Dialog which complains about missing/disabled GPS.
     */
    fun showDialogGpsMissing() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(R.string.gt_gs_title_gps_missing)
        dialogBuilder.setMessage(R.string.gt_gs_message_gps_missing)
        dialogBuilder.setPositiveButton(R.string.gt_gs_button_gps_missing, { _, _ ->
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        })

        dialogBuilder.show()
    }

    /**
     * Shutdown-Hook:
     * - Unbinds Service
     * - Unregisters networkActionReceiver
     */
    override fun onDestroy() {
        try {
            unregisterReceiver(networkActionReciver)
        } catch(ignored: IllegalArgumentException) {
            // Could not be registered in the first place because the user denied gps permission.
        }

        if(binder == null) {
            super.onDestroy()
            return
        }

        if(!binder!!.service.server.isRunning())
            stopService(Intent(this, GPSSenderService::class.java))
        else
            unbindService(serviceConnection)

        super.onDestroy()
    }

}