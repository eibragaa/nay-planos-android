package br.com.eibragaa.nayplanos.data.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import br.com.eibragaa.nayplanos.domain.image.ImageStore
import br.com.eibragaa.nayplanos.domain.image.StoredImage
import br.com.eibragaa.nayplanos.domain.image.StoredThumbnail
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Falha acionável durante importação, consulta ou remoção de imagem. */
class ImageStoreException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Armazena originais e miniaturas no diretório privado do aplicativo. */
class AppImageStore(context: Context) : ImageStore {
    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val imageDirectory = File(context.applicationContext.filesDir, IMAGE_DIRECTORY)
    private val thumbnailDirectory = File(imageDirectory, THUMBNAIL_DIRECTORY)

    override suspend fun importImage(sourceReference: String): StoredImage = withContext(Dispatchers.IO) {
        val source = try {
            Uri.parse(sourceReference)
        } catch (error: RuntimeException) {
            throw ImageStoreException("A referência da imagem de origem é inválida.", error)
        }
        val mimeType = try {
            resolver.getType(source)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw ImageStoreException("Não foi possível identificar o tipo da imagem selecionada.", error)
        }
        if (mimeType?.startsWith(IMAGE_MIME_PREFIX, ignoreCase = true) != true) {
            throw ImageStoreException("O arquivo selecionado não é uma imagem reconhecida.")
        }
        ensureDirectories()

        val id = UUID.randomUUID().toString()
        val originalTemp = File(imageDirectory, "$id$PART_SUFFIX")
        val thumbnailTemp = File(thumbnailDirectory, "$id$PART_SUFFIX")
        val original = File(imageDirectory, "$id$ORIGINAL_SUFFIX")
        val thumbnail = File(thumbnailDirectory, "$id$THUMBNAIL_SUFFIX")
        try {
            copyBounded(source, originalTemp)
            createThumbnail(originalTemp, thumbnailTemp)
            if (!originalTemp.renameTo(original)) {
                throw ImageStoreException("Não foi possível concluir o armazenamento da imagem.")
            }
            if (!thumbnailTemp.renameTo(thumbnail)) {
                throw ImageStoreException("Não foi possível concluir a miniatura da imagem.")
            }
            return@withContext StoredImage(referenceFor(id), thumbnailReferenceFor(id))
        } catch (error: Throwable) {
            cleanupAttemptFiles(listOf(originalTemp, thumbnailTemp, original, thumbnail), error)
            when (error) {
                is CancellationException -> throw error
                is ImageStoreException -> throw error
                is Exception -> throw ImageStoreException("Falha ao importar a imagem selecionada.", error)
                else -> throw error
            }
        }
    }

