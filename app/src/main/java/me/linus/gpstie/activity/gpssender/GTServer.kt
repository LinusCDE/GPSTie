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

    class GTConnection(val gtServer: GTServer, val client: Socket) {

        val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), "UTF-8"))
        val clientInputThread: Thread

        init {
            client.soTimeout = 30000 // = Timeout of 30 seconds

            clientInputThread = Thread {
                try {
                    val inStream = client.getInputStream()

                    val buffer = ByteArray(24)
                    while (inStream.read(buffer) > 0) {
                        Thread.sleep(1)
                    }

                    disconnect()
                }catch(e: Exception) {
                    disconnect()
                }
            }.apply { name = "GpsTie-Server-ClientInputThread" } .also { it.start() }
        }

        fun disconnect() {
            gtServer.connectedClients.remove(this)
            gtServer.updateClientCount()
            try { clientInputThread.stop() }catch(e: Exception) { }
            try { client.close() }catch(e: Exception) { }
        }

        fun send(message: String) {
            try {
                writer.write(message)
                writer.newLine()
                writer.flush()
            }catch (e: Exception) {
                disconnect()
            }
        }

    }

    companion object val SERVER_PORT = 46832

    var serverThread: Thread? = null
    var serverSocket: ServerSocket? = null
    val connectedClients = ArrayList<GTConnection>()

    fun start() {
        if(isRunning()) stop() // Making sure, server is not running already

        serverThread = Thread {

            try {
                serverListener?.onStatusChanged("Starting...")

                serverSocket = ServerSocket(SERVER_PORT)

                serverListener?.onStatusChanged("Running. Waiting for clients...")

                while (true) {
                    val socket = serverSocket?.accept()
                    if (socket == null) {
                        serverSocket?.close()
                        return@Thread
                    }
                    connectedClients.add(GTConnection(this, socket))

                    updateClientCount()
                }

                serverSocket?.close()
            }catch(e: Exception){
                serverListener?.onStatusChanged("Server stopped.")
            }
        }.apply { name = "GpsTie-ServerThread" }.also { it.start() }
    }

    fun isRunning(): Boolean =
    serverThread != null && serverThread!!.isAlive
    && serverSocket != null && !serverSocket!!.isClosed

    fun stop() {
        serverListener?.onStatusChanged("Stopping Server...")
        for(connection in getClients())
            connection.disconnect()
        serverSocket?.close()
        try {
            if (serverThread != null && serverThread!!.isAlive)
                serverThread?.stop()
        }catch(e: Exception) { }
        connectedClients.clear()
        serverListener?.onStatusChanged("Server stopped.")
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
            serverListener?.onStatusChanged("Running. ${connectedClients.size} " +
                    "Client(s) connected")


    fun getClients(): Array<GTConnection>
            = connectedClients.toArray(arrayOf<GTConnection>())

}