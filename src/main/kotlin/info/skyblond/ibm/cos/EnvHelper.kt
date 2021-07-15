package info.skyblond.ibm.cos

object EnvHelper {
    private const val ENV_IAM_AUTH_ENDPOINT_KEY = "IAM_ENDPOINT"
    private const val IAM_AUTH_ENDPOINT_DEFAULT = "https://iam.cloud.ibm.com/identity/token"

    private const val ENV_COS_S3_ENDPOINT_KEY = "COS_ENDPOINT"
    private const val ENV_COS_API_KEY_KEY = "COS_APIKEY"
    private const val ENV_COS_SERVICE_CRN_KEY = "COS_SERVICE_CRN"
    private const val ENV_COS_LOCATION_KEY = "COS_BUCKET_LOCATION"
    private const val ENV_COS_BUCKET_NAME_KEY = "COS_BUCKET_NAME"

    private const val ENV_APP_PREFIX_KEY = "APP_PREFIX"
    private const val ENV_APP_RESULT_NAME_KEY = "APP_SANITY_NAME"
    private const val APP_RESULT_FILENAME_DEFAULT = "SANITY_CHECK.txt"

    private val systemEnv: Map<String, String> = System.getenv()

    fun getIamAuthEndpoint(): String = systemEnv[ENV_IAM_AUTH_ENDPOINT_KEY] ?: IAM_AUTH_ENDPOINT_DEFAULT

    fun getCosEndpoint(): String = systemEnv[ENV_COS_S3_ENDPOINT_KEY] ?: requiredEnvError(ENV_COS_S3_ENDPOINT_KEY)
    fun getCosApikey(): String = systemEnv[ENV_COS_API_KEY_KEY] ?: requiredEnvError(ENV_COS_API_KEY_KEY)
    fun getCosServiceCRN(): String = systemEnv[ENV_COS_SERVICE_CRN_KEY] ?: requiredEnvError(ENV_COS_SERVICE_CRN_KEY)
    fun getCosBucketLocation(): String = systemEnv[ENV_COS_LOCATION_KEY] ?: requiredEnvError(ENV_COS_LOCATION_KEY)
    fun getCosBucketName(): String = systemEnv[ENV_COS_BUCKET_NAME_KEY] ?: requiredEnvError(ENV_COS_BUCKET_NAME_KEY)

    fun getAppPrefix(): String = systemEnv[ENV_APP_PREFIX_KEY] ?: requiredEnvError(ENV_APP_PREFIX_KEY)
    fun getAppResultFilename(): String = systemEnv[ENV_APP_RESULT_NAME_KEY] ?: APP_RESULT_FILENAME_DEFAULT

    private fun requiredEnvError(envKey: String): Nothing {
        throw IllegalArgumentException("env `$envKey` is required")
    }
}
