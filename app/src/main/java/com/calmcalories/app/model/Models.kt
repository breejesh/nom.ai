package com.calmcalories.app.model

data class MealEntry(
    val id: Long = 0,
    val name: String,
    val calories: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val foodItems: List<FoodItem> = emptyList(),
)

data class MealExtraction(
    val mealName: String,
    val items: List<FoodItem>,
)

data class FoodItem(
    val food: String,
    val quantity: String,
    val calories: Int,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0,
    val sugarGrams: Int = 0,
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class EntryOption(val label: String) {
    AiImage("AI Image"),
    AiPrompt("AI Prompt"),
    Manual("Manual"),
}

enum class ActivityLevel(val label: String, val multiplier: Float) {
    Sedentary("Sedentary", 1.2f),
    Light("Light", 1.375f),
    Moderate("Moderate", 1.55f),
    Active("Active", 1.725f),
    VeryActive("Very Active", 1.9f),
}

data class DownloadState(
    val progress: Float,
    val bytesRead: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Double,
    val timeRemainingSeconds: Long?
)

