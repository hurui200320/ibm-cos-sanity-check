package info.skyblond.ibm.cos

import info.skyblond.ibm.cos.SHA3Utils.calculateSha3
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class CalculateTask(
    private val objectKey: String,
    private val oldResult: String?
) : Callable<String> {
    private val logger = LoggerFactory.getLogger(CalculateTask::class.java)

    private val cos = S3ClientHelper.client
    private val bucketName = EnvHelper.getCosBucketName()
    private val digest = SHA3Utils.getSha3Digest()

    override fun call(): String {
        val currentTime = System.currentTimeMillis()
        // if old result is validate, skip calculation
        if (oldResult != null
            && oldResult.startsWith("${Constants.SANITY_RESULT_SHA3_PREFIX}:")
            && oldResult.count { it == ':' } == 2
        ) {
            logger.info("Reuse old result for object $objectKey")
            return oldResult
        }
        // calculation
        return try {
            val obj = cos.getObject(bucketName, objectKey)
            // TODO avoid archived object in the future
            //      Currently if is STANDARD, then null is returned
            if (obj.objectMetadata.storageClass == null) {
                if (EnvHelper.getAppDebugEnable()) {
                    logger.info("Debug, skip file: $objectKey")
                    "${Constants.SANITY_RESULT_DEBUG_PREFIX}:SKIP:$currentTime"
                } else {
                    logger.info("Calculating file: $objectKey")
                    val hash = digest.calculateSha3(obj.objectContent)
                    "${Constants.SANITY_RESULT_SHA3_PREFIX}:$hash:$currentTime"
                }
            } else {
                throw Exception("Unknown storage class `${obj.objectMetadata.storageClass}`")
            }
        } catch (e: Exception) {
            logger.error("Error when calculating sha3-256: ", e)
            val message = e.message?.replace(":", "_")
            "${Constants.SANITY_RESULT_ERROR_PREFIX}:$message:$currentTime"
        }
    }
}
