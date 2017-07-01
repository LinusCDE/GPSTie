package me.linus.gpstie.gpssender

import me.linus.gpstie.AsyncExecutor
import java.net.Socket
import java.nio.charset.Charset

class GTConnection(val gtServer: GTServer, val client: Socket) {

    val writer = client.getOutputStream().bufferedWriter(Charset.forName("UTF-8"))
    val clientInputThread: Thread
    val executor = AsyncExecutor(threadName = "GT-Server-Connection-Writer")

    init {
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
        }.apply { name = "GpsTie-Server-ClientInputThread" } .also { it.start() }
    }

    fun disconnect() {
        gtServer.connectedClients.remove(this)
        gtServer.updateClientCount()
        gtServer.serverListener.onClientDisconnected(this, gtServer)
        try { clientInputThread.stop() }catch(e: Exception) { }
        try { client.close() }catch(e: Exception) { }
        executor.close()
    }

    fun send(message: String) {
        executor.execute {
            try {
                writer.write(message)
                writer.newLine()
                writer.flush() // If executed in Main-Thread an exception will be thrown
            } catch (e: Exception) {
                e.printStackTrace()
                disconnect()
            }
        }
    }

}