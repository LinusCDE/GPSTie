package me.linus.gpstie.gpsreceiver

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.LocationReceiver
import me.linus.gpstie.MyActivityBase
import me.linus.gpstie.R
import me.linus.gpstie.GPSInfoDetailsFragment

class ActivityGPSReceiver: MyActivityBase() {

    // UI-Elements:
    lateinit var uiAddress: EditText
    lateinit var uiConnectDisconnect: ToggleButton
    lateinit var uiStatus: TextView
    // --------------------

    lateinit var prefs: SharedPreferences // To save content of the uiAddress-TextBox

    var gpsLocationReceiver: LocationReceiver? = null // Will initialized with a Fragment

    val clientListener = object: GTClient.GTClientListener {

        override fun onClientConnecting() =
                runOnUiThread {
                    uiAddress.isEnabled = false
                    prefs.edit().apply {
                        putString("address", uiAddress.text.toString()) }.apply()
                }

        override fun onClientConnected() =
                runOnUiThread {
                    uiConnectDisconnect.isChecked = true
                }

        override fun onClientDisconnected() =
                runOnUiThread {
                    uiAddress.isEnabled = true
                    uiConnectDisconnect.isChecked = false
                    gpsLocationReceiver?.resetLocation()
                    gpsLocationReceiver?.resetStatus()
                }

        override fun onClientStatusChanged(status: String) =
                runOnUiThread { uiStatus.text = status }

        override fun onLocationReceived(location: GpsLocation) =
                runOnUiThread {
                    gpsLocationReceiver?.updateLocation(location)
                }

        override fun onGpsStatusChanged(status: Int, time: Long) =
                runOnUiThread {
                    gpsLocationReceiver?.updateStatus(status)
                }

    }


    var serviceBinder: GPSReceiverService.GTClientServiceBinder? = null
    val connection = object: ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
            println("Lost connection to Service!")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if(service == null) return
            serviceBinder = service as GPSReceiverService.GTClientServiceBinder
            serviceBinder?.registerListener(clientListener)
            runOnUiThread {
                uiAddress.isEnabled = !(serviceBinder?.service?.client?.isConnected() ?: true)
                uiConnectDisconnect.isChecked = !uiAddress.isEnabled

                if(serviceBinder?.service?.isMockingAvailable == false)
                    showDialogAllowMocking()
            }
        }

    }

    /**
     * Inits UI and connects/starts the GPSReceiverService
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.gt_gpsreceiver_layout) // Load layout

        prefs = getPreferences(Context.MODE_PRIVATE) // Load preferences

        setTitle(R.string.gt_gr_title_app_name) // Set title of App

        // Get UI-Elements:
        uiAddress = findViewById(R.id.gt_gr_address) as EditText
        uiConnectDisconnect = findViewById(R.id.gt_gr_connect_disconnect) as ToggleButton
        uiStatus = findViewById(R.id.gt_gr_status) as TextView

        // Setup UI-Elements:
        uiAddress.setText(prefs.getString("address", "")) // Reads saved address in prefs
        uiConnectDisconnect.setOnClickListener {

            when(uiConnectDisconnect.isChecked) {
                true -> serviceBinder?.service?.client?.connect(uiAddress.text.toString())
                false -> serviceBinder?.service?.client?.disconnect()
            }

        }

        // Load GPS-Details-Fragment:
        gpsLocationReceiver = GPSInfoDetailsFragment()
        loadFragment(gpsLocationReceiver as Fragment)

        // Create, start and bind to Service:
        val service = Intent(this, GPSReceiverService::class.java)
        startService(service)
        bindService(service, connection, Context.BIND_IMPORTANT)
    }

    /**
     * Loads a Fragment into gt_gr_gps_data (FrameHolder)
     */
    fun loadFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.gt_gr_gps_data, fragment)
        fragmentTransaction.commit()
    }

    /**
     * Shown when App cant mock the location
     */
    fun showDialogAllowMocking() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(R.string.gt_gr_title_mocking)
        dialogBuilder.setMessage(R.string.gt_gr_message_mocking)
        dialogBuilder.setNeutralButton(R.string.gt_gr_button_mocking_ok,
                {_, _ ->
                    stopService(Intent(this, GPSReceiverService::class.java))
                    returnToMainActivity() })
        dialogBuilder.setCancelable(false)
        dialogBuilder.show()
    }

    /**
     * Shutdown-Hook
     * - Unbinds from Service
     */
    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }

}