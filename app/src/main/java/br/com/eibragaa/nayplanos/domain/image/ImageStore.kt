package br.com.eibragaa.nayplanos.domain.image

/** Identificadores opacos retornados após importar uma imagem para o armazenamento privado. */
data class StoredImage(
    val reference: String,
    val thumbnailReference: String,
)

/** Bytes JPEG validados e dimensões da miniatura armazenada. */
data class StoredThumbnail(
    val jpegBytes: ByteArray,
    val width: Int,
    val height: Int,
)

/** Operações de importação, consulta e remoção de imagens mantidas pelo aplicativo. */
interface ImageStore {
    suspend fun importImage(sourceReference: String): StoredImage

    fun thumbnailReference(imageReference: String): String?

    suspend fun readThumbnail(imageReference: String): StoredThumbnail?

    suspend fun deleteImage(imageReference: String)
}
