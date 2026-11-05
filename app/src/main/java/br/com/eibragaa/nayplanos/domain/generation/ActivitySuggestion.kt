package br.com.eibragaa.nayplanos.domain.generation

/** Sugestão de atividade devolvida pelo serviço de geração, ainda não revisada pela usuária. */
data class ActivitySuggestion(
    val schemaVersion: Int,
    val activity: String,
    val objectives: List<String>,
)

/** Motivos pelos quais uma sugestão é rejeitada antes de chegar à revisão. */
enum class ActivitySuggestionError {
    UNSUPPORTED_SCHEMA_VERSION,
    BLANK_ACTIVITY,
    ACTIVITY_TOO_LONG,
    TOO_MANY_OBJECTIVES,
    INVALID_OBJECTIVE,
}
