package info.skyblond.ibm.cos

import com.ibm.cloud.objectstorage.services.s3.model.ListObjectsV2Request
import com.ibm.cloud.objectstorage.services.s3.model.ListObjectsV2Result
import org.slf4j.LoggerFactory
import java.util.concurrent.Future

class ListWorker(
    private val prefix: String
) : Runnable {
    private val logger = LoggerFactory.getLogger(ListWorker::class.java)

    private val cos = S3ClientHelper.client
    private val bucketName = EnvHelper.getCosBucketName()
    private val sanityFileName = EnvHelper.getAppResultFilename()

    private val fileList = mutableListOf<Pair<String, Future<String>>>()

    override fun run() {
        try {
            var moreResults = true
            var nextToken = ""
            while (moreResults) {
                val request = ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix)
                    .withDelimiter("/")
                    .withContinuationToken(nextToken)
                val result: ListObjectsV2Result = cos.listObjectsV2(request)
                // for each sub folder, launch new list worker
                result.commonPrefixes.forEach {
                    logger.info("Discovered sub folder: $it")
                    ExecutionPoolHelper.listWorkerPool.submit(ListWorker(it))
                }

                // Only folders are ended with '/'
                if (prefix.endsWith("/")) {
                    // skip already calculated folder
                    if (result.objectSummaries.any { it.key.endsWith("/$sanityFileName") }) {
                        logger.warn("Sanity file found, Skipping $prefix")
                        // if checked, cancel all task
                        fileList.forEach {
                            if (!it.second.isDone)
                                it.second.cancel(true)
                        }
                        return
                    }

                    result.objectSummaries
                        .filter { it.size != 0L }
                        .forEach {
                            // for each file, launch a calculate task
                            val future = ExecutionPoolHelper.calculatorPool.submit(CalculateTask(it.key))
                            fileList.add(it.key to future)
                        }
                    if (result.isTruncated) {
                        nextToken = result.nextContinuationToken
                    } else {
                        nextToken = ""
                        moreResults = false
                    }
                }
            }
            // by the end, wait each task finished, and save the result to file
            if (fileList.isNotEmpty() && !EnvHelper.getAppDebugEnable()) {
                val content = fileList.joinToString("\n", "# Prefix: ${getFormattedPrefix()}\n") {
                    it.second.get() + ":" + it.first
                }
                S3ClientHelper.createTextFile(
                    prefix.removeSuffix("/") + "/" + sanityFileName,
                    content
                )
            }
            logger.info("List done: $prefix")
        } catch (e: Exception) {
            logger.error("Error when listing: ", e)
        }
    }

    private fun getFormattedPrefix(): String {
        return "/$prefix"
    }
}
