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
        val NOTIFICATION_ID = 101 // Id for the notification used to hold this service in foreground
    }

    var isBound = false // Is true if any Activity is connected to this Service

    val binder = GTClientServiceBinder(this) // Binder for Activities

    var mockProvider: MockProvider? = null // Used to mock the location
    var isMockingAvailable = false // Is true if mockProvider is started successfully

    val client: GTClient = GTClient(object: GTClient.GTClientListener {

        override fun onClientStatusChanged(status: String) {
            assignableClientListener?.onClientStatusChanged(status)
        }
        override fun onClientConnecting() {
            assignableClientListener?.onClientConnecting()
        }

        override fun onGpsStatusChanged(status: Int, time: Long) {
            assignableClientListener?.onGpsStatusChanged(status, time)
            mockProvider?.updateStatus(status, time)
        }

        override fun onLocationReceived(location: GpsLocation) {
            assignableClientListener?.onLocationReceived(location)
            mockProvider?.updateLocation(location)
        }


        override fun onClientConnected() {
            assignableClientListener?.onClientConnected()
            mockProvider?.setEnabled(true)
            lockService()
        }

        override fun onClientDisconnected() {
            assignableClientListener?.onClientDisconnected()
            mockProvider?.setEnabled(false)
            unlockService()
        }

    })

    // Used by bound Activity for reacting
    var assignableClientListener: GTClient.GTClientListener? = null

    /**
     * Startup-Hook
     */
    override fun onCreate() {
        super.onCreate()

        // Initialize mockProvider:
        try {
            mockProvider = MockProvider(LocationManager.GPS_PROVIDER, this)
            isMockingAvailable = true
        }catch (e: Exception) {
            e.printStackTrace()
            isMockingAvailable = false
        }
    }

    /**
     * Operation-Mode of this Service
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
            Service.START_NOT_STICKY // Do not restart if Service gets destroyed/stopped

    override fun onBind(intent: Intent?): IBinder {
        isBound = true
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        cleanUp()
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        isBound = true
    }

    /**
     * Remove everything associated with bound Activties
     */
    fun cleanUp() {
        assignableClientListener = null
    }

    /**
     * Binder for Activities. Used to communicate with this Service
     */
    class GTClientServiceBinder(val service: GPSReceiverService): Binder() {

        fun registerListener(listener: GTClient.GTClientListener) {
            service.assignableClientListener = listener
        }

    }

    /**
     * Shutdown-Hook
     */
    override fun onDestroy() {
        if(mockProvider != null) {
            mockProvider!!.setEnabled(false)
            mockProvider!!.remove()
        }
        super.onDestroy()
    }

    /**
     * Locks Service as Foreground-Service
     */
    fun lockService() {
        val notificationBuilder = Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_gpsreceiver)
                .setContentTitle("GPS-Receiver")
                .setContentText("Connected to ${client.getIpConnectedTo()}")

        // Open ActivityGPSReceiver on click:
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

    /**
     * Removes Service from foreground
     */
    fun unlockService() {
        stopForeground(true)
        if(!isBound)
            stopSelf()
    }

}