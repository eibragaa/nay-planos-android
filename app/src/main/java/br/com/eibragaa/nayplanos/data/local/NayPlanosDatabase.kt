package br.com.eibragaa.nayplanos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WeeklyPlanEntity::class, PlanDayEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NayPlanosDatabase : RoomDatabase() {
    abstract fun weeklyPlanDao(): WeeklyPlanDao
}
