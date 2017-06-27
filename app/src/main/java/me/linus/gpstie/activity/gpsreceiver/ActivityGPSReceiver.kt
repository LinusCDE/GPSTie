package me.linus.gpstie.activity.gpsreceiver

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.MyActivityBase
import me.linus.gpstie.R
import me.linus.gpstie.LocationReceiver
import me.linus.gpstie.fragment.GPSInfoDetailsFragment

class ActivityGPSReceiver: MyActivityBase() {

    lateinit var uiAddress: EditText
    lateinit var uiConnectDisconnect: ToggleButton
    lateinit var uiStatus: TextView

    var gpsLocationReceiver: LocationReceiver? = null

    lateinit var client: GTClient

    lateinit var prefs: SharedPreferences

    var mockProvider: MockProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.gt_gpsreceiver_layout)

        prefs = getPreferences(Context.MODE_PRIVATE)

        setTitle(R.string.gt_gr_title_app_name)

        uiAddress = findViewById(R.id.gt_gr_address) as EditText
        uiConnectDisconnect = findViewById(R.id.gt_gr_connect_disconnect) as ToggleButton
        uiStatus = findViewById(R.id.gt_gr_status) as TextView

        uiAddress.setText(prefs.getString("address", ""))

        uiConnectDisconnect.setOnClickListener {

            when(uiConnectDisconnect.isChecked) {
                true -> client.connect(uiAddress.text.toString())
                false -> client.disconnect()
            }

        }

        gpsLocationReceiver = GPSInfoDetailsFragment()
        loadFragment(gpsLocationReceiver as Fragment)

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
                        mockProvider?.setEnabled(true)
                        wakeLock.acquire()
                    }

            override fun onClientDisconnected() =
                    runOnUiThread {
                        uiAddress.isEnabled = true
                        uiConnectDisconnect.isChecked = false
                        gpsLocationReceiver?.resetLocation()
                        gpsLocationReceiver?.resetStatus()
                        mockProvider?.setEnabled(false)
                        try {
                            wakeLock.release()
                        }catch (e: Exception) {
                            // Occurs when connection failed
                            // So die WakeLock got never acquired but was released
                        }
                    }

            override fun onClientStatusChanged(status: String) =
                    runOnUiThread { uiStatus.text = status }

            override fun onLocationReceived(location: GpsLocation) =
                    runOnUiThread {
                        gpsLocationReceiver?.updateLocation(location)
                        mockProvider?.updateLocation(location)
                    }

            override fun onGpsStatusChanged(status: Int, time: Long) =
                    runOnUiThread {
                        mockProvider?.updateStatus(status, time)
                        gpsLocationReceiver?.updateStatus(status)
                    }

        }

        client = GTClient(clientListener)

        try {
            mockProvider = MockProvider(LocationManager.GPS_PROVIDER, this)
        }catch (e: Exception) {
            e.printStackTrace()
            showDialogAllowMocking()
        }
    }

    fun loadFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.gt_gr_gps_data, fragment)
        fragmentTransaction.commit()
    }

    override fun onDestroy() {
        if(mockProvider != null) {
            mockProvider?.setEnabled(false)
            mockProvider?.remove()
        }

        super.onDestroy()
    }

    fun showDialogAllowMocking() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(R.string.gt_gr_title_mocking)
        dialogBuilder.setMessage(R.string.gt_gr_message_mocking)
        dialogBuilder.setNeutralButton(R.string.gt_gr_button_mocking_ok,
                {_, _ -> returnToMainActivity() })
        dialogBuilder.setCancelable(false)
        dialogBuilder.show()
    }

}