    override fun thumbnailReference(imageReference: String): String? {
        val id = parseReference(imageReference) ?: return null
        return try {
            val original = originalFile(id)
            val thumbnail = thumbnailFile(id)
            if (original.isFile && thumbnail.isFile) thumbnailReferenceFor(id) else null
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    override suspend fun readThumbnail(imageReference: String): StoredThumbnail? = withContext(Dispatchers.IO) {
        val id = parseReference(imageReference) ?: return@withContext null
        try {
            val original = originalFile(id)
            val thumbnail = thumbnailFile(id)
            if (!original.isFile || !thumbnail.isFile) return@withContext null

            val length = thumbnail.length()
            if (length <= 0L || length > MAX_THUMBNAIL_BYTES) return@withContext null
            val bytes = thumbnail.inputStream().use { input ->
                val output = java.io.ByteArrayOutputStream(length.toInt())
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_THUMBNAIL_BYTES) return@withContext null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outMimeType != JPEG_MIME_TYPE ||
                bounds.outWidth !in 1..MAX_THUMBNAIL_DIMENSION ||
                bounds.outHeight !in 1..MAX_THUMBNAIL_DIMENSION
            ) {
                return@withContext null
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                ?: return@withContext null
            decoded.recycle()
            StoredThumbnail(
                jpegBytes = bytes,
                width = bounds.outWidth,
                height = bounds.outHeight,
            )
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    override suspend fun deleteImage(imageReference: String) =
        withContext(NonCancellable + Dispatchers.IO) {
            val id = parseReference(imageReference)
                ?: throw ImageStoreException("A referência da imagem é inválida ou desconhecida.")
            val original = originalFile(id)
            val thumbnail = thumbnailFile(id)
            val originalDeleted = deleteIfPresent(original)
            val thumbnailDeleted = deleteIfPresent(thumbnail)
            if (!originalDeleted || !thumbnailDeleted) {
                throw ImageStoreException("Não foi possível remover todos os arquivos da imagem.")
            }
        }

    private fun copyBounded(source: Uri, destination: File) {
        val input = try {
            resolver.openInputStream(source)
                ?: throw ImageStoreException("O aplicativo não conseguiu abrir a imagem selecionada.")
        } catch (error: CancellationException) {
            throw error
        } catch (error: ImageStoreException) {
            throw error
        } catch (error: Exception) {
            throw ImageStoreException("O aplicativo não conseguiu abrir a imagem selecionada.", error)
        }
        input.use { stream ->
            BufferedOutputStream(FileOutputStream(destination)).use { output ->
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_ORIGINAL_BYTES) {
                        throw ImageStoreException("A imagem excede o limite de 10 MiB.")
                    }
                    output.write(buffer, 0, count)
                }
                if (total == 0L) throw ImageStoreException("A imagem selecionada está vazia.")
            }
        }
    }

    private fun createThumbnail(original: File, destination: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(original.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw ImageStoreException("O conteúdo selecionado não pôde ser decodificado como imagem.")
        }
        val pixelCount = bounds.outWidth.toLong() * bounds.outHeight.toLong()
        if (pixelCount > MAX_IMAGE_PIXELS) {
            throw ImageStoreException("A imagem excede o limite seguro de $MAX_IMAGE_PIXELS pixels.")
        }
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val bitmap = BitmapFactory.decodeFile(
            original.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: throw ImageStoreException("Não foi possível criar a miniatura da imagem.")

        bitmap.useBitmap { decoded ->
            val scale = minOf(
                1f,
                MAX_THUMBNAIL_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height),
            )
            val width = (decoded.width * scale).toInt().coerceAtLeast(1)
            val height = (decoded.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (width == decoded.width && height == decoded.height) {
                decoded
            } else {
                Bitmap.createScaledBitmap(decoded, width, height, true)
            }
            try {
                FileOutputStream(destination).use { output ->
                    if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                        throw ImageStoreException("Não foi possível codificar a miniatura JPEG.")
                    }
                }
            } finally {
                if (scaled !== decoded) scaled.recycle()
            }
        }
    }

    private fun ensureDirectories() {
        if ((!imageDirectory.isDirectory && !imageDirectory.mkdirs()) ||
            (!thumbnailDirectory.isDirectory && !thumbnailDirectory.mkdirs())
        ) {
            throw ImageStoreException("Não foi possível preparar o armazenamento privado de imagens.")
        }
    }

    private fun originalFile(id: String): File = childFile(imageDirectory, "$id$ORIGINAL_SUFFIX")

    private fun thumbnailFile(id: String): File =
        childFile(thumbnailDirectory, "$id$THUMBNAIL_SUFFIX")

    private fun childFile(parent: File, name: String): File {
        val child = File(parent, name).canonicalFile
        if (child.parentFile != parent.canonicalFile) {
            throw ImageStoreException("A referência aponta para um caminho não permitido.")
        }
        return child
    }

    private fun deleteIfPresent(file: File): Boolean {
        if (!file.exists()) return true
        return file.delete() || !file.exists()
    }

    private fun cleanupAttemptFiles(files: List<File>, cause: Throwable) {
        files.forEach { file ->
            try {
                if (file.exists() && !file.delete() && file.exists()) {
                    cause.addSuppressed(IOException("Não foi possível limpar o arquivo temporário da importação."))
                }
            } catch (cleanupError: Exception) {
                cause.addSuppressed(cleanupError)
            }
        }
    }

    private fun parseReference(reference: String): String? {
        if (!reference.startsWith(REFERENCE_PREFIX)) return null
        val rawId = reference.removePrefix(REFERENCE_PREFIX)
        val id = try {
            UUID.fromString(rawId).toString()
        } catch (_: IllegalArgumentException) {
            return null
        }
        return id.takeIf { rawId == it }
    }

    private fun referenceFor(id: String): String = "$REFERENCE_PREFIX$id"

    private fun thumbnailReferenceFor(id: String): String = "$REFERENCE_PREFIX$id$THUMBNAIL_MARKER"

    private inline fun Bitmap.useBitmap(block: (Bitmap) -> Unit) {
        try {
            block(this)
        } finally {
            recycle()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width / sample, height / sample) > MAX_THUMBNAIL_DIMENSION * 2) {
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val IMAGE_DIRECTORY = "images"
        const val THUMBNAIL_DIRECTORY = "thumbnails"
        const val REFERENCE_PREFIX = "image://"
        const val THUMBNAIL_MARKER = "/thumbnail"
        const val ORIGINAL_SUFFIX = ".img"
        const val THUMBNAIL_SUFFIX = ".jpg"
        const val PART_SUFFIX = ".part"
        const val IMAGE_MIME_PREFIX = "image/"
        const val MAX_ORIGINAL_BYTES = 10L * 1024L * 1024L
        const val MAX_IMAGE_PIXELS = 40_000_000L
        const val MAX_THUMBNAIL_DIMENSION = 512
        const val MAX_THUMBNAIL_BYTES = 1024 * 1024
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val COPY_BUFFER_SIZE = 8 * 1024
        const val JPEG_QUALITY = 88
    }
}
