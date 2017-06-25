package me.linus.gpstie.activity.gpsreceiver

import android.util.Base64
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.activity.gpssender.GTServer
import org.json.JSONObject
import java.net.Socket
import java.nio.charset.Charset

class GTClient(val clientListener: GTClientListener) {

    interface GTClientListener {
        fun onStatusChanged(status: String)
        fun onLocationReceived(location: GpsLocation)
        fun onClientConnecting()
        fun onClientConnected()
        fun onClientDisconnected()
    }

    var client: Socket? = null
    var clientMainThread: Thread? = null
    var clientPingingThread: Thread? = null

    fun connect(ip: String, port: Int = GTServer.SERVER_PORT) {
        clientListener.onClientConnecting()
        clientListener.onStatusChanged("Connecting...")
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
                clientListener.onStatusChanged("Connected.")
                clientListener.onClientConnected()

                while(true) {
                    val line = reader.readLine() // Will receive a Base64-String
                    if(line == null) {
                        disconnect()
                        return@Thread
                    }
                    clientListener.onStatusChanged("Connected. Receiving Data...")

                    val jsonStr = String(
                            Base64.decode(line, Base64.NO_WRAP), Charset.forName("UTF-8"))
                    val location = GpsLocation.fromJson(JSONObject(jsonStr))

                    clientListener.onLocationReceived(location)
                }

            }catch (e: Exception) {
                clientListener.onStatusChanged("Connection failed: ${e.message}")
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
        try { client?.close() } catch (e: Exception) { }
        try { clientMainThread?.stop() } catch (e: Exception) { }
        try { clientPingingThread?.stop() } catch (e: Exception) { }
        client = null
        clientMainThread = null
        clientPingingThread = null
        clientListener.onClientDisconnected()
        clientListener.onStatusChanged("Disconnected")
    }

}