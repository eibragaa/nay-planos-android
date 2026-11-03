package br.com.eibragaa.nayplanos.data.repository

import br.com.eibragaa.nayplanos.data.local.WeeklyPlanDao
import br.com.eibragaa.nayplanos.data.local.WeeklyPlanMapper
import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan
import br.com.eibragaa.nayplanos.domain.repository.WeeklyPlanRepository
import br.com.eibragaa.nayplanos.domain.validation.WeeklyPlanValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed append-only repository; editing/upsert will be added in a later iteration. */
class RoomWeeklyPlanRepository(
    private val dao: WeeklyPlanDao,
) : WeeklyPlanRepository {
    override fun observeAll(): Flow<List<WeeklyPlan>> =
        dao.observeAllByWeekDescending().map { plans ->
            plans.map { plan -> WeeklyPlanMapper.run { plan.toDomain() } }
        }

    override suspend fun save(plan: WeeklyPlan): Long {
        val errors = WeeklyPlanValidator.validate(plan)
        require(errors.isEmpty()) { "Invalid weekly plan: ${errors.joinToString()}" }

        val planEntity = WeeklyPlanMapper.run { plan.toEntity() }
        val dayEntities = plan.days.map { day ->
            WeeklyPlanMapper.run { day.toEntity(planId = 0L) }
        }
        return dao.insertPlanWithDays(planEntity, dayEntities)
    }
}
