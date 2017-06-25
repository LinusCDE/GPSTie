package me.linus.gpstie.activity.gpssender

import android.location.Location
import android.util.Base64
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset

class GTServer(val serverListener: GTServerListener) {

    interface GTServerListener {
        fun onStatusChanged(status: String)
        fun onServerStarted()
        fun onServerStopped()
    }

    companion object {
        val SERVER_PORT = 46832
    }

    var serverThread: Thread? = null
    var serverSocket: ServerSocket? = null
    val connectedClients = ArrayList<GTConnection>()

    fun start() {
        if(isRunning()) stop() // Making sure, server is not running already

        serverThread = Thread {

            try {
                serverListener.onStatusChanged("Starting...")

                serverSocket = ServerSocket(SERVER_PORT)

                serverListener.onStatusChanged("Running. Waiting for clients...")
                serverListener.onServerStarted()

                while (true) {
                    val socket = serverSocket!!.accept()
                    if (socket == null) {
                        serverSocket?.close()
                        return@Thread
                    }
                    connectedClients.add(GTConnection(this, socket))

                    updateClientCount()
                }

            }catch(e: Exception){
                serverListener.onStatusChanged("Server stopped.")
            }
        }.apply { name = "GpsTie-ServerThread" }.also { it.start() }
    }

    fun isRunning(): Boolean =
    serverThread != null && serverThread!!.isAlive
    && serverSocket != null && !serverSocket!!.isClosed

    fun stop() {
        serverListener.onStatusChanged("Stopping Server...")
        for(connection in getClients())
            connection.disconnect()
        serverSocket?.close()
        try {
            if (serverThread != null && serverThread!!.isAlive)
                serverThread?.stop()
        }catch(e: Exception) { }
        connectedClients.clear()
        serverListener.onStatusChanged("Server stopped.")
        serverListener.onServerStopped()
    }

    fun updateLocation(location: Location) {
        val jsonObj = JSONObject()
        jsonObj.put("latitude", location.latitude)
        jsonObj.put("longitude", location.longitude)
        jsonObj.put("accuracy", location.accuracy)
        jsonObj.put("altitude", location.altitude)
        jsonObj.put("bearing", location.bearing)
        jsonObj.put("speed", location.speed)
        jsonObj.put("provider", location.provider)
        jsonObj.put("elapsedRealtimeNanos", location.elapsedRealtimeNanos)
        jsonObj.put("time", location.time)
        jsonObj.put("hasAccuracy", location.hasAccuracy())
        jsonObj.put("hasAltitude", location.hasAltitude())
        jsonObj.put("hasBearing", location.hasBearing())
        jsonObj.put("hasSpeed", location.hasSpeed())

        val packetLine = Base64.encodeToString(
                jsonObj.toString().toByteArray(Charset.forName("UTF-8")), Base64.NO_WRAP)
        for(connection in getClients()) {
            connection.send(packetLine)
        }
    }

    fun updateClientCount() =
            serverListener.onStatusChanged("Running. ${connectedClients.size} " +
                    "Client(s) connected")


    fun getClients(): Array<GTConnection>
            = connectedClients.toArray(arrayOf<GTConnection>())

}