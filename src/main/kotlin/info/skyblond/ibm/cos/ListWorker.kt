package info.skyblond.ibm.cos

import com.ibm.cloud.objectstorage.services.s3.model.ListObjectsV2Request
import com.ibm.cloud.objectstorage.services.s3.model.ListObjectsV2Result
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.concurrent.Future

class ListWorker(
    private val prefix: String
) : Runnable {
    private val logger = LoggerFactory.getLogger(ListWorker::class.java)

    private val cos = S3ClientHelper.client
    private val bucketName = EnvHelper.getCosBucketName()
    private val sanityFileName = EnvHelper.getAppResultFilename()

    private val fileList = mutableListOf<Pair<String, Future<String>>>()

    // key to old result
    private val oldResultMap = mutableMapOf<String, String>()

    override fun run() {
        try {
            // check the sanity file
            if (prefix.endsWith("/") && cos.doesObjectExist(bucketName, prefix + sanityFileName)) {
                cos.getObject(bucketName, prefix + sanityFileName)
                    .objectContent
                    .use { it.bufferedReader(StandardCharsets.UTF_8).readLines() }
                    .filter { it.startsWith("${Constants.SANITY_RESULT_SHA3_PREFIX}:") }
                    .map { it.split(":") }
                    .filter { it.size >= 4 } // type:payload:timestamp:key
                    .forEach {
                        val key = it.drop(3).joinToString(":")
                        val value = it.take(3).joinToString(":")
                        oldResultMap[key] = value
                    }
            }


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

                // Only folders are ended with '/', check files
                if (prefix.endsWith("/")) {
                    result.objectSummaries
                        .filter { it.size != 0L }
                        .filterNot { it.key.endsWith("/$sanityFileName") }
                        .forEach {
                            // for each file, launch a calculate task
                            val task = CalculateTask(it.key, oldResultMap[it.key])
                            val future = ExecutionPoolHelper.calculatorPool.submit(task)
                            fileList.add(it.key to future)
                        }
                }

                if (result.isTruncated) {
                    nextToken = result.nextContinuationToken
                } else {
                    nextToken = ""
                    moreResults = false
                }
            }
            // by the end, wait each task finished, and save the result to file
            if (prefix.endsWith("/") && fileList.isNotEmpty() && !EnvHelper.getAppDebugEnable()) {
                val content = fileList.joinToString("\n", "# Prefix: ${getFormattedPrefix()}\n") {
                    it.second.get() + ":" + it.first
                }
                S3ClientHelper.createTextFile(
                    prefix + sanityFileName,
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
