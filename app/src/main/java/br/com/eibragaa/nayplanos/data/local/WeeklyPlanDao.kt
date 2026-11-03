package br.com.eibragaa.nayplanos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.temporal.ChronoUnit

@Dao
abstract class WeeklyPlanDao {
    @Transaction
    @Query("SELECT * FROM weekly_plans WHERE id = :id")
    abstract fun observeById(id: Long): Flow<WeeklyPlanWithDays?>

    @Transaction
    @Query("SELECT * FROM weekly_plans ORDER BY weekStart DESC, id DESC")
    abstract fun observeAllByWeekDescending(): Flow<List<WeeklyPlanWithDays>>

    /** Inserts a complete five-day plan as one transaction. */
    @Transaction
    open suspend fun insertPlanWithDays(plan: WeeklyPlanEntity, days: List<PlanDayEntity>): Long {
        requireValidWeek(plan, days)
        val planId = insertPlanRow(plan)
        insertDayRows(days.map { it.copy(planId = planId) })
        return planId
    }

    /** Updates the five existing day rows atomically; every row must retain its database id. */
    @Transaction
    open suspend fun updatePlanDays(planId: Long, days: List<PlanDayEntity>) {
        require(days.size == DAYS_PER_WEEK) { "A weekly plan must contain exactly five days." }
        require(days.all { it.planId == planId }) { "All days must belong to the requested plan." }
        val updatedRows = updateDayRows(days)
        check(updatedRows == DAYS_PER_WEEK) { "Expected five existing day rows, updated $updatedRows." }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertPlanRow(plan: WeeklyPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertDayRows(days: List<PlanDayEntity>)

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun updateDayRows(days: List<PlanDayEntity>): Int

    @Query("DELETE FROM weekly_plans WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    private fun requireValidWeek(plan: WeeklyPlanEntity, days: List<PlanDayEntity>) {
        require(days.size == DAYS_PER_WEEK) { "A weekly plan must contain exactly five days." }
        val start = LocalDateCodec.decode(plan.weekStart)
        val dates = days.map { LocalDateCodec.decode(it.date) }.sorted()
        require(dates.first() == start) { "The first day must match the plan week start." }
        require(dates.all { ChronoUnit.DAYS.between(start, it) in 0L..4L }) {
            "Plan days must fall within Monday through Friday of the selected week."
        }
        require(dates.distinct().size == DAYS_PER_WEEK) { "Plan days must have unique dates." }
    }

    private companion object {
        const val DAYS_PER_WEEK = 5
    }
}
