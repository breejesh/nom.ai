package com.calmcalories.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calmcalories.app.ai.GemmaLiteRtService
import com.calmcalories.app.ai.LocalAiExtractor
import com.calmcalories.app.data.AppDatabase
import com.calmcalories.app.data.AppRepository
import com.calmcalories.app.model.ActivityLevel
import com.calmcalories.app.model.InferenceBackend
import com.calmcalories.app.model.MealEntry
import com.calmcalories.app.model.ChatMessage
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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.client.http.FileContent
import java.util.Collections

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

    // Cloud Backup & Restore Dialog States
    private val _showRestoreConfirm = MutableStateFlow(false)
    val showRestoreConfirm = _showRestoreConfirm.asStateFlow()

    private val _showBackupSuccess = MutableStateFlow(false)
    val showBackupSuccess = _showBackupSuccess.asStateFlow()

    private val _showNoBackup = MutableStateFlow(false)
    val showNoBackup = _showNoBackup.asStateFlow()

    private val _showPermissionRequest = MutableStateFlow(false)
    val showPermissionRequest = _showPermissionRequest.asStateFlow()

    private val _showGoogleSignInPrompt = MutableStateFlow(false)
    val showGoogleSignInPrompt = _showGoogleSignInPrompt.asStateFlow()

    private val _backupErrorText = MutableStateFlow<String?>(null)
    val backupErrorText = _backupErrorText.asStateFlow()

    fun dismissBackupDialogs() {
        _showRestoreConfirm.value = false
        _showBackupSuccess.value = false
        _showNoBackup.value = false
        _showPermissionRequest.value = false
        _showGoogleSignInPrompt.value = false
        _backupErrorText.value = null
    }

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

    val chatMessages: StateFlow<List<ChatMessage>> = repository.observeChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCoachBusy = MutableStateFlow(false)
    val isCoachBusy = _isCoachBusy.asStateFlow()

    fun sendMessageToCoach(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val userMsg = ChatMessage(text = text, isUser = true)
            val currentList = chatMessages.value.toMutableList()
            currentList.add(userMsg)
            repository.saveChatMessages(currentList)
            
            _isCoachBusy.value = true
            
            val uName = userName.value
            val target = dailyGoal.value
            val weight = weightKg.value
            val height = heightCm.value
            val ageVal = age.value
            val act = activityLevel.value.label
            val mealsList = meals.value
            
            val systemBrief = getCoachSystemPrompt(uName, target, weight, height, ageVal, act, mealsList)
            val contextText = currentList.takeLast(6).joinToString("\n") { msg ->
                if (msg.isUser) "User: ${msg.text}" else "Eucalyptus: ${msg.text}"
            }
            
            val fullPrompt = "$systemBrief\n\nDialogue History:\n$contextText\n\nEucalyptus:"
            
            val responseResult = withContext(Dispatchers.IO) {
                runCatching {
                    gemmaService.initializeIfNeeded(backend.value).getOrThrow()
                    gemmaService.sendPrompt(fullPrompt, backend.value).getOrThrow()
                }
            }
            
            responseResult.fold(
                onSuccess = { reply ->
                    val cleanReply = reply.trim()
                        .removePrefix("Eucalyptus:")
                        .removePrefix("System:")
                        .trim()
                    
                    val coachMsg = ChatMessage(text = cleanReply, isUser = false)
                    val updatedList = chatMessages.value.toMutableList()
                    updatedList.add(coachMsg)
                    repository.saveChatMessages(updatedList)
                },
                onFailure = { err ->
                    val errorMsg = ChatMessage(text = "Eucalyptus was unable to process your request offline. Please verify that your local model is fully downloaded inside Settings.", isUser = false)
                    val updatedList = chatMessages.value.toMutableList()
                    updatedList.add(errorMsg)
                    repository.saveChatMessages(updatedList)
                }
            )
            
            _isCoachBusy.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.saveChatMessages(emptyList())
        }
    }

    private fun getCoachSystemPrompt(
        userName: String,
        dailyGoal: Int,
        weightKg: Float,
        heightCm: Float,
        age: Int,
        activityLevel: String,
        meals: List<MealEntry>
    ): String {
        val today = startOfDay(System.currentTimeMillis())
        val todayMeals = meals.filter { it.createdAt >= today }
        val totalCalsToday = todayMeals.sumOf { it.calories }
        
        val mealsSummary = todayMeals.joinToString(", ") { meal ->
            "${meal.name} (${meal.calories} kcal)"
        }
        
        return buildString {
            append("System: You are Eucalyptus, a warm, wise, and private on-device health & nutrition coach. ")
            append("Provide brief, encouraging, actionable health advice. Keep answers under 4 sentences. ")
            append("Do not make medical prescriptions; focus on nutrition, habits, and mindfulness. ")
            append("User Profile: ")
            if (userName.isNotBlank()) append("Name: $userName, ")
            append("Daily Calorie Target: $dailyGoal kcal, ")
            if (weightKg > 0) append("Weight: $weightKg kg, ")
            if (heightCm > 0) append("Height: $heightCm cm, ")
            if (age > 0) append("Age: $age years, ")
            append("Activity Level: $activityLevel. ")
            append("Current Intake Today: $totalCalsToday kcal logged. ")
            if (mealsSummary.isNotBlank()) append("Meals eaten today: $mealsSummary. ")
        }
    }

    private fun startOfDay(e: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = e
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }



    fun handleExportClick(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            _showGoogleSignInPrompt.value = true
        } else {
            performExport(context, account)
        }
    }

    fun handleImportClick(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            _showGoogleSignInPrompt.value = true
        } else {
            queryBackupPresence(context, account)
        }
    }

    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton("https://www.googleapis.com/auth/drive.appdata")
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("NomAI")
            .build()
    }

    private fun performExport(context: Context, account: GoogleSignInAccount) {
        viewModelScope.launch(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                try {
                    AppDatabase.get(context).openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                } catch (ce: Exception) {
                    android.util.Log.w("BackupRestore", "WAL checkpoint failed: ${ce.message}")
                }

                val dbFile = context.getDatabasePath("calm_calories.db")
                val dbShm = File(dbFile.parent, "${dbFile.name}-shm")
                val dbWal = File(dbFile.parent, "${dbFile.name}-wal")

                tempFile = File(context.cacheDir, "nomai_backup_temp.zip")
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                FileOutputStream(tempFile).use { outputStream ->
                    ZipOutputStream(outputStream).use { zipOut ->
                        fun addFileToZip(file: File, name: String) {
                            if (file.exists()) {
                                zipOut.putNextEntry(ZipEntry(name))
                                file.inputStream().use { input ->
                                    input.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                            }
                        }
                        addFileToZip(dbFile, "calm_calories.db")
                        addFileToZip(dbShm, "calm_calories.db-shm")
                        addFileToZip(dbWal, "calm_calories.db-wal")
                    }
                }

                val driveService = getDriveService(context, account)
                val queryResult = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = 'nomai_backup.zip' and trashed = false")
                    .setFields("files(id)")
                    .execute()
                val files = queryResult.files

                val mediaContent = FileContent("application/zip", tempFile)
                if (!files.isNullOrEmpty()) {
                    val fileId = files[0].id
                    driveService.files().update(fileId, null, mediaContent).execute()
                } else {
                    val fileMetadata = DriveFile().apply {
                        name = "nomai_backup.zip"
                        parents = Collections.singletonList("appDataFolder")
                    }
                    driveService.files().create(fileMetadata, mediaContent).execute()
                }

                withContext(Dispatchers.Main) {
                    _showBackupSuccess.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("BackupRestore", "Google Drive Export failed", e)
                withContext(Dispatchers.Main) {
                    _backupErrorText.value = e.message ?: "Google Drive connection error"
                }
            } finally {
                tempFile?.delete()
            }
        }
    }

    private fun queryBackupPresence(context: Context, account: GoogleSignInAccount) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context, account)
                val queryResult = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = 'nomai_backup.zip' and trashed = false")
                    .setFields("files(id)")
                    .execute()
                val files = queryResult.files

                withContext(Dispatchers.Main) {
                    if (!files.isNullOrEmpty()) {
                        _showRestoreConfirm.value = true
                    } else {
                        _showNoBackup.value = true
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BackupRestore", "Google Drive Query failed", e)
                withContext(Dispatchers.Main) {
                    _backupErrorText.value = e.message ?: "Google Drive connection error"
                }
            }
        }
    }

    fun performImport(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            _showGoogleSignInPrompt.value = true
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var tempZipFile: File? = null
            try {
                val driveService = getDriveService(context, account)
                val queryResult = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = 'nomai_backup.zip' and trashed = false")
                    .setFields("files(id)")
                    .execute()
                val files = queryResult.files

                if (files.isNullOrEmpty()) {
                    throw java.io.FileNotFoundException("Backup not found on Google Drive.")
                }

                val fileId = files[0].id
                tempZipFile = File(context.cacheDir, "nomai_backup_downloaded.zip")
                if (tempZipFile.exists()) tempZipFile.delete()

                FileOutputStream(tempZipFile).use { outputStream ->
                    driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                }

                val cacheDir = context.cacheDir
                val tempDb = File(cacheDir, "calm_calories_temp.db")
                val tempShm = File(cacheDir, "calm_calories_temp.db-shm")
                val tempWal = File(cacheDir, "calm_calories_temp.db-wal")

                if (tempDb.exists()) tempDb.delete()
                if (tempShm.exists()) tempShm.delete()
                if (tempWal.exists()) tempWal.delete()

                var hasDb = false
                tempZipFile.inputStream().use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            val targetFile = when (entry.name) {
                                "calm_calories.db" -> { hasDb = true; tempDb }
                                "calm_calories.db-shm" -> tempShm
                                "calm_calories.db-wal" -> tempWal
                                else -> null
                            }
                            if (targetFile != null) {
                                targetFile.outputStream().use { output ->
                                    zipIn.copyTo(output)
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                }

                if (!hasDb) {
                    throw IllegalArgumentException("Invalid backup: main database not found inside zip.")
                }

                AppDatabase.get(context).close()
                AppDatabase.reset()

                val dbFile = context.getDatabasePath("calm_calories.db")
                val dbShm = File(dbFile.parent, "${dbFile.name}-shm")
                val dbWal = File(dbFile.parent, "${dbFile.name}-wal")

                fun copyFile(src: File, dest: File) {
                    if (src.exists()) {
                        src.inputStream().use { input ->
                            dest.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        src.delete()
                    } else if (dest.exists()) {
                        dest.delete()
                    }
                }

                copyFile(tempDb, dbFile)
                copyFile(tempShm, dbShm)
                copyFile(tempWal, dbWal)

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Backup restored successfully! Restarting...", android.widget.Toast.LENGTH_SHORT).show()
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            } catch (e: Exception) {
                android.util.Log.e("BackupRestore", "Restore failed", e)
                withContext(Dispatchers.Main) {
                    _backupErrorText.value = e.message ?: "Google Drive restore error"
                }
            } finally {
                tempZipFile?.delete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gemmaService.close()
    }
}
