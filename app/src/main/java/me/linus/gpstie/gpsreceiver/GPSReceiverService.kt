package me.linus.gpstie.gpsreceiver

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.R

class GPSReceiverService : Service() {

    companion object {
        val NOTIFICATION_ID = 101
    }

    class GTClientDummyListener: GTClient.GTClientListener {
        override fun onClientConnecting() {}
        override fun onClientConnected() {}
        override fun onClientDisconnected() {}
        override fun onClientStatusChanged(status: String) {}
        override fun onLocationReceived(location: GpsLocation) {}
        override fun onGpsStatusChanged(status: Int, time: Long) {}
    }

    val client: GTClient = GTClient(GTClientDummyListener())
    val binder = GTClientServiceBinder(this)

    var mockProvider: MockProvider? = null
    var isMockingAvailable = false

    fun lockService() {
        val notifcationBuilder = Notification.Builder(this)
        notifcationBuilder.setSmallIcon(R.drawable.ic_stat_gpsreceiver)
        notifcationBuilder.setContentTitle("GPS-Receiver")
        notifcationBuilder.setContentText("Connected to ${client.connectedToIp}")

        // Open ActivityGPSReceiver at click:
        // thanks to https://stackoverflow.com/a/38107532/3949509
        val launchIntent = Intent(this, ActivityGPSReceiver::class.java)
        launchIntent.action = Intent.ACTION_MAIN
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        notifcationBuilder.setContentIntent(pendingIntent)
        // -------------------------------------

        val notification = notifcationBuilder.build()
        startForeground(NOTIFICATION_ID, notification)
    }

    fun unlockService() = stopForeground(true)

    override fun onCreate() {
        super.onCreate()

        println("GTClientService onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(mockProvider == null) {
            try {
                mockProvider = MockProvider(LocationManager.GPS_PROVIDER, this)
                isMockingAvailable = true
            }catch (e: Exception) {
                e.printStackTrace()
                isMockingAvailable = false
            }
        }
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    class GTClientServiceBinder(val service: GPSReceiverService): Binder() {

        fun registerListener(listener: GTClient.GTClientListener) {
            service.client.clientListener = listener
        }

    }

    override fun onDestroy() {
        if(mockProvider != null) {
            mockProvider?.setEnabled(false)
            mockProvider?.remove()
        }
        println("GTClientService onDestroy()")

        super.onDestroy()
    }

}