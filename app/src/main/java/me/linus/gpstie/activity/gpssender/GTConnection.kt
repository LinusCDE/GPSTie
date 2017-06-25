package me.linus.gpstie.activity.gpssender

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.Charset

class GTConnection(val gtServer: GTServer, val client: Socket) {

    val writer = client.getOutputStream().bufferedWriter(Charset.forName("UTF-8"))
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