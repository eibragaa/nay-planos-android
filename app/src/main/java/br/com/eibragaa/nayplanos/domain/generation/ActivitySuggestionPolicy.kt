package br.com.eibragaa.nayplanos.domain.generation

/** Validação pura da resposta do serviço. Nunca publica texto; apenas decide se ele pode ir à revisão. */
object ActivitySuggestionPolicy {
    const val SUPPORTED_SCHEMA_VERSION: Int = 1
    const val MAX_ACTIVITY_LENGTH: Int = 1_200
    const val MAX_OBJECTIVES: Int = 5
    const val MAX_OBJECTIVE_LENGTH: Int = 32

    private val objectivePattern = Regex("^EI0[0-9][A-Z0-9.]*$")

    fun validate(suggestion: ActivitySuggestion): List<ActivitySuggestionError> = buildList {
        if (suggestion.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            add(ActivitySuggestionError.UNSUPPORTED_SCHEMA_VERSION)
        }
        val activity = suggestion.activity.trim()
        if (activity.isEmpty()) add(ActivitySuggestionError.BLANK_ACTIVITY)
        if (activity.length > MAX_ACTIVITY_LENGTH) add(ActivitySuggestionError.ACTIVITY_TOO_LONG)
        if (suggestion.objectives.size > MAX_OBJECTIVES) {
            add(ActivitySuggestionError.TOO_MANY_OBJECTIVES)
        }
        if (suggestion.objectives.any { !isValidObjective(it) }) {
            add(ActivitySuggestionError.INVALID_OBJECTIVE)
        }
    }

    /** Normaliza o texto aceito: remove espaços das bordas. Só deve ser chamado após validate() vazio. */
    fun normalizedActivity(suggestion: ActivitySuggestion): String = suggestion.activity.trim()

    private fun isValidObjective(value: String): Boolean =
        value.length <= MAX_OBJECTIVE_LENGTH && objectivePattern.matches(value)
}
