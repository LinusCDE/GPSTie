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

    var isBound = false

    val client: GTClient = GTClient(GTClientDummyListener())
    val binder = GTClientServiceBinder(this)

    var mockProvider: MockProvider? = null
    var isMockingAvailable = false

    fun lockService() {
        val notificationBuilder = Notification.Builder(this)
        notificationBuilder.setSmallIcon(R.drawable.ic_stat_gpsreceiver)
        notificationBuilder.setContentTitle("GPS-Receiver")
        notificationBuilder.setContentText("Connected to ${client.connectedToIp}")

        // Open ActivityGPSReceiver at click:
        // thanks to https://stackoverflow.com/a/38107532/3949509
        val launchIntent = Intent(this, ActivityGPSReceiver::class.java)
        launchIntent.action = Intent.ACTION_MAIN
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.setContentIntent(pendingIntent)
        // -------------------------------------

        val notification = notificationBuilder.build()
        startForeground(NOTIFICATION_ID, notification)
    }

    fun unlockService() {
        stopForeground(true)
        if(!isBound)
            stopSelf()
    }

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

    override fun onBind(intent: Intent?): IBinder {
        isBound = true
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        isBound = true
    }

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