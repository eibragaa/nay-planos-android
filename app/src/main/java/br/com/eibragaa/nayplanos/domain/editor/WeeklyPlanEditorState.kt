package br.com.eibragaa.nayplanos.domain.editor

import br.com.eibragaa.nayplanos.domain.model.PlanDay

/** Estado imutável da revisão de um plano semanal. */
data class WeeklyPlanEditorState(
    val days: List<PlanDay>,
    val selectedDayIndex: Int = 0,
) {
    init {
        require(days.size == WEEK_LENGTH) { "O editor exige exatamente cinco dias." }
        require(days.first().date.dayOfWeek == java.time.DayOfWeek.MONDAY) {
            "A semana do editor deve começar na segunda-feira."
        }
        require(days.zipWithNext().all { (first, second) ->
            second.date == first.date.plusDays(1)
        }) { "Os dias do editor devem ser consecutivos e estar em ordem." }
        require(selectedDayIndex in days.indices) { "O dia selecionado deve existir." }
    }

    val selectedDay: PlanDay
        get() = days[selectedDayIndex]

    companion object {
        const val WEEK_LENGTH = 5
    }
}
