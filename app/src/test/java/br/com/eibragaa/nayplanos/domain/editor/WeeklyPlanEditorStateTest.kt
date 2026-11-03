package br.com.eibragaa.nayplanos.domain.editor

import br.com.eibragaa.nayplanos.domain.model.ActivityDraft
import br.com.eibragaa.nayplanos.domain.model.PlanDay
import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class WeeklyPlanEditorStateTest {
    @Test
    fun rejeitaCincoDiasConsecutivosQueComecamEmOutroDiaDaSemana() {
        val tuesdayStart = LocalDate.of(2026, 11, 10)
        val consecutiveDays = (0L..4L).map { offset ->
            PlanDay(
                date = tuesdayStart.plusDays(offset),
                activity = ActivityDraft(text = "Atividade ${offset + 1}"),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            WeeklyPlanEditorState(days = consecutiveDays)
        }
    }
}
