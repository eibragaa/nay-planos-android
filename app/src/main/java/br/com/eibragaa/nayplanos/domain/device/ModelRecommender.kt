package br.com.eibragaa.nayplanos.domain.device

/** Resultado da recomendação: modelos compatíveis ordenados do maior para o menor, ou o motivo de nenhum caber. */
sealed interface ModelRecommendation {
    /** `options` vem do maior para o menor; o primeiro é a sugestão padrão, mas a usuária escolhe. */
    data class Compatible(val options: List<LocalModel>) : ModelRecommendation
    data class NoneFits(val reason: NoFitReason) : ModelRecommendation
}

enum class NoFitReason {
    UNSUPPORTED_ABI,
    INSUFFICIENT_RAM,
    INSUFFICIENT_STORAGE,
}

/** Regras puras de compatibilidade. Conservadoras: deixam folga para o sistema e para o app. */
object ModelRecommender {
    const val REQUIRED_ABI = "arm64-v8a"

    /** Fração da RAM total que o modelo pode ocupar, para não derrubar o sistema e outros apps. */
    const val RAM_BUDGET_FRACTION = 0.5

    /** Folga de armazenamento além dos arquivos, para o download temporário e o cache. */
    const val STORAGE_MARGIN_MB = 1_024L

    private const val BYTES_PER_MB = 1_048_576L

    fun requiredStorageMb(model: LocalModel): Long =
        ceilMb(model.totalDownloadBytes) + STORAGE_MARGIN_MB

    fun recommend(device: DeviceProfile, catalog: List<LocalModel>): ModelRecommendation {
        if (device.cpuAbi != REQUIRED_ABI) {
            return ModelRecommendation.NoneFits(NoFitReason.UNSUPPORTED_ABI)
        }
        val ramBudget = (device.totalRamMb * RAM_BUDGET_FRACTION).toLong()
        val fitsRam = catalog.filter { it.supportsImages && it.estimatedRamMb <= ramBudget }
        val fitting = fitsRam
            .filter { device.freeStorageMb >= requiredStorageMb(it) }
            .sortedByDescending { it.estimatedRamMb }

        if (fitting.isNotEmpty()) return ModelRecommendation.Compatible(fitting)
        return ModelRecommendation.NoneFits(
            if (fitsRam.isNotEmpty()) NoFitReason.INSUFFICIENT_STORAGE else NoFitReason.INSUFFICIENT_RAM,
        )
    }

    private fun ceilMb(bytes: Long): Long = (bytes + BYTES_PER_MB - 1) / BYTES_PER_MB
}
