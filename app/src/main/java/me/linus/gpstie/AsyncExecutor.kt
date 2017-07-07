package me.linus.gpstie

import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor

/**
 * Own implementation of a single-thread-pool which accepts and executes Runnables
 */
class AsyncExecutor(val threadName: String? = null,
                    val threadPriority: Int = Thread.NORM_PRIORITY): Executor, Closeable {

    val queue = ArrayBlockingQueue<Runnable>(10) // Queue of Runnables
    val thread = Thread { // Executer Thread for queue
        while(true) {
            try {
                queue.take().run()
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }.apply { // Setup thread and start it
        if(threadName != null)
            name = threadName
        priority = threadPriority
        start()
    }

    /**
     * Add new Runnable to the queue
     */
    override fun execute(command: Runnable?) = queue.put(command)

    /**
     * Stops Thread
     */
    override fun close() {
        try {
            thread.stop()
        }catch(e: Exception) { }
    }

}