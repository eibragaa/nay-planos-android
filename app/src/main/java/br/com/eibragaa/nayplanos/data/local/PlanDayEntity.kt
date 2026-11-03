package br.com.eibragaa.nayplanos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A day and its editable activity; imageReference is an opaque external reference. */
@Entity(
    tableName = "plan_days",
    foreignKeys = [
        ForeignKey(
            entity = WeeklyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["planId", "date"], unique = true)],
)
data class PlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val date: String,
    val activityText: String,
    val imageReference: String?,
    val isNoClass: Boolean,
)
