package com.calmcalories.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calmcalories.app.ai.GemmaLiteRtService
import com.calmcalories.app.ai.LocalAiExtractor
import com.calmcalories.app.data.AppDatabase
import com.calmcalories.app.data.AppRepository
import com.calmcalories.app.model.ActivityLevel
import com.calmcalories.app.model.InferenceBackend
import com.calmcalories.app.model.MealEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(AppDatabase.get(application))
    private val gemmaService = GemmaLiteRtService(application)
    private val extractor = LocalAiExtractor(gemmaService)

    private val _isModelPresent = MutableStateFlow(gemmaService.isModelPresent())
    val isModelPresent = _isModelPresent.asStateFlow()

    private val _downloadState = MutableStateFlow<com.calmcalories.app.model.DownloadState?>(null)
    val downloadState = _downloadState.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError = _downloadError.asStateFlow()

    fun downloadModel(downloadUrl: String) {
        viewModelScope.launch {
            _downloadError.value = null
            _downloadState.value = com.calmcalories.app.model.DownloadState(
                progress = 0f,
                bytesRead = 0,
                totalBytes = 0,
                speedBytesPerSecond = 0.0,
                timeRemainingSeconds = null
            )
            val success = withContext(Dispatchers.IO) {
                var connection: HttpURLConnection? = null
                var tempFile: File? = null
                try {
                    val targetFile = gemmaService.getTargetFile()
                    val parent = targetFile.parentFile
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs()
                    }
                    tempFile = File(parent, "${targetFile.name}.tmp")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }

                    val url = URL(downloadUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.connect()

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        _downloadError.value = "HTTP error code: ${connection.responseCode} (${connection.responseMessage})"
                        return@withContext false
                    }

                    val contentLength = connection.contentLengthLong
                    connection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            var lastProgressUpdate = System.currentTimeMillis()
                            var lastBytesRead = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                val now = System.currentTimeMillis()
                                val timeDiff = now - lastProgressUpdate
                                if (timeDiff >= 400) { // Update stats every 400ms
                                    val progressVal = if (contentLength > 0) totalBytesRead.toFloat() / contentLength.toFloat() else 0f
                                    val bytesSinceLast = totalBytesRead - lastBytesRead
                                    val speed = (bytesSinceLast.toDouble() / (timeDiff.toDouble() / 1000.0))
                                    val timeRemaining = if (contentLength > 0 && speed > 0) {
                                        ((contentLength - totalBytesRead) / speed).toLong()
                                    } else null

                                    _downloadState.value = com.calmcalories.app.model.DownloadState(
                                        progress = progressVal,
                                        bytesRead = totalBytesRead,
                                        totalBytes = contentLength,
                                        speedBytesPerSecond = speed,
                                        timeRemainingSeconds = timeRemaining
                                    )

                                    lastProgressUpdate = now
                                    lastBytesRead = totalBytesRead
                                }
                            }
                        }
                    }

                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    if (tempFile.renameTo(targetFile)) {
                        true
                    } else {
                        _downloadError.value = "Failed to rename temporary download file"
                        false
                    }
                } catch (e: Exception) {
                    _downloadError.value = e.message ?: "Network error"
                    tempFile?.delete()
                    false
                } finally {
                    connection?.disconnect()
                }
            }

            if (success) {
                _downloadState.value = null
                _isModelPresent.value = true
            } else {
                _downloadState.value = null
            }
        }
    }

    val meals: StateFlow<List<MealEntry>> = repository.observeMeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyGoal: StateFlow<Int> = repository.observeDailyGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val backend: StateFlow<InferenceBackend> = repository.observeBackend()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InferenceBackend.GPU)

    val userName: StateFlow<String> = repository.observeUserName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val weightKg: StateFlow<Float> = repository.observeWeight()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val heightCm: StateFlow<Float> = repository.observeHeight()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val activityLevel: StateFlow<ActivityLevel> = repository.observeActivityLevel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActivityLevel.Sedentary)

    val age: StateFlow<Int> = repository.observeAge()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 28)

    /**
     * Suggested daily calories for weight loss using the Mifflin-St Jeor equation.
     * Uses a gender-neutral average and subtracts 500 kcal for ~0.5 kg/week loss.
     * Returns null if weight or height are not yet set.
     */
    val suggestedCalories: StateFlow<Int?> = combine(
        repository.observeWeight(),
        repository.observeHeight(),
        repository.observeActivityLevel(),
        repository.observeAge(),
    ) { w, h, a, ageVal ->
        if (w <= 0f || h <= 0f) return@combine null
        // Mifflin-St Jeor (gender-neutral average: use -78 offset — midpoint of male +5 and female -161)
        val bmr = (10f * w) + (6.25f * h) - (5f * ageVal) - 78f
        val tdee = bmr * a.multiplier
        val deficit = tdee - 500f // 500 kcal daily deficit ≈ 0.5 kg/week
        deficit.toInt().coerceIn(1200, 4000)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private val _processingPrompt = MutableStateFlow<String?>(null)
    val processingPrompt = _processingPrompt.asStateFlow()

    private val _processingImageBytes = MutableStateFlow<ByteArray?>(null)
    val processingImageBytes = _processingImageBytes.asStateFlow()

    private val _mealAddedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val mealAddedEvent = _mealAddedEvent.asSharedFlow()

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            repository.deleteMeal(id)
        }
    }

    fun updateMeal(id: Long, name: String, calories: Int) {
        viewModelScope.launch {
            repository.updateMeal(id, name, calories)
        }
    }

    fun addManualMeal(name: String, calories: Int) {
        viewModelScope.launch {
            repository.addMeal(name, calories)
            _mealAddedEvent.tryEmit("Meal logged: $name")
        }
    }

    fun addByPrompt(prompt: String) {
        viewModelScope.launch {
            _isBusy.value = true
            _processingPrompt.value = prompt
            _processingImageBytes.value = null
            _lastError.value = null
            val result = extractor.extract(
                prompt = prompt,
                imageBytes = null,
                mimeType = null,
                backend = backend.value,
            )
            result.fold(
                onSuccess = { parsed ->
                    if (parsed.items.isEmpty()) {
                        _lastError.value = "No food items detected, entry not added"
                        _mealAddedEvent.tryEmit("No food items detected, entry not added")
                    } else {
                        repository.addMeal(
                            name = parsed.mealName,
                            calories = parsed.items.sumOf { it.calories },
                            foodItems = parsed.items,
                        )
                        _mealAddedEvent.tryEmit("Meal logged: ${parsed.mealName}")
                    }
                },
                onFailure = {
                    _lastError.value = it.message ?: "Failed to add AI prompt entry"
                    _mealAddedEvent.tryEmit("No food items detected, entry not added")
                },
            )
            _processingPrompt.value = null
            _isBusy.value = false
        }
    }

    fun addByImage(imageBytes: ByteArray, mimeType: String?) {
        viewModelScope.launch {
            _isBusy.value = true
            _processingPrompt.value = "AI Image Scan"
            _processingImageBytes.value = imageBytes
            _lastError.value = null
            val result = extractor.extract(
                prompt = "Analyze this meal image and return calories JSON.",
                imageBytes = imageBytes,
                mimeType = mimeType,
                backend = backend.value,
            )
            result.fold(
                onSuccess = { parsed ->
                    if (parsed.items.isEmpty()) {
                        _lastError.value = "No food items detected, entry not added"
                        _mealAddedEvent.tryEmit("No food items detected, entry not added")
                    } else {
                        repository.addMeal(
                            name = parsed.mealName,
                            calories = parsed.items.sumOf { it.calories },
                            foodItems = parsed.items,
                        )
                        _mealAddedEvent.tryEmit("Meal logged: ${parsed.mealName}")
                    }
                },
                onFailure = {
                    _lastError.value = it.message ?: "Failed to add AI image entry"
                    _mealAddedEvent.tryEmit("No food items detected, entry not added")
                },
            )
            _processingImageBytes.value = null
            _processingPrompt.value = null
            _isBusy.value = false
        }
    }

    fun updateGoal(goal: Int) {
        viewModelScope.launch { repository.setDailyGoal(goal.coerceIn(1200, 4000)) }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch { repository.setUserName(name) }
    }

    fun updateWeight(kg: Float) {
        viewModelScope.launch { repository.setWeight(kg) }
    }

    fun updateHeight(cm: Float) {
        viewModelScope.launch { repository.setHeight(cm) }
    }

    fun updateActivityLevel(level: ActivityLevel) {
        viewModelScope.launch { repository.setActivityLevel(level) }
    }

    fun updateAge(ageVal: Int) {
        viewModelScope.launch { repository.setAge(ageVal) }
    }

    val isDarkTheme: StateFlow<Boolean> = repository.observeDarkTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkTheme(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        gemmaService.close()
    }
}
