package me.linus.gpstie.gpssender

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import me.linus.gpstie.LocationReceiver
import me.linus.gpstie.R
import me.linus.gpstie.getLocalIp

class GPSSenderService: Service() {

    companion object {
        val NOTIFICATION_ID = 102
    }

    val handler = Handler()

    val binder = GPSSenderServiceBinder(this)
    
    var initialized = false

    lateinit var gpsApi: GPSSenderServiceGpsApi

    // For registering
    var assignableListener: GTServer.GTServerListener? = null

    val server = GTServer(object: GTServer.GTServerListener {
        override fun onServerStatusChanged(status: String) {
            assignableListener?.onServerStatusChanged(status)
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
                    gpsApi.gpsEnabled = false // This calls a function.
                }
        }

    })

    var lastIp: String? = null

    val networkActionReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val newIp = getLocalIp()
            if(lastIp == newIp) return
            lastIp = newIp

            if(server.isRunning())
                server.stop()
        }


    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!initialized) {
            gpsApi = GPSSenderServiceGpsApi(
                    baseContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
                    server)
            lastIp = getLocalIp()
            registerReceiver(networkActionReceiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

            initialized = true
        }

        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        gpsApi.gpsEnabled = false
        server.stop()
        super.onDestroy()
    }

    class GPSSenderServiceBinder(val service: GPSSenderService): Binder() {

        fun registerLocationReceiver(locationReceiver: LocationReceiver) {
            service.gpsApi.assignableLocationReceiver = locationReceiver
        }
        
        fun unregisterLocationReceiver() {
            service.gpsApi.assignableLocationReceiver = null
        }
        
        fun registerLocationListener(locationListener: LocationListener) {
            service.gpsApi.assignableLocationListener = locationListener
        }
        
        fun unregisterLocationListener() {
            service.gpsApi.assignableLocationListener = null
        }

        fun registerServerListener(serverListener: GTServer.GTServerListener) {
            service.assignableListener = serverListener
        }

        fun unregisterServerListener() {
            service.assignableListener = null
        }

        fun startServer() = service.server.start()

        fun stopServer() = service.server.stop()

        fun isServerRunning(): Boolean = service.server.isRunning()

    }

    fun lockService() {
        val notifcationBuilder = Notification.Builder(this)
        notifcationBuilder.setSmallIcon(R.drawable.ic_gpssender_grey_500_24dp)
        notifcationBuilder.setContentTitle("GPS-Sender")
        notifcationBuilder.setContentText("Server is running...")

        // Open ActivityGPSSender at click:
        // thanks to https://stackoverflow.com/a/38107532/3949509
        val launchIntent = Intent(this, ActivityGPSSender::class.java)
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

}