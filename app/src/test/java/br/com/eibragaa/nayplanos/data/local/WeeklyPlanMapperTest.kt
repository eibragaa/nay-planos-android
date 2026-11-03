package br.com.eibragaa.nayplanos.data.local

import br.com.eibragaa.nayplanos.domain.model.ActivityDraft
import br.com.eibragaa.nayplanos.domain.model.PlanDay
import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class WeeklyPlanMapperTest {
    @Test
    fun `data usa formato ISO-8601 e retorna ao mesmo valor`() {
        val date = LocalDate.of(2026, 11, 9)

        assertEquals("2026-11-09", LocalDateCodec.encode(date))
        assertEquals(date, LocalDateCodec.decode("2026-11-09"))
    }

    @Test
    fun `mapper preserva atividade e ordena varios dias pela data decodificada`() {
        val start = LocalDate.of(2026, 11, 9)
        val week = WeeklyPlan(start, listOf(
            PlanDay(start.plusDays(3), ActivityDraft("Peixes", "content://quinta")),
            PlanDay(start, ActivityDraft("Dentro e fora", null)),
            PlanDay(start.plusDays(1), ActivityDraft("Frutas", "content://terca")),
        ))
        val header = with(WeeklyPlanMapper) { week.toEntity() }
        val days = with(WeeklyPlanMapper) { week.days.map { it.toEntity(42) } }
        val restored = with(WeeklyPlanMapper) {
            WeeklyPlanWithDays(header.copy(id = 42), days).toDomain()
        }

        assertEquals("2026-11-09", header.weekStart)
        assertEquals(listOf(42L, 42L, 42L), days.map { it.planId })
        assertEquals(listOf(start, start.plusDays(1), start.plusDays(3)), restored.days.map { it.date })
        assertEquals("content://quinta", restored.days.last().activity.imageReference)
        assertNull(restored.days.first().activity.imageReference)
        assertEquals("Dentro e fora", restored.days.first().activity.text)
    }

    @Test
    fun `mapper preserva dia sem aula e imagem nula`() {
        val date = LocalDate.of(2026, 11, 10)
        val storedDay = PlanDayEntity(
            planId = 7,
            date = LocalDateCodec.encode(date),
            activityText = "Feriado",
            imageReference = null,
            isNoClass = true,
        )
        val restored = WeeklyPlanWithDays(
            WeeklyPlanEntity(id = 7, weekStart = "2026-11-09"),
            listOf(storedDay),
        ).toDomain().days.single()

        assertEquals(date, restored.date)
        assertEquals(true, restored.isNoClass)
        assertNull(restored.activity.imageReference)
    }

    @Test
    fun `codec rejeita data invalida`() {
        assertThrows(java.time.format.DateTimeParseException::class.java) {
            LocalDateCodec.decode("2026-02-30")
        }
    }
}
