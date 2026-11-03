package br.com.eibragaa.nayplanos.domain.validation

import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

/** Erros de conteúdo e sequência encontrados em um plano semanal. */
enum class WeeklyPlanError {
    INVALID_DAY_COUNT,
    WEEK_MUST_START_ON_MONDAY,
    DAYS_OUT_OF_ORDER_OR_NOT_CONSECUTIVE,
    MISSING_ACTIVITY_TEXT,
}

/** Validação pura da estrutura de segunda a sexta e da atividade de cada dia letivo. */
object WeeklyPlanValidator {
    /** Retorna todos os problemas encontrados; uma lista vazia significa plano válido. */
    fun validate(plan: WeeklyPlan): List<WeeklyPlanError> = buildList {
        if (plan.days.size != 5) add(WeeklyPlanError.INVALID_DAY_COUNT)
        if (plan.weekStart.dayOfWeek != DayOfWeek.MONDAY) {
            add(WeeklyPlanError.WEEK_MUST_START_ON_MONDAY)
        }

        val datesMatchExpectedWeek = plan.days.size == 5 && plan.days.withIndex().all { (index, day) ->
            day.date == plan.weekStart.plus(index.toLong(), ChronoUnit.DAYS)
        }
        if (!datesMatchExpectedWeek) {
            add(WeeklyPlanError.DAYS_OUT_OF_ORDER_OR_NOT_CONSECUTIVE)
        }

        if (plan.days.any { day -> !day.isNoClass && day.activity.text.isBlank() }) {
            add(WeeklyPlanError.MISSING_ACTIVITY_TEXT)
        }
    }
}
