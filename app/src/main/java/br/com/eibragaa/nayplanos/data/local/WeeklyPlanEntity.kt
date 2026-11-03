package br.com.eibragaa.nayplanos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Persisted week header. Dates cross the database boundary as ISO-8601 strings. */
@Entity(tableName = "weekly_plans", indices = [Index(value = ["weekStart"], unique = true)])
data class WeeklyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStart: String,
)
