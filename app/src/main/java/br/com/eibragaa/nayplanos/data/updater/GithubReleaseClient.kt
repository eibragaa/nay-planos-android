package br.com.eibragaa.nayplanos.data.updater

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Consulta apenas a release latest do repositório oficial. Não baixa assets. */
class GithubReleaseClient(
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) {
    fun fetchLatest(): GithubReleaseMetadata {
        val endpoint = URL(API_ENDPOINT)
        val connection = connectionFactory(endpoint)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "NayPlanos-Android")
            if (connection.url.protocol != "https" || connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Não foi possível consultar a release oficial do GitHub.")
            }
            val body = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) {
                        throw IOException("Metadados da release excedem o limite permitido.")
                    }
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
            return parse(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(json: String): GithubReleaseMetadata {
        if (json.length > MAX_RESPONSE_BYTES) throw IOException("Metadados da release excedem o limite permitido.")
        try {
            val root = JSONObject(json)
            if (root.optBoolean("draft", true) || root.optBoolean("prerelease", true)) {
                throw IOException("A release não é uma versão estável publicada.")
            }
            val version = root.getString("tag_name").removePrefix("v")
            if (!VERSION_PATTERN.matches(version)) throw IOException("Versão da release inválida.")
            val assets = root.getJSONArray("assets")
            var apk: ReleaseAsset? = null
            var signature: ReleaseAsset? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.getString("name")
                val url = asset.getString("browser_download_url")
                validateAssetUrl(url, name, version)
                when (name) {
                    "nay-planos-$version.apk" -> if (apk == null) apk = ReleaseAsset(name, url)
                        else throw IOException("A release contém assets APK duplicados.")
                    "nay-planos-$version.apk.sig" -> if (signature == null) signature = ReleaseAsset(name, url)
                        else throw IOException("A release contém assinaturas duplicadas.")
                }
            }
            val selectedApk = apk ?: throw IOException("A release não contém o APK esperado.")
            val selectedSignature = signature ?: throw IOException("A release não contém o asset de assinatura esperado.")
            val sha256 = parseSha256(root.optString("body", ""), selectedApk.name)
            return GithubReleaseMetadata(version, selectedApk, sha256, selectedSignature)
        } catch (error: JSONException) {
            throw IOException("Metadados JSON da release estão incompletos ou inválidos.", error)
        }
    }

    private fun parseSha256(body: String, apkName: String): String {
        val line = body.lineSequence().firstOrNull { it.startsWith("SHA256[$apkName]=") }
            ?: throw IOException("SHA-256 obrigatório não encontrado nas notas da release.")
        val digest = line.substringAfter('=').trim()
        if (!SHA256_PATTERN.matches(digest)) throw IOException("SHA-256 da release tem formato inválido.")
        return digest.lowercase()
    }

    private fun validateAssetUrl(value: String, assetName: String, version: String) {
        val uri = try { URI(value) } catch (error: Exception) {
            throw IOException("URL de asset inválida.", error)
        }
        val expectedPath = "/eibragaa/nay-planos-android/releases/download/v$version/$assetName"
        if (uri.scheme != "https" || uri.host != "github.com" || uri.userInfo != null || uri.port != -1 ||
            uri.query != null || uri.fragment != null || uri.path != expectedPath) {
            throw IOException("A URL do asset não pertence ao repositório oficial.")
        }
    }

    companion object {
        private const val API_ENDPOINT = "https://api.github.com/repos/eibragaa/nay-planos-android/releases/latest"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 8_000
        private const val MAX_RESPONSE_BYTES = 256 * 1024
        private val VERSION_PATTERN = Regex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")
        private val SHA256_PATTERN = Regex("^[a-fA-F0-9]{64}$")
    }
}
