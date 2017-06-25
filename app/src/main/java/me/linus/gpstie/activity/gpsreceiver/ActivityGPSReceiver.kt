package me.linus.gpstie.activity.gpsreceiver

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.MyActivityBase
import me.linus.gpstie.R
import me.linus.gpstie.fragment.GPSDataFragment

class ActivityGPSReceiver: MyActivityBase() {

    lateinit var uiAddress: EditText
    lateinit var uiConnectDisconnect: ToggleButton
    lateinit var uiStatus: TextView

    lateinit var gpsDataFragment: GPSDataFragment

    lateinit var client: GTClient

    lateinit var prefs: SharedPreferences

    lateinit var mockProvider: MockProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.gt_gpsreceiver_layout)

        prefs = getPreferences(Context.MODE_PRIVATE)

        supportActionBar?.title = "GPS-Receiver"

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

        gpsDataFragment = GPSDataFragment()
        loadFragment(gpsDataFragment)

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
                        mockProvider.setEnabled(true)
                    }

            override fun onClientDisconnected() =
                    runOnUiThread {
                        uiAddress.isEnabled = true
                        uiConnectDisconnect.isChecked = false
                        gpsDataFragment.resetLocation()
                        mockProvider.setEnabled(false)
                    }

            override fun onStatusChanged(status: String) =
                    runOnUiThread { uiStatus.text = status }

            override fun onLocationReceived(location: GpsLocation) =
                    runOnUiThread {
                        gpsDataFragment.updateLocation(location)
                        mockProvider.updateLocation(location)
                    }

        }

        client = GTClient(clientListener)

        try {
            mockProvider = MockProvider("tiedGps", this)
        }catch (e: Exception) {
            finish()
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
            mockProvider.setEnabled(false)
            mockProvider.remove()
        }

        super.onDestroy()
    }

}