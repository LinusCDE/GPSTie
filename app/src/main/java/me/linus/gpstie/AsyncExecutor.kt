package me.linus.gpstie

import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor

class AsyncExecutor(val threadName: String? = null,
                    val threadPriority: Int = Thread.NORM_PRIORITY): Executor, Closeable {

    val queue = ArrayBlockingQueue<Runnable>(10)
    val thread = Thread {
        while(true) {
            try {
                queue.take().run()
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }.apply {
        if(threadName != null)
            name = threadName
        priority = threadPriority
        start()
    }

    override fun execute(command: Runnable?) = queue.put(command)

    override fun close() {
        try {
            thread.stop()
        }catch(e: Exception) { }
    }

}