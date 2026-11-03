package br.com.eibragaa.nayplanos.domain.repository

import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan
import kotlinx.coroutines.flow.Flow

/** Access to persisted weekly plans. The initial implementation is append-only; editing/upsert comes later. */
interface WeeklyPlanRepository {
    fun observeAll(): Flow<List<WeeklyPlan>>

    /** Appends a new plan and returns its generated identifier. */
    suspend fun save(plan: WeeklyPlan): Long
}
