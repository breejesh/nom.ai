package com.calmcalories.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val calories: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val foodItemsJson: String = "",
)
