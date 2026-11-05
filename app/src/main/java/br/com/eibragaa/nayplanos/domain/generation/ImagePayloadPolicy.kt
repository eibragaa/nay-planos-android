package br.com.eibragaa.nayplanos.domain.generation

/** Limites e formato do JPEG enviado ao serviço de análise. Imagens nunca são registradas em log. */
object ImagePayloadPolicy {
    const val MAX_PAYLOAD_BYTES: Int = 1 * 1024 * 1024

    private val jpegSignature = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    sealed interface Result {
        data object Accepted : Result
        data class Rejected(val reason: Reason) : Result
    }

    enum class Reason { EMPTY, TOO_LARGE, NOT_JPEG }

    fun check(jpegBytes: ByteArray): Result = when {
        jpegBytes.isEmpty() -> Result.Rejected(Reason.EMPTY)
        jpegBytes.size > MAX_PAYLOAD_BYTES -> Result.Rejected(Reason.TOO_LARGE)
        !startsWith(jpegBytes, jpegSignature) -> Result.Rejected(Reason.NOT_JPEG)
        else -> Result.Accepted
    }

    private fun startsWith(bytes: ByteArray, prefix: ByteArray): Boolean =
        bytes.size >= prefix.size && prefix.indices.all { bytes[it] == prefix[it] }
}
