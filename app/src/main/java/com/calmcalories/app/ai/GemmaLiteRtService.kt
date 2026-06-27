package com.calmcalories.app.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.calmcalories.app.model.InferenceBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

class GemmaLiteRtService(private val context: Context) {

    @Volatile
    private var engine: Engine? = null
    @Volatile
    private var currentBackend: InferenceBackend? = null

    suspend fun initializeIfNeeded(
        backend: InferenceBackend,
        modelFileName: String = DEFAULT_MODEL_FILE,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (engine != null && currentBackend == backend) return@runCatching

            engine?.close()

            val modelPath = ensureModelPresent(modelFileName)
            val backendImpl = resolveBackend(backend)
            val config = EngineConfig(
                modelPath = modelPath,
                backend = backendImpl,
                visionBackend = Backend.GPU(),
                audioBackend = Backend.CPU(),
                maxNumTokens = 1024,
                maxNumImages = 1,
                cacheDir = context.cacheDir.absolutePath,
            )
            val created = Engine(config)
            created.initialize()
            engine = created
            currentBackend = backend
        }
    }

    /**
     * Send a text-only prompt to the model.
     */
    suspend fun sendPrompt(
        prompt: String,
        backend: InferenceBackend,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            initializeIfNeeded(backend).getOrThrow()
            val current = engine ?: error("Engine not initialized")
            current.createConversation().use { conversation ->
                val response = conversation.sendMessage(prompt)
                response.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
            }
        }
    }

    /**
     * Send a multimodal prompt (text + image bytes) to the model.
     */
    suspend fun sendMultimodalPrompt(
        prompt: String,
        imageBytes: ByteArray,
        backend: InferenceBackend,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            initializeIfNeeded(backend).getOrThrow()
            val current = engine ?: error("Engine not initialized")
            current.createConversation().use { conversation ->
                val payload = Contents.of(
                    Content.ImageBytes(imageBytes),
                    Content.Text(prompt),
                )
                val response = conversation.sendMessage(payload)
                response.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
            }
        }
    }

    suspend fun benchmark(prompt: String, backend: InferenceBackend): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            var output = ""
            val elapsed = measureTimeMillis {
                output = sendPrompt(prompt, backend).getOrThrow()
            }
            if (output.isBlank()) error("Blank model response")
            elapsed
        }
    }

    fun close() {
        engine?.close()
        engine = null
        currentBackend = null
    }

    fun isModelPresent(modelFileName: String = DEFAULT_MODEL_FILE): Boolean {
        val externalTarget = context.getExternalFilesDir(null)?.let { File(it, modelFileName) }
        if (externalTarget != null && externalTarget.exists()) return true
        if (File(context.filesDir, modelFileName).exists()) return true

        val alternativeName = if (modelFileName.contains("E2B")) {
            modelFileName.replace("E2B", "e2b")
        } else {
            modelFileName.replace("e2b", "E2B")
        }
        val altExternal = context.getExternalFilesDir(null)?.let { File(it, alternativeName) }
        if (altExternal != null && altExternal.exists()) return true
        if (File(context.filesDir, alternativeName).exists()) return true
        return false
    }

    fun getTargetFile(modelFileName: String = DEFAULT_MODEL_FILE): File {
        val externalDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(externalDir, modelFileName)
    }

    private fun resolveBackend(backend: InferenceBackend): Backend {
        return when (backend) {
            InferenceBackend.CPU -> Backend.CPU()
            InferenceBackend.GPU -> Backend.GPU()
            InferenceBackend.NPU -> Backend.NPU(
                nativeLibraryDir = context.applicationInfo.nativeLibraryDir,
            )
        }
    }

    private fun ensureModelPresent(modelFileName: String): String {
        // 1. Check external files directory (recommended for large models via adb push)
        val externalTarget = context.getExternalFilesDir(null)?.let { File(it, modelFileName) }
        if (externalTarget != null && externalTarget.exists()) {
            return externalTarget.absolutePath
        }

        // 2. Check internal storage files directory
        val internalTarget = File(context.filesDir, modelFileName)
        if (internalTarget.exists()) {
            return internalTarget.absolutePath
        }

        // 3. Fallback to copy from assets if it exists there (for smaller models)
        try {
            context.assets.open("models/$modelFileName").use { input ->
                internalTarget.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return internalTarget.absolutePath
        } catch (_: java.io.IOException) {
            // Keep trying case-insensitive name just in case
            val alternativeName = if (modelFileName.contains("E2B")) {
                modelFileName.replace("E2B", "e2b")
            } else {
                modelFileName.replace("e2b", "E2B")
            }
            val altExternal = context.getExternalFilesDir(null)?.let { File(it, alternativeName) }
            if (altExternal != null && altExternal.exists()) {
                return altExternal.absolutePath
            }
            if (File(context.filesDir, alternativeName).exists()) {
                return File(context.filesDir, alternativeName).absolutePath
            }

            throw IllegalStateException(
                "Model '$modelFileName' not found.\n" +
                "To install, push it to your device's external files directory:\n" +
                "adb push <path_to_model>/$modelFileName /sdcard/Android/data/com.calmcalories.app/files/"
            )
        }
    }

    companion object {
        // Default model filename (case-matched to what is on disk)
        private const val DEFAULT_MODEL_FILE = "gemma-4-E2B-it.litertlm"
    }
}
