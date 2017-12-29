package me.linus.gpstie.gpssender

import android.location.Location
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.R
import org.json.JSONObject
import java.net.ServerSocket

class GTServer(val serverListener: GTServerListener) {

    /**
     * Used to monitor state-changes of this Server or any connected clients
     */
    interface GTServerListener {
        fun onServerStatusChanged(statusResId: Int)
        fun onServerStatusChanged(statusResId: Int, quantity: Int)
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
                serverListener.onServerStatusChanged(R.string.gt_gs_info_starting)
                //System.setProperty("javax.net.ssl.keyStore", "keystore")
                //System.setProperty("javax.net.ssl.keyStorePassword", "password")
                serverSocket = ServerSocket(SERVER_PORT)

                serverListener.onServerStatusChanged(R.string.gt_gs_info_connection_amount_none)
                serverListener.onServerStarted()

                while (true) {
                    val socket = serverSocket!!.accept()
                    if (socket == null) {
                        serverSocket?.close()
                        return@Thread
                    }

                    try {
                        val connection = GTConnection(this, socket)
                        connectedClients.add(connection)
                        updateClientCount()
                        serverListener.onClientConnected(connection, this)
                    } catch(ignored: Exception) {}
                }

            }catch(e: Exception){
                stop()
                e.printStackTrace()
                serverListener.onServerStatusChanged(R.string.gt_gs_info_stopped)
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
        serverListener.onServerStatusChanged(R.string.gt_gs_info_stopping)
        for(connection in getClients())
            connection.disconnect()
        serverSocket?.close()
        try {
            if (serverThread != null && serverThread!!.isAlive)
                serverThread?.stop()
        }catch(e: Exception) { }
        connectedClients.clear()
        serverListener.onServerStatusChanged(R.string.gt_gs_info_stopped)
        serverListener.onServerStopped()
    }

    /**
     * Creates an Location-Packet and sends it to all clients
     */
    fun updateLocation(location: Location, singleReceiver: GTConnection? = null) {
        val jsonObj = GpsLocation.toJson(location)
        jsonObj.put("type", "location") // Set packet-type

        val jsonString = jsonObj.toString()
        singleReceiver?.send(jsonString) ?: getClients().forEach { it.send(jsonString) }
    }

    /**
     * Creates a Status-Packet and sends it to all clients
     */
    fun updateStatus(status: Int, singleReceiver: GTConnection? = null) {
        val jsonObj = JSONObject()
        jsonObj.put("type", "status") // Packet type
        jsonObj.put("status", status)
        jsonObj.put("time", System.currentTimeMillis())

        val jsonString = jsonObj.toString()
        singleReceiver?.send(jsonString) ?: getClients().forEach { it.send(jsonString) }
    }

    /**
     * Outputs connected-client-count to serverListener
     */
    fun updateClientCount() {
        val amount = connectedClients.size
        if(amount == 0)
            serverListener.onServerStatusChanged(R.string.gt_gs_info_connection_amount_none)
        else
            serverListener.onServerStatusChanged(
                    R.plurals.gt_gs_info_connection_amount, connectedClients.size)
    }

    /**
     * Returns all connected clients as an Array (prevents Exceptions when removing clients from
     * list while iterating trough it)
     */
    fun getClients(): Array<GTConnection> = connectedClients.toArray(arrayOf<GTConnection>())

}