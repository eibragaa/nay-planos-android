package br.com.eibragaa.nayplanos.data.model

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Baixa um arquivo do Hugging Face com retomada (HTTP Range) e verifica o SHA-256 antes de
 * torná-lo visível. Um arquivo só existe no destino final depois de íntegro.
 */
class ModelDownloader(
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) {
    /**
     * @param onProgress recebe (bytesBaixados, bytesTotais).
     * @throws IOException em falha de rede, HTTP inesperado ou SHA-256 divergente.
     */
    suspend fun download(
        url: String,
        destination: File,
        expectedSize: Long,
        expectedSha256: String,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (destination.isFile && destination.length() == expectedSize &&
            sha256(destination).equals(expectedSha256, ignoreCase = true)
        ) {
            onProgress(expectedSize, expectedSize)
            return@withContext
        }

        val partial = File(destination.parentFile, destination.name + PART_SUFFIX)
        var offset = if (partial.isFile) partial.length() else 0L
        if (offset > expectedSize) {
            partial.delete()
            offset = 0L
        }

        val connection = connectionFactory(URL(url))
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")

            val code = connection.responseCode
            if (connection.url.protocol != "https") throw IOException("Download recusado: conexão não segura.")
            when (code) {
                HttpURLConnection.HTTP_PARTIAL -> Unit
                HttpURLConnection.HTTP_OK -> {
                    // Servidor ignorou o Range: recomeça do zero.
                    offset = 0L
                    partial.delete()
                }
                else -> throw IOException("Servidor respondeu HTTP $code ao baixar o modelo.")
            }

            RandomAccessFile(partial, "rw").use { output ->
                output.seek(offset)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var written = offset
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        written += count
                        if (written > expectedSize) {
                            throw IOException("O arquivo baixado excede o tamanho publicado.")
                        }
                        onProgress(written, expectedSize)
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } finally {
            connection.disconnect()
        }

        if (partial.length() != expectedSize) {
            throw IOException("Download incompleto. Tente novamente para retomar.")
        }
        if (!sha256(partial).equals(expectedSha256, ignoreCase = true)) {
            partial.delete()
            throw IOException("O arquivo baixado não passou na verificação de integridade.")
        }
        destination.parentFile?.mkdirs()
        if (destination.exists()) destination.delete()
        if (!partial.renameTo(destination)) {
            throw IOException("Não foi possível concluir o arquivo do modelo.")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PART_SUFFIX = ".part"
        const val BUFFER_SIZE = 64 * 1024
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val USER_AGENT = "NayPlanos-Android"
    }
}
