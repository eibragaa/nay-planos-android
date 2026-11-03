package br.com.eibragaa.nayplanos.domain.editor

import br.com.eibragaa.nayplanos.domain.model.ActivityDraft
import br.com.eibragaa.nayplanos.domain.model.PlanDay
import br.com.eibragaa.nayplanos.domain.image.ImageReferencePolicy
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class WeeklyPlanEditorReducerTest {
    private val state = WeeklyPlanEditorState(
        days = (0L..4L).map { offset ->
            PlanDay(
                date = LocalDate.of(2026, 11, 9).plusDays(offset),
                activity = ActivityDraft(text = "Atividade ${offset + 1}"),
            )
        },
    )

    @Test
    fun selecionaSomenteIndiceValido() {
        assertEquals(3, WeeklyPlanEditorReducer.selectDay(state, 3).selectedDayIndex)
        assertEquals(state, WeeklyPlanEditorReducer.selectDay(state, -1))
        assertEquals(state, WeeklyPlanEditorReducer.selectDay(state, 5))
    }

    @Test
    fun editarTextoPreservaTextosDosOutrosDiasEEstadoOriginal() {
        val edited = WeeklyPlanEditorReducer.updateText(state, "Nova atividade")

        assertEquals("Nova atividade", edited.selectedDay.activity.text)
        assertEquals("Atividade 2", edited.days[1].activity.text)
        assertEquals("Atividade 1", state.days[0].activity.text)
        assertNotSame(state.days, edited.days)
    }

    @Test
    fun anexarERemoverReferenciaMantemTexto() {
        val attached = WeeklyPlanEditorReducer.attachImageReference(state, "content://imagem/1")
        val removed = WeeklyPlanEditorReducer.removeImageReference(attached)

        assertEquals("content://imagem/1", attached.selectedDay.activity.imageReference)
        assertNull(removed.selectedDay.activity.imageReference)
        assertEquals("Atividade 1", removed.selectedDay.activity.text)
    }

    @Test
    fun anexarReferenciaRemoveEspacosExteriores() {
        val attached = WeeklyPlanEditorReducer.attachImageReference(
            state,
            "  content://imagem/1  ",
        )

        assertEquals("content://imagem/1", attached.selectedDay.activity.imageReference)
    }

    @Test
    fun naoAceitaReferenciaVazia() {
        assertThrows(IllegalArgumentException::class.java) {
            WeeklyPlanEditorReducer.attachImageReference(state, " ")
        }
    }

    @Test
    fun naoAceitaReferenciaAcimaDoLimiteDaPolitica() {
        val oversizedReference = "x".repeat(ImageReferencePolicy.MAX_REFERENCE_LENGTH + 1)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            WeeklyPlanEditorReducer.attachImageReference(state, oversizedReference)
        }

        assertEquals(
            "Referência de imagem inválida: TOO_LONG.",
            exception.message,
        )
    }

    @Test
    fun estadoRejeitaSelecaoForaDosCincoDias() {
        assertThrows(IllegalArgumentException::class.java) {
            state.copy(selectedDayIndex = 5)
        }
    }
}
