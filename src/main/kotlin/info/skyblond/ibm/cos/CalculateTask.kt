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
        return try {
            // fetch the meta data first
            val meta = cos.getObjectMetadata(bucketName, objectKey)
            val lastModifiedTimestamp = meta.lastModified.time
            val (ableToReuse: Boolean, resultHash: String, resultTimestamp: Long?) = oldResult?.split(":")
                .let {
                    if (it == null || it.size != 3)
                        Triple(false, "", null)
                    else
                        Triple(
                            it[0] == Constants.SANITY_RESULT_SHA3_PREFIX,
                            it[1], it[2].toLongOrNull()
                        )
                }

            // skip calculation if we can reuse the result
            if (ableToReuse && EnvHelper.getAllowReuse()
                && resultTimestamp == lastModifiedTimestamp
            ) {
                logger.info("Reuse old result for object $objectKey")
                return "${Constants.SANITY_RESULT_SHA3_PREFIX}:$resultHash:$resultTimestamp"
            }

            require(S3ClientHelper.objectCanRead(meta)) {
                "Storage class is `${meta.storageClass}`, but restored copy storage class is ${meta.ibmRestoredCopyStorageClass}"
            }

            // use file content
            cos.getObject(bucketName, objectKey).objectContent.use {
                if (EnvHelper.getAppDebugEnable()) {
                    logger.info("Debug, skip file: $objectKey")
                    "${Constants.SANITY_RESULT_DEBUG_PREFIX}:SKIP:-1"
                } else {
                    logger.info("Calculating file: $objectKey")
                    val hash = digest.calculateSha3(it)
                    "${Constants.SANITY_RESULT_SHA3_PREFIX}:$hash:$lastModifiedTimestamp"
                }
            }
        } catch (e: Exception) {
            logger.error("Error when calculating sha3-256 for `$objectKey`: ", e)
            val message = e.message?.replace(":", "_")
            "${Constants.SANITY_RESULT_ERROR_PREFIX}:$message:-1"
        }
    }
}
