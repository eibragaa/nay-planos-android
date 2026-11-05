package br.com.eibragaa.nayplanos.domain.generation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivitySuggestionPolicyTest {
    @Test
    fun sugestaoValidaNaoGeraErros() {
        val suggestion = suggestion(activity = "Roda de conversa sobre a família.", objectives = listOf("EI02EO01"))

        assertTrue(ActivitySuggestionPolicy.validate(suggestion).isEmpty())
    }

    @Test
    fun rejeitaVersaoDeSchemaDesconhecida() {
        val errors = ActivitySuggestionPolicy.validate(suggestion(schemaVersion = 2))

        assertTrue(ActivitySuggestionError.UNSUPPORTED_SCHEMA_VERSION in errors)
    }

    @Test
    fun rejeitaAtividadeEmBranco() {
        val errors = ActivitySuggestionPolicy.validate(suggestion(activity = "   "))

        assertTrue(ActivitySuggestionError.BLANK_ACTIVITY in errors)
    }

    @Test
    fun rejeitaAtividadeAcimaDoLimite() {
        val longText = "a".repeat(ActivitySuggestionPolicy.MAX_ACTIVITY_LENGTH + 1)

        val errors = ActivitySuggestionPolicy.validate(suggestion(activity = longText))

        assertTrue(ActivitySuggestionError.ACTIVITY_TOO_LONG in errors)
    }

    @Test
    fun rejeitaMaisObjetivosDoQueOLimite() {
        val objectives = List(ActivitySuggestionPolicy.MAX_OBJECTIVES + 1) { "EI02EO0${(it % 9) + 1}" }

        val errors = ActivitySuggestionPolicy.validate(suggestion(objectives = objectives))

        assertTrue(ActivitySuggestionError.TOO_MANY_OBJECTIVES in errors)
    }

    @Test
    fun rejeitaObjetivoFora_doPadraoEI0() {
        val errors = ActivitySuggestionPolicy.validate(suggestion(objectives = listOf("matematica")))

        assertTrue(ActivitySuggestionError.INVALID_OBJECTIVE in errors)
    }

    @Test
    fun normalizaEspacosDasBordas() {
        val normalized = ActivitySuggestionPolicy.normalizedActivity(suggestion(activity = "  Pintura livre  "))

        assertEquals("Pintura livre", normalized)
    }

    private fun suggestion(
        schemaVersion: Int = ActivitySuggestionPolicy.SUPPORTED_SCHEMA_VERSION,
        activity: String = "Atividade de teste",
        objectives: List<String> = emptyList(),
    ) = ActivitySuggestion(schemaVersion = schemaVersion, activity = activity, objectives = objectives)
}
