package info.skyblond.ibm.cos

import com.ibm.cloud.objectstorage.ClientConfiguration
import com.ibm.cloud.objectstorage.Protocol
import com.ibm.cloud.objectstorage.SDKGlobalConfiguration
import com.ibm.cloud.objectstorage.auth.AWSCredentials
import com.ibm.cloud.objectstorage.auth.AWSStaticCredentialsProvider
import com.ibm.cloud.objectstorage.client.builder.AwsClientBuilder
import com.ibm.cloud.objectstorage.oauth.BasicIBMOAuthCredentials
import com.ibm.cloud.objectstorage.services.s3.AmazonS3
import com.ibm.cloud.objectstorage.services.s3.AmazonS3ClientBuilder
import com.ibm.cloud.objectstorage.services.s3.model.ObjectMetadata
import com.ibm.cloud.objectstorage.services.s3.model.PutObjectRequest
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets


object S3ClientHelper {
    val client: AmazonS3

    init {
        SDKGlobalConfiguration.IAM_ENDPOINT = EnvHelper.getIamAuthEndpoint()

        val credentials: AWSCredentials = BasicIBMOAuthCredentials(
            EnvHelper.getCosApikey(), EnvHelper.getCosServiceCRN()
        )
        val clientConfig = ClientConfiguration()
        // default timeout for 30s
        clientConfig.requestTimeout = 30000
        clientConfig.setUseTcpKeepAlive(true)

        client = AmazonS3ClientBuilder.standard()
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(
                    EnvHelper.getCosEndpoint(),
                    EnvHelper.getCosBucketLocation()
                )
            )
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfig)
            .build()
    }

    fun createTextFile(fileKey: String, fileText: String) {
        val arr: ByteArray = fileText.toByteArray(StandardCharsets.UTF_8)
        val newStream: InputStream = ByteArrayInputStream(arr)
        val metadata = ObjectMetadata()
        metadata.contentLength = arr.size.toLong()
        val req = PutObjectRequest(
            EnvHelper.getCosBucketName(), fileKey, newStream, metadata
        )
        client.putObject(req)
    }
}
