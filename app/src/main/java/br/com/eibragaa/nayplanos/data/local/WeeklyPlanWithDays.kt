package br.com.eibragaa.nayplanos.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class WeeklyPlanWithDays(
    @Embedded val plan: WeeklyPlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId") val days: List<PlanDayEntity>,
)
