package me.linus.gpstie.gpsreceiver

import android.util.Base64
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.gpssender.GTServer
import org.json.JSONObject
import java.net.Socket
import java.nio.charset.Charset

class GTClient(var clientListener: GTClientListener) {

    interface GTClientListener {
        fun onClientStatusChanged(status: String)
        fun onGpsStatusChanged(status: Int, time: Long)
        fun onLocationReceived(location: GpsLocation)
        fun onClientConnecting()
        fun onClientConnected()
        fun onClientDisconnected()
    }

    var client: Socket? = null
    var clientMainThread: Thread? = null
    var clientPingingThread: Thread? = null
    var connectedToIp: String? = null
    var connectedToPort: Int = -1

    fun connect(ip: String, port: Int = GTServer.SERVER_PORT) {
        connectedToIp = ip
        connectedToPort = port
        clientListener.onClientConnecting()
        clientListener.onClientStatusChanged("Connecting...")
        if(isConnected()) disconnect()

        clientMainThread = Thread {

            try{
                client = Socket(ip, port)

                clientPingingThread = Thread {
                    try {

                        val writer = client!!.getOutputStream().bufferedWriter(
                                Charset.forName("UTF-8"))
                        while(true) {
                            Thread.sleep(1000)
                            writer.write("Ping!")
                            writer.newLine()
                            writer.flush()
                        }

                    }catch(e: Exception) { }
                }.apply { name = "GpsTie-Client-Pinger" }.also { it.start() }

                val reader = client!!.getInputStream().bufferedReader(Charset.forName("UTF-8"))
                clientListener.onClientStatusChanged("Connected.")
                clientListener.onClientConnected()

                while(true) {
                    val line = reader.readLine() // Will receive a Base64-String
                    if(line == null) {
                        disconnect()
                        return@Thread
                    }
                    clientListener.onClientStatusChanged("Connected. Receiving Data...")

                    val jsonStr = String(
                            Base64.decode(line, Base64.NO_WRAP), Charset.forName("UTF-8"))
                    val json = JSONObject(jsonStr)

                    when(json.getString("type")) {
                        "location" -> clientListener.onLocationReceived(GpsLocation.fromJson(json))
                        "status" -> clientListener.onGpsStatusChanged(json.getInt("status"),
                                json.getLong("time"))
                        else -> println("Unkown packet received!")
                    }

                }

            }catch (e: Exception) {
                clientListener.onClientStatusChanged("Connection failed: ${e.message}")
                disconnect()
            }

        }.apply { name = "GpsTie-Client-Main" } .also { it.start() }
    }

    fun isConnected() =
            client != null && !client!!.isClosed && client!!.isConnected
                    && !client!!.isInputShutdown && !client!!.isOutputShutdown
                    && clientMainThread != null && clientMainThread!!.isAlive
                    && clientPingingThread != null && clientPingingThread!!.isAlive

    fun disconnect() {
        connectedToIp = null
        connectedToPort = -1
        try { client?.close() } catch (e: Exception) { }
        try { clientMainThread?.stop() } catch (e: Exception) { }
        try { clientPingingThread?.stop() } catch (e: Exception) { }
        client = null
        clientMainThread = null
        clientPingingThread = null
        clientListener.onClientDisconnected()
        clientListener.onClientStatusChanged("Disconnected")
    }

}