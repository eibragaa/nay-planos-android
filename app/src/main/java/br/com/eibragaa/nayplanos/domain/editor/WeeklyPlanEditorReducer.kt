package br.com.eibragaa.nayplanos.domain.editor

import br.com.eibragaa.nayplanos.domain.model.ActivityDraft
import br.com.eibragaa.nayplanos.domain.image.ImageReferencePolicy
import br.com.eibragaa.nayplanos.domain.image.ImageReferenceResult

/** Transformações puras do estado usado na revisão da semana. */
object WeeklyPlanEditorReducer {
    fun selectDay(state: WeeklyPlanEditorState, index: Int): WeeklyPlanEditorState =
        if (index in state.days.indices) state.copy(selectedDayIndex = index) else state

    fun updateText(state: WeeklyPlanEditorState, text: String): WeeklyPlanEditorState =
        updateSelectedDraft(state) { it.copy(text = text) }

    fun attachImageReference(
        state: WeeklyPlanEditorState,
        reference: String,
    ): WeeklyPlanEditorState {
        val normalized = when (val result = ImageReferencePolicy.normalize(reference)) {
            is ImageReferenceResult.Success -> result.value
            is ImageReferenceResult.Error -> throw IllegalArgumentException(
                "Referência de imagem inválida: ${result.reason}.",
            )
        }
        return updateSelectedDraft(state) { it.copy(imageReference = normalized) }
    }

    fun removeImageReference(state: WeeklyPlanEditorState): WeeklyPlanEditorState =
        updateSelectedDraft(state) { it.copy(imageReference = null) }

    private inline fun updateSelectedDraft(
        state: WeeklyPlanEditorState,
        update: (ActivityDraft) -> ActivityDraft,
    ): WeeklyPlanEditorState {
        val updatedDays = state.days.toMutableList()
        val selectedDay = updatedDays[state.selectedDayIndex]
        updatedDays[state.selectedDayIndex] = selectedDay.copy(
            activity = update(selectedDay.activity),
        )
        return state.copy(days = updatedDays)
    }
}
