package br.com.eibragaa.nayplanos.data.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import br.com.eibragaa.nayplanos.data.device.AndroidDeviceProfileReader
import br.com.eibragaa.nayplanos.domain.device.DeviceProfile
import br.com.eibragaa.nayplanos.domain.device.LocalModel
import br.com.eibragaa.nayplanos.domain.device.LocalModelCatalog
import br.com.eibragaa.nayplanos.domain.device.ModelRecommendation
import br.com.eibragaa.nayplanos.domain.device.ModelRecommender
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Progresso de um download em andamento. `file` é o arquivo que está sendo baixado agora. */
data class DownloadProgress(
    val modelId: String,
    val file: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
)

/** Estado observável da tela de modelo. A recomendação é apenas sugestão; a escolha é da usuária. */
data class ModelSetupState(
    val profile: DeviceProfile? = null,
    val recommendation: ModelRecommendation? = null,
    val selectedModelId: String? = null,
    val installedModelIds: Set<String> = emptySet(),
    val download: DownloadProgress? = null,
    val errorMessage: String? = null,
)

/** Orquestra leitura do aparelho, escolha, download e remoção dos modelos locais. */
class ModelSetupController(
    context: Context,
    private val scope: CoroutineScope,
    private val store: ModelInstallStore,
    private val downloader: ModelDownloader = ModelDownloader(),
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("model_setup", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(ModelSetupState())
    val state: StateFlow<ModelSetupState> = _state.asStateFlow()
    private var downloadJob: Job? = null

    fun refresh() {
        val profile = AndroidDeviceProfileReader(appContext).read()
        val recommendation = ModelRecommender.recommend(profile, LocalModelCatalog.models)
        val compatibleIds = (recommendation as? ModelRecommendation.Compatible)?.options?.map { it.id }.orEmpty()
        val savedSelection = preferences.getString(KEY_SELECTED, null)
        val selection = savedSelection?.takeIf { it in compatibleIds } ?: compatibleIds.firstOrNull()
        _state.value = _state.value.copy(
            profile = profile,
            recommendation = recommendation,
            selectedModelId = selection,
            installedModelIds = LocalModelCatalog.models.filter(store::isInstalled).map { it.id }.toSet(),
            errorMessage = null,
        )
    }

    fun select(modelId: String) {
        preferences.edit().putString(KEY_SELECTED, modelId).apply()
        _state.value = _state.value.copy(selectedModelId = modelId, errorMessage = null)
    }

    fun startDownload() {
        val current = _state.value
        val model = current.selectedModel() ?: return
        if (current.download != null) return
        if (!isOnWifi()) {
            _state.value = current.copy(errorMessage = ERROR_WIFI_REQUIRED)
            return
        }
        downloadJob = scope.launch {
            try {
                download(model)
                _state.value = _state.value.copy(
                    download = null,
                    installedModelIds = _state.value.installedModelIds + model.id,
                    errorMessage = null,
                )
            } catch (cancellation: CancellationException) {
                _state.value = _state.value.copy(download = null)
                throw cancellation
            } catch (error: IOException) {
                _state.value = _state.value.copy(download = null, errorMessage = error.message ?: ERROR_GENERIC)
            } catch (_: Exception) {
                _state.value = _state.value.copy(download = null, errorMessage = ERROR_GENERIC)
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    fun delete(modelId: String) {
        val model = LocalModelCatalog.models.firstOrNull { it.id == modelId } ?: return
        scope.launch {
            store.delete(model)
            _state.value = _state.value.copy(
                installedModelIds = _state.value.installedModelIds - modelId,
            )
        }
    }

    private suspend fun download(model: LocalModel) {
        store.ensureInstallDir(model)
        for (file in listOf(model.model, model.mmproj)) {
            val destination = if (file == model.model) store.modelPath(model) else store.mmprojPath(model)
            downloader.download(
                url = model.downloadUrl(file),
                destination = destination,
                expectedSize = file.sizeBytes,
                expectedSha256 = file.sha256,
            ) { downloaded, total ->
                _state.value = _state.value.copy(
                    download = DownloadProgress(model.id, file.fileName, downloaded, total),
                )
            }
        }
    }

    private fun isOnWifi(): Boolean {
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun ModelSetupState.selectedModel(): LocalModel? =
        LocalModelCatalog.models.firstOrNull { it.id == selectedModelId }

    private companion object {
        const val KEY_SELECTED = "selected_model_id"
        const val ERROR_WIFI_REQUIRED = "Conecte-se ao Wi-Fi para baixar o modelo (são de 1,7 a 2,8 GB, conforme o modelo)."
        const val ERROR_GENERIC = "Não foi possível baixar o modelo. Verifique a conexão e tente de novo."
    }
}
