package info.skyblond.ibm.cos

import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadPoolExecutor

object Main {
    private val logger = LoggerFactory.getLogger("Application")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            logger.info("Application started...")
            logger.info("Bucket name: " + EnvHelper.getCosBucketName())
            logger.info("Prefix: " + EnvHelper.getAppPrefix())
            logger.info("Sanity filename: " + EnvHelper.getAppResultFilename())

            ExecutionPoolHelper.listWorkerPool.submit(
                ListWorker(EnvHelper.getAppPrefix())
            )

            waitThreadPool(ExecutionPoolHelper.listWorkerPool)
            ExecutionPoolHelper.listWorkerPool.shutdown()

            waitThreadPool(ExecutionPoolHelper.calculatorPool)
            ExecutionPoolHelper.listWorkerPool.shutdown()
            logger.info("Job done. Exit...")
        } catch (e: Exception) {
            logger.error("Error when starting the application: ", e)
        }
    }

    private fun waitThreadPool(threadPool: ThreadPoolExecutor) {
        var taskCount: Long = threadPool.taskCount
        var completedTaskCount: Long = threadPool.completedTaskCount
        while (taskCount != completedTaskCount) {
            Thread.sleep(5000)
            taskCount = threadPool.taskCount
            completedTaskCount = threadPool.completedTaskCount
        }
    }
}
