package me.linus.gpstie.gpsreceiver

import me.linus.gpstie.*
import me.linus.gpstie.gpssender.GTServer
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.Socket
import java.nio.charset.Charset
import javax.crypto.SecretKey


/**
 * Connects to a server and receives location-updates using an interface
 */
class GTClient(val clientListener: GTClientListener) {

    /**
     * Used to monitor state-changes of this client
     */
    interface GTClientListener {
        fun onClientStatusChanged(statusResId: Int)
        fun onGpsStatusChanged(status: Int, time: Long)
        fun onLocationReceived(location: GpsLocation)
        fun onIncompatibility(messageResId: Int)
        fun onIncorrectPassphrase()
        fun onClientConnecting()
        fun onClientConnected()
        fun onClientDisconnected()
    }

    var client: Socket? = null // Connection to server
    var clientMainThread: Thread? = null // Thread that connects to the server and receives data
    var clientPingingThread: Thread? = null // Thread that sends a ping to keep connection alive
    lateinit var secretKey: SecretKey

    /**
     * Establishes connection to given IP
     */
    fun connect(ip: String, port: Int = GTServer.SERVER_PORT, passphrase: String) {
        if(isConnected()) disconnect() // Disconnect first, if already connected

        // Output information to listener:
        clientListener.onClientConnecting()
        clientListener.onClientStatusChanged(R.string.gt_gr_info_connecting)

        // Create clientMainThread to handle connection and incoming data:
        clientMainThread = Thread {

            try{
                // Create socket
                client = Socket(ip, port)

                // Protocol version exchange and matching (unencrypted):
                var serverProtocolVersion = -1
                try {
                    serverProtocolVersion = DataInputStream(client!!.getInputStream()).readInt()
                } catch(ignored: Exception) {}

                DataOutputStream(client!!.getOutputStream()).writeInt(BuildConfig.PROTOCOL_VERSION)
                client!!.getOutputStream().flush()

                if(BuildConfig.PROTOCOL_VERSION != serverProtocolVersion) {
                    if(BuildConfig.PROTOCOL_VERSION > serverProtocolVersion)
                        clientListener.onIncompatibility(
                                R.string.gt_gr_info_incompatible_server_outdated)
                    else
                        clientListener.onIncompatibility(
                                R.string.gt_gr_info_incompatible_client_outdated)

                    disconnect()
                    return@Thread
                }
                // ------------------------------

                secretKey = generateSecretKey(client!!)
                // At this point, the connection should have succeeded (if not, and Exception would
                // be thrown)

                // Send Passphrase:
                val message = encryptText(passphrase, secretKey)
                DataOutputStream(client!!.getOutputStream()).writeUTF(message)
                // Check success of passphrase
                val authSuccessEnc = DataInputStream(client!!.getInputStream()).readUTF()
                val authSuccess = decryptText(authSuccessEnc, secretKey).toBoolean()

                if (!authSuccess) {
                    clientListener.onIncorrectPassphrase()
                    disconnect()
                    return@Thread
                }
                // ------------------------------

                // Output information to listener:
                clientListener.onClientStatusChanged(R.string.gt_gr_info_connected)
                clientListener.onClientConnected()

                // Create clientPingingThread to keep the connection alive:
                clientPingingThread = Thread {
                    try {

                        val outputStreamWriter = client!!.getOutputStream().bufferedWriter(
                                Charset.forName("UTF-8"))
                        while(true) { // Send Ping every second
                            Thread.sleep(1000)

                            outputStreamWriter.run { // Send Ping to Server
                                write("Ping!")
                                newLine()
                                flush()
                            }
                        }

                    }catch(e: Exception) {
                        // Can be ignored since the same Exception will be thrown in the
                        // clientMainThread which will handle this
                    }
                }.apply { name = "GpsTie-Client-Pinger" }.also { it.start() } // Name + start thread

                // Read incoming data from Server:
                val inputStreamReader = client!!.getInputStream().
                        bufferedReader(Charset.forName("UTF-8"))

                // Loop to read incoming data:
                while(true) {
                    val jsonString = decryptText(inputStreamReader.readLine(), secretKey)

                    // Output information to listener:
                    clientListener.onClientStatusChanged(
                            R.string.gt_gr_info_connected_receiving_data)

                    val json = JSONObject(jsonString) // Create JSON-Object

                    // Check packet-type which was received:
                    when(json.getString("type")) {
                        "location" -> clientListener.onLocationReceived(GpsLocation.fromJson(json))
                        "status" -> clientListener.onGpsStatusChanged(json.getInt("status"),
                                json.getLong("time"))
                        else -> println("Unkown packet received!")
                    }

                }

            }catch (e: Exception) {
                // Connection failed / closed / etc.
                e.printStackTrace()
                disconnect()
            }

        }.apply { name = "GpsTie-Client-Main" } .also { it.start() } // Name and start thread
    }

    /**
     * Checks (very detailed) if connection is established
     */
    fun isConnected() =
            client != null && !client!!.isClosed && client!!.isConnected
                    && !client!!.isInputShutdown && !client!!.isOutputShutdown
                    && clientMainThread != null && clientMainThread!!.isAlive
                    && clientPingingThread != null && clientPingingThread!!.isAlive

    /**
     * This Method does:
     * - Canceling current connection
     * - Ensuring that all threads are closed
     * - Outputting information to listener
     */
    fun disconnect() {
        try { client?.close() } catch (e: Exception) { }
        try { clientMainThread?.stop() } catch (e: Exception) { }
        try { clientPingingThread?.stop() } catch (e: Exception) { }
        client = null
        clientMainThread = null
        clientPingingThread = null
        clientListener.onClientDisconnected()
        clientListener.onClientStatusChanged(R.string.gt_gr_info_disconnected)
    }

    /**
     * Get Ip
     * @return The Ip of the current connection. Will be null if no connection found
     */
    fun getIpConnectedTo(): String? {
        val ip: String? = client?.inetAddress?.hostAddress
        // Removes the port if necessary
        if(client?.inetAddress is Inet4Address) // Address is IPv4
            return if(ip?.contains(":") ?: false) ip!!.split(":")[0] else ip // Remove port
        else // Address is IPv6 or doesn't exists
            return if(ip?.contains("]:") ?: false) ip!!.split("]:")[0] else ip // Remove port
    }

}