package br.com.eibragaa.nayplanos.domain.model

import java.time.LocalDate

/** Plano de uma semana; a validação de conteúdo é feita por [WeeklyPlanValidator]. */
data class WeeklyPlan(
    val weekStart: LocalDate,
    val days: List<PlanDay>,
)
