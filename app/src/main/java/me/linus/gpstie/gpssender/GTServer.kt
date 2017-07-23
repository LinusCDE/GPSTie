package me.linus.gpstie.gpssender

import android.location.Location
import android.util.Base64
import me.linus.gpstie.GpsLocation
import org.json.JSONObject
import java.net.ServerSocket
import java.nio.charset.Charset

class GTServer(val serverListener: GTServerListener) {

    /**
     * Used to monitor state-changes of this Server or any connected clients
     */
    interface GTServerListener {
        fun onServerStatusChanged(status: String)
        fun onServerStarted()
        fun onServerStopped()
        fun onClientConnected(clientConnection: GTConnection, server: GTServer)
        fun onClientDisconnected(clientConnection: GTConnection, server: GTServer)
    }

    companion object {
        val SERVER_PORT = 46832 // Default Server-Port
    }

    var serverThread: Thread? = null // Server-Thread (obvious or not? ;) )
    var serverSocket: ServerSocket? = null // Server-Socket (also obvious)
    val connectedClients = ArrayList<GTConnection>() // All active connections

    /**
     * Starts the server
     */
    fun start() {
        if(isRunning()) stop() // Makes sure that the server is not running already

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

    /**
     * Checks if the Server is running
     */
    fun isRunning(): Boolean =
    serverThread != null && serverThread!!.isAlive
    && serverSocket != null && !serverSocket!!.isClosed

    /**
     * Stops the server and makes sure that all connections get shut down
     */
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

    /**
     * Creates an Location-Packet and sends it to all clients
     */
    fun updateLocation(location: Location, singleReceiver: GTConnection? = null) {
        val jsonObj = GpsLocation.toJson(location)
        jsonObj.put("type", "location") // Set packet-type

        val packetLine = Base64.encodeToString(
                jsonObj.toString().toByteArray(Charset.forName("UTF-8")), Base64.NO_WRAP)

        singleReceiver?.send(packetLine) ?: getClients().forEach { it.send(packetLine) }
    }

    /**
     * Creates a Status-Packet and sends it to all clients
     */
    fun updateStatus(status: Int, singleReceiver: GTConnection? = null) {
        val jsonObj = JSONObject()
        jsonObj.put("type", "status") // Packet type
        jsonObj.put("status", status)
        jsonObj.put("time", System.currentTimeMillis())

        val packetLine = Base64.encodeToString(
                jsonObj.toString().toByteArray(Charset.forName("UTF-8")), Base64.NO_WRAP)

        singleReceiver?.send(packetLine) ?: getClients().forEach { it.send(packetLine) }
    }

    /**
     * Outputs connected-client-count to serverListener
     */
    fun updateClientCount() = serverListener.onServerStatusChanged(
            "Running. ${connectedClients.size} " + "Client(s) connected")

    /**
     * Returns all connected clients as an Array (prevents Exceptions when removing clients from
     * list while iterating trough it)
     */
    fun getClients(): Array<GTConnection> = connectedClients.toArray(arrayOf<GTConnection>())

}