package br.com.eibragaa.nayplanos.data.local

import br.com.eibragaa.nayplanos.domain.model.ActivityDraft
import br.com.eibragaa.nayplanos.domain.model.PlanDay
import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan

/** Explicit conversions between Room rows and domain objects. */
object WeeklyPlanMapper {
    fun WeeklyPlan.toEntity(): WeeklyPlanEntity =
        WeeklyPlanEntity(weekStart = LocalDateCodec.encode(weekStart))

    fun PlanDay.toEntity(planId: Long): PlanDayEntity =
        PlanDayEntity(
            planId = planId,
            date = LocalDateCodec.encode(date),
            activityText = activity.text,
            imageReference = activity.imageReference,
            isNoClass = isNoClass,
        )

    fun WeeklyPlanWithDays.toDomain(): WeeklyPlan =
        WeeklyPlan(
            weekStart = LocalDateCodec.decode(plan.weekStart),
            days = days.map { day ->
                val date = LocalDateCodec.decode(day.date)
                date to PlanDay(
                    date = date,
                    activity = ActivityDraft(day.activityText, day.imageReference),
                    isNoClass = day.isNoClass,
                )
            }.sortedBy { (date, _) -> date }.map { (_, day) -> day },
        )
}
