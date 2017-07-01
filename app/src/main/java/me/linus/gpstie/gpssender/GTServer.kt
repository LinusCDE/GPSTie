package me.linus.gpstie.gpssender

import android.location.Location
import android.util.Base64
import org.json.JSONObject
import java.net.ServerSocket
import java.nio.charset.Charset

class GTServer(val serverListener: GTServerListener) {

    interface GTServerListener {
        fun onServerStatusChanged(status: String)
        fun onServerStarted()
        fun onServerStopped()
        fun onClientConnected(clientConnection: GTConnection, server: GTServer)
        fun onClientDisconnected(clientConnection: GTConnection, server: GTServer)
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
                serverListener.onServerStatusChanged("Starting...")

                serverSocket = ServerSocket(SERVER_PORT)

                serverListener.onServerStatusChanged("Running. Waiting for clients...")
                serverListener.onServerStarted()

                while (true) {
                    val socket = serverSocket!!.accept()
                    if (socket == null) {
                        serverSocket?.close()
                        return@Thread
                    }
                    val connection = GTConnection(this, socket)
                    connectedClients.add(connection)
                    updateClientCount()
                    serverListener.onClientConnected(connection, this)
                }

            }catch(e: Exception){
                stop()
                e.printStackTrace()
                serverListener.onServerStatusChanged("Server stopped.")
            }
        }.apply { name = "GpsTie-ServerThread" }.also { it.start() }
    }

    fun isRunning(): Boolean =
    serverThread != null && serverThread!!.isAlive
    && serverSocket != null && !serverSocket!!.isClosed

    fun stop() {
        serverListener.onServerStatusChanged("Stopping Server...")
        for(connection in getClients())
            connection.disconnect()
        serverSocket?.close()
        try {
            if (serverThread != null && serverThread!!.isAlive)
                serverThread?.stop()
        }catch(e: Exception) { }
        connectedClients.clear()
        serverListener.onServerStatusChanged("Server stopped.")
        serverListener.onServerStopped()
    }

    fun updateLocation(location: Location, singleReceiver: GTConnection? = null) {
        val jsonObj = JSONObject()
        jsonObj.put("type", "location") // Packet type
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

        if(singleReceiver != null)
            singleReceiver.send(packetLine)
        else
            getClients().forEach { it.send(packetLine) }    }

    fun updateStatus(status: Int, singleReceiver: GTConnection? = null) {
        val jsonObj = JSONObject()
        jsonObj.put("type", "status") // Packet type
        jsonObj.put("status", status)
        jsonObj.put("time", System.currentTimeMillis())

        val packetLine = Base64.encodeToString(
                jsonObj.toString().toByteArray(Charset.forName("UTF-8")), Base64.NO_WRAP)

        if(singleReceiver != null)
            singleReceiver.send(packetLine)
        else
            getClients().forEach { it.send(packetLine) }
    }

    fun updateClientCount() =
            serverListener.onServerStatusChanged("Running. ${connectedClients.size} " +
                    "Client(s) connected")


    fun getClients(): Array<GTConnection>
            = connectedClients.toArray(arrayOf<GTConnection>())

}