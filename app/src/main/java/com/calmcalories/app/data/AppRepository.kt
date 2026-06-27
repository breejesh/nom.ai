package com.calmcalories.app.data

import com.calmcalories.app.data.entity.MealEntity
import com.calmcalories.app.data.entity.SettingEntity
import com.calmcalories.app.model.ActivityLevel
import com.calmcalories.app.model.FoodItem
import com.calmcalories.app.model.InferenceBackend
import com.calmcalories.app.model.MealEntry
import com.calmcalories.app.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(private val db: AppDatabase) {

    fun observeMeals(): Flow<List<MealEntry>> {
        return db.mealDao().observeAll().map { list ->
            list.map { entity ->
                val items = runCatching {
                    val arr = org.json.JSONArray(entity.foodItemsJson)
                    (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        FoodItem(
                            food = obj.getString("food"),
                            quantity = obj.getString("quantity"),
                            calories = obj.getInt("calories"),
                            proteinGrams = obj.optInt("proteinGrams", 0),
                            carbsGrams = obj.optInt("carbsGrams", 0),
                            fatGrams = obj.optInt("fatGrams", 0),
                            sugarGrams = obj.optInt("sugarGrams", 0)
                        )
                    }
                }.getOrDefault(emptyList())
                MealEntry(id = entity.id, name = entity.name, calories = entity.calories, createdAt = entity.createdAt, foodItems = items)
            }
        }
    }

    suspend fun addMeal(name: String, calories: Int, foodItems: List<FoodItem> = emptyList()) {
        val json = org.json.JSONArray().apply {
            foodItems.forEach { item ->
                put(org.json.JSONObject().apply {
                    put("food", item.food)
                    put("quantity", item.quantity)
                    put("calories", item.calories)
                    put("proteinGrams", item.proteinGrams)
                    put("carbsGrams", item.carbsGrams)
                    put("fatGrams", item.fatGrams)
                    put("sugarGrams", item.sugarGrams)
                })
            }
        }.toString()
        db.mealDao().insert(MealEntity(name = name, calories = calories, foodItemsJson = json))
    }

    suspend fun deleteMeal(id: Long) {
        db.mealDao().delete(id)
    }

    suspend fun updateMeal(id: Long, name: String, calories: Int) {
        val entity = db.mealDao().getById(id) ?: return
        val oldItems = runCatching {
            val arr = org.json.JSONArray(entity.foodItemsJson)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                FoodItem(
                    food = obj.getString("food"),
                    quantity = obj.getString("quantity"),
                    calories = obj.getInt("calories"),
                    proteinGrams = obj.optInt("proteinGrams", 0),
                    carbsGrams = obj.optInt("carbsGrams", 0),
                    fatGrams = obj.optInt("fatGrams", 0),
                    sugarGrams = obj.optInt("sugarGrams", 0)
                )
            }
        }.getOrDefault(emptyList())

        val oldSum = oldItems.sumOf { it.calories }
        val newItems = if (oldSum == 0) {
            if (oldItems.isNotEmpty()) {
                val perItem = calories / oldItems.size
                val remainder = calories % oldItems.size
                oldItems.mapIndexed { idx, item ->
                    val extra = if (idx == oldItems.lastIndex) remainder else 0
                    item.copy(calories = perItem + extra)
                }
            } else {
                emptyList()
            }
        } else {
            val ratio = calories.toDouble() / oldSum.toDouble()
            var currentSum = 0
            oldItems.mapIndexed { idx, item ->
                val scaledCals = if (idx == oldItems.lastIndex) {
                    (calories - currentSum).coerceAtLeast(0)
                } else {
                    val scaled = kotlin.math.round(item.calories * ratio).toInt()
                    currentSum += scaled
                    scaled
                }
                val scaledProtein = kotlin.math.round(item.proteinGrams * ratio).toInt()
                val scaledCarbs = kotlin.math.round(item.carbsGrams * ratio).toInt()
                val scaledFat = kotlin.math.round(item.fatGrams * ratio).toInt()
                val scaledSugar = kotlin.math.round(item.sugarGrams * ratio).toInt()
                item.copy(
                    calories = scaledCals,
                    proteinGrams = scaledProtein,
                    carbsGrams = scaledCarbs,
                    fatGrams = scaledFat,
                    sugarGrams = scaledSugar
                )
            }
        }

        val json = org.json.JSONArray().apply {
            newItems.forEach { item ->
                put(org.json.JSONObject().apply {
                    put("food", item.food)
                    put("quantity", item.quantity)
                    put("calories", item.calories)
                    put("proteinGrams", item.proteinGrams)
                    put("carbsGrams", item.carbsGrams)
                    put("fatGrams", item.fatGrams)
                    put("sugarGrams", item.sugarGrams)
                })
            }
        }.toString()

        db.mealDao().updateFull(id, name, calories, json)
    }

    // ── Daily Goal ──
    fun observeDailyGoal(): Flow<Int> {
        return db.settingDao().observeByKey(KEY_DAILY_GOAL)
            .map { it?.value?.toIntOrNull() ?: 2000 }
    }

    suspend fun setDailyGoal(goal: Int) {
        db.settingDao().upsert(SettingEntity(KEY_DAILY_GOAL, goal.toString()))
    }

    // ── Backend (kept for AI layer, just not exposed in Settings UI) ──
    fun observeBackend(): Flow<InferenceBackend> {
        return db.settingDao().observeByKey(KEY_BACKEND)
            .map { setting ->
                runCatching { InferenceBackend.valueOf(setting?.value ?: InferenceBackend.GPU.name) }
                    .getOrDefault(InferenceBackend.GPU)
            }
    }

    suspend fun setBackend(backend: InferenceBackend) {
        db.settingDao().upsert(SettingEntity(KEY_BACKEND, backend.name))
    }

    // ── User Name ──
    fun observeUserName(): Flow<String> {
        return db.settingDao().observeByKey(KEY_USER_NAME)
            .map { it?.value ?: "" }
    }

    suspend fun setUserName(name: String) {
        db.settingDao().upsert(SettingEntity(KEY_USER_NAME, name))
    }

    // ── Weight (kg) ──
    fun observeWeight(): Flow<Float> {
        return db.settingDao().observeByKey(KEY_WEIGHT)
            .map { it?.value?.toFloatOrNull() ?: 0f }
    }

    suspend fun setWeight(kg: Float) {
        db.settingDao().upsert(SettingEntity(KEY_WEIGHT, kg.toString()))
    }

    // ── Height (cm) ──
    fun observeHeight(): Flow<Float> {
        return db.settingDao().observeByKey(KEY_HEIGHT)
            .map { it?.value?.toFloatOrNull() ?: 0f }
    }

    suspend fun setHeight(cm: Float) {
        db.settingDao().upsert(SettingEntity(KEY_HEIGHT, cm.toString()))
    }

    // ── Activity Level ──
    fun observeActivityLevel(): Flow<ActivityLevel> {
        return db.settingDao().observeByKey(KEY_ACTIVITY)
            .map { setting ->
                runCatching { ActivityLevel.valueOf(setting?.value ?: ActivityLevel.Sedentary.name) }
                    .getOrDefault(ActivityLevel.Sedentary)
            }
    }

    suspend fun setActivityLevel(level: ActivityLevel) {
        db.settingDao().upsert(SettingEntity(KEY_ACTIVITY, level.name))
    }

    // ── Age (years) ──
    fun observeAge(): Flow<Int> {
        return db.settingDao().observeByKey(KEY_AGE)
            .map { it?.value?.toIntOrNull() ?: 28 }
    }

    suspend fun setAge(age: Int) {
        db.settingDao().upsert(SettingEntity(KEY_AGE, age.toString()))
    }

    // ── Dark Theme ──
    fun observeDarkTheme(): Flow<Boolean> {
        return db.settingDao().observeByKey(KEY_DARK_THEME)
            .map { it?.value?.toBoolean() ?: false }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        db.settingDao().upsert(SettingEntity(KEY_DARK_THEME, enabled.toString()))
    }

    // ── Chat History ──
    fun observeChatMessages(): Flow<List<ChatMessage>> {
        return db.settingDao().observeByKey(KEY_CHAT_HISTORY)
            .map { setting ->
                val valStr = setting?.value
                if (valStr.isNullOrBlank()) return@map emptyList<ChatMessage>()
                runCatching {
                    val arr = org.json.JSONArray(valStr)
                    (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        ChatMessage(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            text = obj.getString("text"),
                            isUser = obj.getBoolean("isUser"),
                            timestamp = obj.getLong("timestamp")
                        )
                    }
                }.getOrDefault(emptyList())
            }
    }

    suspend fun saveChatMessages(messages: List<ChatMessage>) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val filtered = messages.filter { it.timestamp >= thirtyDaysAgo }
        val json = org.json.JSONArray().apply {
            filtered.forEach { msg ->
                put(org.json.JSONObject().apply {
                    put("id", msg.id)
                    put("text", msg.text)
                    put("isUser", msg.isUser)
                    put("timestamp", msg.timestamp)
                })
            }
        }.toString()
        db.settingDao().upsert(SettingEntity(KEY_CHAT_HISTORY, json))
    }

    companion object {
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_BACKEND = "inference_backend"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_WEIGHT = "weight_kg"
        private const val KEY_HEIGHT = "height_cm"
        private const val KEY_ACTIVITY = "activity_level"
        private const val KEY_AGE = "user_age"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_CHAT_HISTORY = "chat_history"
    }
}
