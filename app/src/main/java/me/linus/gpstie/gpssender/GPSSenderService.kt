package me.linus.gpstie.gpssender

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import me.linus.gpstie.LocationReceiver
import me.linus.gpstie.R
import me.linus.gpstie.getLocalIp
import java.util.*

class GPSSenderService: Service() {

    companion object {
        val NOTIFICATION_ID = 102
    }

    var isBound = false // Is true if any Activity is connected to this Service

    val handler = Handler() // Running actions in another thread
    val binder = GPSSenderServiceBinder(this) // Binder for activities

    lateinit var gpsApi: GPSSenderServiceGpsApi // GPS-Api for getting GPS-Locations

    var assignableListener: GTServer.GTServerListener? = null // For registering

    val server = GTServer(object: GTServer.GTServerListener {
        override fun onServerStatusChanged(statusResId: Int) {
            assignableListener?.onServerStatusChanged(statusResId)
        }

        override fun onServerStatusChanged(statusResId: Int, quantity: Int) {
            assignableListener?.onServerStatusChanged(statusResId, quantity)
        }

        override fun onServerStarted() {
            assignableListener?.onServerStarted()
            lockService()
        }

        override fun onServerStopped() {
            assignableListener?.onServerStopped()
            unlockService()
        }

        override fun onClientConnected(clientConnection: GTConnection, server: GTServer) {
            assignableListener?.onClientConnected(clientConnection, server)
            handler.post {
                gpsApi.gpsEnabled = true // This calls a function. Will be ignored if already true
            }

            // Send new Client the latest data:
            if(gpsApi.lastStatus != GPSSenderServiceGpsApi.GPS_STATUS_DONT_SEND)
                server.updateStatus(gpsApi.lastStatus, singleReceiver = clientConnection)
            if(gpsApi.lastLocation != null)
                server.updateLocation(gpsApi.lastLocation!!, singleReceiver = clientConnection)
        }

        override fun onClientDisconnected(clientConnection: GTConnection, server: GTServer) {
            assignableListener?.onClientDisconnected(clientConnection, server)
            if(server.getClients().isEmpty())
                handler.post {
                    gpsApi.gpsEnabled = false
                }
        }

    })

    var lastLocalIp: String? = null // Last know local Ip

    val networkActionReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val newIp = getLocalIp()
            if(lastLocalIp == newIp) return
            lastLocalIp = newIp

            if(server.isRunning())
                server.stop()
        }

    }

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
     * Startup-Hook
     */
    override fun onCreate() {
        super.onCreate()

        gpsApi = GPSSenderServiceGpsApi(
                baseContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
                server)
        lastLocalIp = getLocalIp()
        registerReceiver(networkActionReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    /**
     * Operation-Mode of this Service
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
            Service.START_NOT_STICKY // Do not restart if Service gets destroyed/stopped

    /**
     * Shutdown-Hook
     */
    override fun onDestroy() {
        gpsApi.gpsEnabled = false
        server.stop()
        super.onDestroy()
    }

    /**
     * Remove everything associated with bound Activties
     */
    fun cleanUp() {
        gpsApi.assignableLocationListener = null
        gpsApi.assignableLocationReceiver = null
        assignableListener = null
    }

    class GPSSenderServiceBinder(val service: GPSSenderService): Binder() {

        fun registerLocationReceiver(locationReceiver: LocationReceiver) {
            service.gpsApi.assignableLocationReceiver = locationReceiver
        }
        
        fun registerLocationListener(locationListener: LocationListener) {
            service.gpsApi.assignableLocationListener = locationListener
        }

        fun registerServerListener(serverListener: GTServer.GTServerListener) {
            service.assignableListener = serverListener
        }

        fun setPassphrase(passphrase: String) {
            service.server.passphrase = passphrase
        }

        fun startServer() = service.server.start()

        fun stopServer() = service.server.stop()

        fun isServerRunning(): Boolean = service.server.isRunning()

    }

    /**
     * Locks Service as Foreground-Service
     */
    fun lockService() {
        val notificationBuilder = Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_gpssender)
                .setContentTitle("GPS-Sender")
                .setContentText("Server is running...")

        // Open ActivityGPSSender on click:
        // thanks to https://stackoverflow.com/a/38107532/3949509
        val launchIntent = Intent(this, ActivityGPSSender::class.java)
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