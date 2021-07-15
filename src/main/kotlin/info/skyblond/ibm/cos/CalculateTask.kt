package info.skyblond.ibm.cos

import info.skyblond.ibm.cos.SHA3Utils.calculateSha3
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class CalculateTask(
    private val objectKey: String
) : Callable<String> {
    private val logger = LoggerFactory.getLogger(CalculateTask::class.java)

    private val cos = S3ClientHelper.client
    private val bucketName = EnvHelper.getCosBucketName()
    private val digest = SHA3Utils.getSha3Digest()

    override fun call(): String {
        val obj = cos.getObject(bucketName, objectKey)
        // TODO avoid archived object in the future
        //      Currently if is STANDARD, then null is returned
        return if (obj.objectMetadata.storageClass == null) {
            logger.info("Calculating file: $objectKey")
            "SHA3-256:" + digest.calculateSha3(obj.objectContent)
        } else {
            logger.info("Unknown storage class `${obj.objectMetadata.storageClass}`: $objectKey")
            "NONE:unknown storage class `${obj.objectMetadata.storageClass}`"
        }
    }
}
