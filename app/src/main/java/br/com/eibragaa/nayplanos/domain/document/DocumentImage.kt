package br.com.eibragaa.nayplanos.domain.document

/** JPEG payload and optional accessible description for embedding in a plan document. */
data class DocumentImage(
    val jpegBytes: ByteArray,
    val description: String? = null,
    val width: Int = 1,
    val height: Int = 1,
)
