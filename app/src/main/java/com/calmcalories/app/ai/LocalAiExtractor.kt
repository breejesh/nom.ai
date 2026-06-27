package com.calmcalories.app.ai

import com.calmcalories.app.model.FoodItem
import com.calmcalories.app.model.InferenceBackend
import com.calmcalories.app.model.MealExtraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LocalAiExtractor(
    private val gemmaLiteRtService: GemmaLiteRtService,
) {
    suspend fun extract(
        prompt: String,
        imageBytes: ByteArray?,
        mimeType: String?,
        backend: InferenceBackend,
    ): Result<MealExtraction> = withContext(Dispatchers.IO) {
        runCatching {
            gemmaLiteRtService.initializeIfNeeded(backend).getOrThrow()

            val systemPrompt = buildString {
                append("System: You are an expert food nutritionist. Extract food items, calories, macronutrients (protein, carbs, and fat in grams), and sugar (in grams). ")
                append("Estimate reasonably if details are vague. NEVER return 0 calories or 0 macros for solid food. ")
                append("If the input or image does not contain any food items, return an empty items list: {\"mealName\":\"\",\"items\":[]}. ")
                append("Return ONLY the JSON. No markdown backticks, no code block formatting, no text before or after.\n\n")
                append("Example Input: 'double espresso and a croissant'\n")
                append("Example Output: {\"mealName\":\"Espresso & Croissant\",\"items\":[{\"food\":\"Double Espresso\",\"quantity\":\"1 cup\",\"calories\":5,\"proteinGrams\":0,\"carbsGrams\":1,\"fatGrams\":0,\"sugarGrams\":0},{\"food\":\"Croissant\",\"quantity\":\"1 medium\",\"calories\":270,\"proteinGrams\":6,\"carbsGrams\":32,\"fatGrams\":13,\"sugarGrams\":8}]}\n\n")
                append("User Input: ")
                append(prompt)
            }

            val raw = if (imageBytes != null) {
                gemmaLiteRtService.sendMultimodalPrompt(
                    prompt = systemPrompt,
                    imageBytes = imageBytes,
                    backend = backend,
                ).getOrThrow()
            } else {
                gemmaLiteRtService.sendPrompt(
                    prompt = systemPrompt,
                    backend = backend,
                ).getOrThrow()
            }

            parseExtraction(raw)
        }
    }

    private fun parseExtraction(raw: String): MealExtraction {
        val jsonText = extractJsonObject(raw)
            ?: throw IllegalStateException("Could not find valid JSON in model response: ${raw.take(200)}")

        val json = JSONObject(jsonText)
        val itemsArray = json.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.optJSONObject(i) ?: continue
                val calVal = item.opt("calories")
                val parsedCals = when (calVal) {
                    is Number -> calVal.toInt()
                    is String -> calVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                    else -> 0
                }
                val calories = if (parsedCals == 0) {
                    val nameLower = item.optString("food", "").lowercase()
                    when {
                        nameLower.contains("water") || nameLower.contains("diet soda") || nameLower.contains("coke zero") -> 0
                        nameLower.contains("black coffee") || nameLower.contains("espresso") -> 5
                        nameLower.contains("tea") -> 2
                        nameLower.contains("croissant") || nameLower.contains("pastry") || nameLower.contains("cake") -> 300
                        nameLower.contains("bread") || nameLower.contains("toast") -> 80
                        nameLower.contains("egg") -> 70
                        nameLower.contains("salad") -> 120
                        nameLower.contains("rice") -> 200
                        nameLower.contains("chicken") || nameLower.contains("meat") || nameLower.contains("beef") || nameLower.contains("fish") -> 220
                        else -> 150 // safe baseline fallback for unparsed solid food items
                    }
                } else {
                    parsedCals
                }

                val pVal = item.opt("proteinGrams")
                val protein = when (pVal) {
                    is Number -> pVal.toInt()
                    is String -> pVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                    else -> 0
                }

                val cVal = item.opt("carbsGrams")
                val carbs = when (cVal) {
                    is Number -> cVal.toInt()
                    is String -> cVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                    else -> 0
                }

                val fVal = item.opt("fatGrams")
                val fat = when (fVal) {
                    is Number -> fVal.toInt()
                    is String -> fVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                    else -> 0
                }

                val sVal = item.opt("sugarGrams")
                val sugar = when (sVal) {
                    is Number -> sVal.toInt()
                    is String -> sVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                    else -> 0
                }

                add(
                    FoodItem(
                        food = item.optString("food", "Unknown item"),
                        quantity = item.optString("quantity", "1 serving"),
                        calories = calories,
                        proteinGrams = protein,
                        carbsGrams = carbs,
                        fatGrams = fat,
                        sugarGrams = sugar,
                    )
                )
            }
        }

        val mealName = json.optString("mealName", "").let { name ->
            if (name.isBlank() && items.isNotEmpty()) {
                items.first().food
            } else if (name.isBlank()) {
                "Logged Meal"
            } else {
                name
            }
        }

        return MealExtraction(
            mealName = mealName,
            items = items,
        )
    }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start == -1) return null

        var depth = 0
        for (i in start until raw.length) {
            when (raw[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
