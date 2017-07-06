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

    lateinit var uiAddress: EditText
    lateinit var uiConnectDisconnect: ToggleButton
    lateinit var uiStatus: TextView

    lateinit var prefs: SharedPreferences

    var gpsLocationReceiver: LocationReceiver? = null

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
                    serviceBinder?.service?.mockProvider?.setEnabled(true)
                    //wakeLock.acquire()
                    serviceBinder?.service?.lockService()
                }

        override fun onClientDisconnected() =
                runOnUiThread {
                    uiAddress.isEnabled = true
                    uiConnectDisconnect.isChecked = false
                    gpsLocationReceiver?.resetLocation()
                    gpsLocationReceiver?.resetStatus()
                    serviceBinder?.service?.mockProvider?.setEnabled(false)
                    //try {
                    //    wakeLock.release()
                    //}catch (e: Exception) {
                        // Occurs when connection failed
                        // So die WakeLock got never acquired but was released
                    //}
                    serviceBinder?.service?.unlockService()
                }

        override fun onClientStatusChanged(status: String) =
                runOnUiThread { uiStatus.text = status }

        override fun onLocationReceived(location: GpsLocation) =
                runOnUiThread {
                    gpsLocationReceiver?.updateLocation(location)
                    serviceBinder?.service?.mockProvider?.updateLocation(location)
                }

        override fun onGpsStatusChanged(status: Int, time: Long) =
                runOnUiThread {
                    serviceBinder?.service?.mockProvider?.updateStatus(status, time)
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
                true -> serviceBinder?.service?.client?.connect(uiAddress.text.toString())
                false -> serviceBinder?.service?.client?.disconnect()
            }

        }

        gpsLocationReceiver = GPSInfoDetailsFragment()
        loadFragment(gpsLocationReceiver as Fragment)

        val service = Intent(this, GPSReceiverService::class.java)
        startService(service)
        bindService(service, connection, Context.BIND_IMPORTANT)
    }

    fun loadFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.gt_gr_gps_data, fragment)
        fragmentTransaction.commit()
    }

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

    override fun onDestroy() {
        unbindService(connection)

        super.onDestroy()
    }

}