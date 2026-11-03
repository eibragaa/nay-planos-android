package br.com.eibragaa.nayplanos.domain

import br.com.eibragaa.nayplanos.domain.model.ActivityDraft
import br.com.eibragaa.nayplanos.domain.model.PlanDay
import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan
import br.com.eibragaa.nayplanos.domain.validation.WeeklyPlanError
import br.com.eibragaa.nayplanos.domain.validation.WeeklyPlanValidator
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyPlanValidatorTest {
    private val monday = LocalDate.of(2026, 11, 9)

    @Test
    fun semanaValidaDeSegundaAQuintaSexta() {
        val plan = plano()

        assertTrue(WeeklyPlanValidator.validate(plan).isEmpty())
        assertEquals(listOf(9, 10, 11, 12, 13), plan.days.map { it.date.dayOfMonth })
    }

    @Test
    fun rejeitaDatasForaDeOrdem() {
        val original = plano()
        val reorderedDays = original.days.toMutableList().apply {
            val first = this[0]
            this[0] = this[1]
            this[1] = first
        }
        val plan = original.copy(days = reorderedDays)

        assertTrue(WeeklyPlanError.DAYS_OUT_OF_ORDER_OR_NOT_CONSECUTIVE in WeeklyPlanValidator.validate(plan))
    }

    @Test
    fun rejeitaDiaFaltando() {
        val original = plano()
        val plan = original.copy(days = original.days.dropLast(1))

        assertTrue(WeeklyPlanError.INVALID_DAY_COUNT in WeeklyPlanValidator.validate(plan))
        assertTrue(WeeklyPlanError.DAYS_OUT_OF_ORDER_OR_NOT_CONSECUTIVE in WeeklyPlanValidator.validate(plan))
    }

    @Test
    fun exigeTextoEmDiaLetivo() {
        val original = plano()
        val plan = original.copy(days = original.days.toMutableList().apply {
            this[2] = this[2].copy(activity = ActivityDraft(text = "  "))
        })

        assertTrue(WeeklyPlanError.MISSING_ACTIVITY_TEXT in WeeklyPlanValidator.validate(plan))
    }

    @Test
    fun feriadoDispensaTextoEImagem() {
        val original = plano()
        val plan = original.copy(days = original.days.toMutableList().apply {
            this[2] = this[2].copy(isNoClass = true, activity = ActivityDraft())
        })

        assertFalse(WeeklyPlanError.MISSING_ACTIVITY_TEXT in WeeklyPlanValidator.validate(plan))
        assertTrue(WeeklyPlanValidator.validate(plan).isEmpty())
    }

    private fun plano() = WeeklyPlan(
        weekStart = monday,
        days = (0L..4L).map { offset ->
            PlanDay(
                date = monday.plusDays(offset),
                activity = ActivityDraft(text = "Atividade ${offset + 1}"),
            )
        },
    )
}
