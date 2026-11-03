package br.com.eibragaa.nayplanos.domain.image

/** Valida e normaliza referências opacas de imagem antes de armazená-las. */
object ImageReferencePolicy {
    const val MAX_REFERENCE_LENGTH: Int = 2_048

    fun normalize(reference: String): ImageReferenceResult {
        val normalized = reference.trim()
        if (normalized.isEmpty()) {
            return ImageReferenceResult.Error(ImageReferenceError.EMPTY)
        }
        if (normalized.length > MAX_REFERENCE_LENGTH) {
            return ImageReferenceResult.Error(ImageReferenceError.TOO_LONG)
        }
        return ImageReferenceResult.Success(normalized)
    }
}

sealed interface ImageReferenceResult {
    data class Success(val value: String) : ImageReferenceResult
    data class Error(val reason: ImageReferenceError) : ImageReferenceResult
}

enum class ImageReferenceError {
    EMPTY,
    TOO_LONG,
}
