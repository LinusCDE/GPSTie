package me.linus.gpstie.activity.gpssender

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.PowerManager
import android.text.format.Formatter
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.ToggleButton
import me.linus.gpstie.MyActivityBase
import me.linus.gpstie.R
import java.net.NetworkInterface

class ActivityGPSSender: MyActivityBase() {

    lateinit var uiServerStartStop: ToggleButton
    lateinit var uiServerAddress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gt_gpssender_layout)

        // Display Ip-Address:
        val refreshIp = {
            uiServerAddress.text = getLocalIp() ?: "Schalte Wlan oder Hotspot an!"
        }

        registerReceiver(object: BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                val oldIp: String = uiServerAddress.text.toString()
                val newIp = getLocalIp()

                if(oldIp == newIp) return

                refreshIp()
            }

        }, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        supportActionBar?.title = "GPS-Sender"

        uiServerStartStop = findViewById(R.id.gt_gs_toggleServer) as ToggleButton
        uiServerAddress = findViewById(R.id.gt_gs_serverAddress) as TextView

        refreshIp()
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

}