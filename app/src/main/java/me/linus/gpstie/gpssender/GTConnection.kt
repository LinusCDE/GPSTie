package me.linus.gpstie.gpssender

import me.linus.gpstie.AsyncExecutor
import me.linus.gpstie.BuildConfig
import me.linus.gpstie.encryptText
import me.linus.gpstie.receiveSecretAESKey
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset
import javax.crypto.SecretKey

/**
 * Handles connected clients
 */
class GTConnection(val gtServer: GTServer, val client: Socket) {

    val outputStreamWriter = client.getOutputStream().bufferedWriter(Charset.forName("UTF-8"))
    val clientInputThread: Thread // Thread that reads Pings from the Client
    val executor = AsyncExecutor(threadName = "GT-Server-Connection-Writer") // Handles sending
    val secretKey: SecretKey

    init {
        client.soTimeout = 3000 // = Timeout of 3 seconds during handshake

        // Protocol version exchange and matching (unencrypted):
        DataOutputStream(client.getOutputStream()).writeInt(BuildConfig.PROTOCOL_VERSION)
        client.getOutputStream().flush()

        var clientProtocolVersion = -1
        try {
            clientProtocolVersion = DataInputStream(client.getInputStream()).readInt()
        } catch (ignored: Exception) {}

        if (clientProtocolVersion != BuildConfig.PROTOCOL_VERSION)
            throw IOException("Client has incompatible protocol version ($clientProtocolVersion)!")
        // ------------------------------

        secretKey = receiveSecretAESKey(client)
        client.soTimeout = 30000 // = Timeout of 30 seconds

        clientInputThread = Thread {
            try {
                val inStream = client.getInputStream()

                val buffer = ByteArray(28)
                while (inStream.read(buffer) > 0) Thread.sleep(1)

                disconnect()
            }catch(e: Exception) {
                e.printStackTrace()
                disconnect()
            }
        }.apply { name = "GpsTie-Server-ClientInputThread" } .also { it.start() } // Setup Thread
    }

    /**
     * Disconnects from client, closes the Thread and informs the serverListener about the event
     */
    fun disconnect() {
        gtServer.connectedClients.remove(this)
        gtServer.updateClientCount()
        gtServer.serverListener.onClientDisconnected(this, gtServer)
        try { clientInputThread.stop() }catch(e: Exception) { }
        try { client.close() }catch(e: Exception) { }
        executor.close()
    }

    /**
     * Sends raw message to the client which should be a json string
     */
    fun send(message: String) {
        executor.execute {
            try {
                outputStreamWriter.run {
                    write(encryptText(message, secretKey))
                    newLine()
                    flush() // If executed in Main-Thread an exception will be thrown
                }
            } catch (e: Exception) {
                e.printStackTrace()
                disconnect()
            }
        }
    }

}