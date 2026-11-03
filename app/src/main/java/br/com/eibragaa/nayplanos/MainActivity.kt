package br.com.eibragaa.nayplanos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import br.com.eibragaa.nayplanos.data.document.MinimalDocxGenerator
import br.com.eibragaa.nayplanos.data.image.AppImageStore
import br.com.eibragaa.nayplanos.data.local.NayPlanosDatabase
import br.com.eibragaa.nayplanos.data.repository.RoomWeeklyPlanRepository
import br.com.eibragaa.nayplanos.ui.design.NayPlanosTheme
import br.com.eibragaa.nayplanos.ui.navigation.NayPlanosApp
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DOCX_MIME_TYPE =
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cleanupStaleCameraCaptures()
        val database = Room.databaseBuilder(
            applicationContext,
            NayPlanosDatabase::class.java,
            "nay-planos.db",
        ).build()
        val repository = RoomWeeklyPlanRepository(database.weeklyPlanDao())
        val imageStore = AppImageStore(applicationContext)
        val documentGenerator = MinimalDocxGenerator()
        setContent {
            NayPlanosTheme {
                NayPlanosApp(
                    repository = repository,
                    imageStore = imageStore,
                    documentGenerator = documentGenerator,
                    onShareDocument = ::shareDocument,
                    onPrepareCameraCapture = ::prepareCameraCapture,
                    onCleanupCameraCapture = ::cleanupCameraCapture,
                )
            }
        }
    }

    private fun prepareCameraCapture(): Uri? {
        var captureFile: File? = null
        return try {
            val cameraDirectory = File(cacheDir, "camera")
            check(cameraDirectory.exists() || cameraDirectory.mkdirs())
            val createdFile = File(cameraDirectory, "${UUID.randomUUID()}.jpg")
            captureFile = createdFile
            check(createdFile.createNewFile())
            FileProvider.getUriForFile(this, "$packageName.fileprovider", createdFile)
        } catch (_: Exception) {
            captureFile?.let { file -> if (file.exists()) file.delete() }
            null
        }
    }

    private fun cleanupStaleCameraCaptures() {
        val cameraDirectory = File(cacheDir, "camera")
        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        cameraDirectory.listFiles()
            ?.filter { it.isFile && it.name.matches(Regex("[0-9a-fA-F-]{36}\\.jpg")) && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    private fun cleanupCameraCapture(uri: Uri) {
        if (uri.scheme != "content" || uri.authority != "$packageName.fileprovider") return
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != "camera_capture") return
        val fileName = segments[1]
        if (!Regex("[0-9a-fA-F-]{36}\\.jpg").matches(fileName)) return
        val cameraDirectory = File(cacheDir, "camera").canonicalFile
        val captureFile = File(cameraDirectory, fileName).canonicalFile
        if (captureFile.parentFile == cameraDirectory && captureFile.isFile) {
            captureFile.delete()
        }
    }

    private suspend fun shareDocument(fileName: String, bytes: ByteArray) {
        val safeName = fileName
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9._-]"), "-")
            .trim('-', '.', '_')
            .take(80)
            .ifBlank { "plano-semanal" }
            .let { if (it.endsWith(".docx")) it else "$it.docx" }
        val outputFile = withContext(Dispatchers.IO) {
            val exportDirectory = File(cacheDir, "exports").apply { mkdirs() }
            check(exportDirectory.isDirectory) {
                "Não foi possível preparar o arquivo para compartilhamento."
            }
            File(exportDirectory, "${UUID.randomUUID()}-$safeName").apply {
                writeBytes(bytes)
            }
        }
        val contentUri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            outputFile,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = DOCX_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = android.content.ClipData.newUri(contentResolver, safeName, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_docx_chooser_title)))
    }
}
