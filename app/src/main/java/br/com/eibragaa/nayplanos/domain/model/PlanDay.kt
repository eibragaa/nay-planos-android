package br.com.eibragaa.nayplanos.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/** Um dia do plano, letivo ou marcado como feriado/sem aula. */
data class PlanDay(
    val date: LocalDate,
    val activity: ActivityDraft = ActivityDraft(),
    val isNoClass: Boolean = false,
) {
    /** Dia da semana derivado da data, evitando estado duplicado inconsistente. */
    val dayOfWeek: DayOfWeek
        get() = date.dayOfWeek
}
