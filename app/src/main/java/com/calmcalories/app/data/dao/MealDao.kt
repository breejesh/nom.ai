package com.calmcalories.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.calmcalories.app.data.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getById(id: Long): MealEntity?

    @Insert
    suspend fun insert(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE meals SET name = :name, calories = :calories, foodItemsJson = :foodItemsJson WHERE id = :id")
    suspend fun updateFull(id: Long, name: String, calories: Int, foodItemsJson: String)
}
