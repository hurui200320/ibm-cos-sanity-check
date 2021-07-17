package info.skyblond.ibm.cos

import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

object ExecutionPoolHelper {
    val listWorkerPool = getFixedDaemonThreadPool(Runtime.getRuntime().availableProcessors() * 2)
    val calculatorPool = getFixedDaemonThreadPool(Runtime.getRuntime().availableProcessors())

    private fun getFixedDaemonThreadPool(nThread: Int): ThreadPoolExecutor {
        return Executors.newFixedThreadPool(nThread) { r ->
            Executors.defaultThreadFactory()
                .newThread(r)
                .also { it.isDaemon = true }
        } as ThreadPoolExecutor
    }
}
