package info.skyblond.ibm.cos

import java.io.InputStream
import java.security.MessageDigest

object SHA3Utils {
    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte ->
            "%02x".format(eachByte)
        }

    fun getSha3Digest(): MessageDigest =
        MessageDigest.getInstance("SHA3-256")

    fun MessageDigest.calculateSha3(byteArray: ByteArray): String =
        this.digest(byteArray).toHex()

    fun MessageDigest.calculateSha3(inputStream: InputStream, bufferSize: Int): String {
        val buffer = ByteArray(bufferSize)
        var bytesReadCount: Int
        while (inputStream.read(buffer, 0, buffer.size).also { bytesReadCount = it } != -1) {
            this.update(buffer, 0, bytesReadCount)
        }
        inputStream.close()
        return this.digest().toHex()
    }

    // default 64MB buffer
    fun MessageDigest.calculateSha3(inputStream: InputStream): String =
        this.calculateSha3(inputStream, 64 * 1024 * 1024)
}
