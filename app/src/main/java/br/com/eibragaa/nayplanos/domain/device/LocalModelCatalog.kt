package br.com.eibragaa.nayplanos.domain.device

/** Arquivo de um modelo publicado no Hugging Face, com tamanho e SHA-256 verificados via API. */
data class ModelFile(
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
)

/**
 * Modelo local multimodal (texto + imagem) em GGUF quantizado.
 *
 * `estimatedRamMb` é a memória de execução (pesos + codificador de imagem + contexto).
 * É uma estimativa conservadora e deve ser confirmada medindo em aparelho real.
 */
data class LocalModel(
    val id: String,
    val displayName: String,
    val huggingFaceRepo: String,
    val quantization: String,
    val model: ModelFile,
    val mmproj: ModelFile,
    val estimatedRamMb: Long,
    val supportsImages: Boolean = true,
) {
    val totalDownloadBytes: Long get() = model.sizeBytes + mmproj.sizeBytes

    fun downloadUrl(file: ModelFile): String =
        "https://huggingface.co/$huggingFaceRepo/resolve/main/${file.fileName}"
}

/**
 * Escolhas de quantização: Q4_K_M no modelo (melhor equilíbrio de velocidade e qualidade
 * em CPU ARM com dotprod/i8mm) e mmproj Q8_0 no codificador de imagem (qualidade alta,
 * metade do custo de memória do f16).
 */
object LocalModelCatalog {
    val models: List<LocalModel> = listOf(
        LocalModel(
            id = "qwen2-vl-2b",
            displayName = "Qwen2-VL 2B",
            huggingFaceRepo = "ggml-org/Qwen2-VL-2B-Instruct-GGUF",
            quantization = "Q4_K_M",
            model = ModelFile(
                fileName = "Qwen2-VL-2B-Instruct-Q4_K_M.gguf",
                sizeBytes = 986_046_944L,
                sha256 = "5745685d2e607a82a0696c1118e56a2a1ae0901da450fd9cd4f161c6b62867d7",
            ),
            mmproj = ModelFile(
                fileName = "mmproj-Qwen2-VL-2B-Instruct-Q8_0.gguf",
                sizeBytes = 709_883_360L,
                sha256 = "a0ad91f00a7a80dcf84d719a61b00ee2e07b71794f4ee2dfa81a254621a8c418",
            ),
            estimatedRamMb = 2_304,
        ),
        LocalModel(
            id = "qwen2.5-vl-3b",
            displayName = "Qwen2.5-VL 3B",
            huggingFaceRepo = "ggml-org/Qwen2.5-VL-3B-Instruct-GGUF",
            quantization = "Q4_K_M",
            model = ModelFile(
                fileName = "Qwen2.5-VL-3B-Instruct-Q4_K_M.gguf",
                sizeBytes = 1_929_901_056L,
                sha256 = "d02fe9b69ad8cadbbd228e387667af66612c44bed29ffc8eb1e7caf9ac486c12",
            ),
            mmproj = ModelFile(
                fileName = "mmproj-Qwen2.5-VL-3B-Instruct-Q8_0.gguf",
                sizeBytes = 844_757_728L,
                sha256 = "980c9b2f78c04e6cff93d277ada09e768394f112d75db3b4e9dea8a69f9fb904",
            ),
            estimatedRamMb = 3_584,
        ),
        LocalModel(
            id = "qwen2.5-vl-7b",
            displayName = "Qwen2.5-VL 7B",
            huggingFaceRepo = "ggml-org/Qwen2.5-VL-7B-Instruct-GGUF",
            quantization = "Q4_K_M",
            model = ModelFile(
                fileName = "Qwen2.5-VL-7B-Instruct-Q4_K_M.gguf",
                sizeBytes = 4_683_072_032L,
                sha256 = "9258bf05b12686d097ff3b6b18d968ab393649780aa2b3cd67fec43d50554392",
            ),
            mmproj = ModelFile(
                fileName = "mmproj-Qwen2.5-VL-7B-Instruct-Q8_0.gguf",
                sizeBytes = 853_119_712L,
                sha256 = "2ddb555391bae966e412deab9e07b58afa18bcc06930ba0f1c78a3695ab9e506",
            ),
            estimatedRamMb = 6_656,
        ),
    )
}
