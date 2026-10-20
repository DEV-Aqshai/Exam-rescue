package com.example.ai.providers

import com.example.ai.models.AiMessage
import com.example.ai.models.AiRequest
import com.example.ai.models.AiResponse
import com.example.ai.models.AiResult
import com.example.core.database.entities.AiProviderConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

interface AiProvider {
    val id: String
    suspend fun testConnection(config: AiProviderConfigEntity, apiKey: String): AiResult<Unit>
    suspend fun generate(request: AiRequest, config: AiProviderConfigEntity, apiKey: String): AiResult<AiResponse>
}

class GeminiProvider : AiProvider {
    override val id: String = "gemini"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(config: AiProviderConfigEntity, apiKey: String): AiResult<Unit> {
        val testRequest = AiRequest(
            messages = listOf(AiMessage(role = "user", content = "Reply with 'OK' if you can read this test message."))
        )
        return when (val result = generate(testRequest, config, apiKey)) {
            is AiResult.Success -> AiResult.Success(Unit)
            is AiResult.InvalidKey -> AiResult.InvalidKey
            is AiResult.QuotaExceeded -> AiResult.QuotaExceeded
            is AiResult.Offline -> AiResult.Offline
            is AiResult.Timeout -> AiResult.Timeout
            is AiResult.ProviderError -> result
            is AiResult.MalformedResponse -> result
        }
    }

    override suspend fun generate(
        request: AiRequest,
        config: AiProviderConfigEntity,
        apiKey: String
    ): AiResult<AiResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext AiResult.InvalidKey

        val modelId = config.modelId.ifBlank { "gemini-2.5-flash" }
        val baseUrl = config.endpoint.ifBlank { "https://generativelanguage.googleapis.com/v1beta" }
        val url = "$baseUrl/models/$modelId:generateContent?key=$apiKey"

        val contentsArray = JSONArray()
        for (msg in request.messages) {
            val role = if (msg.role == "assistant") "model" else "user"
            val partsArray = JSONArray().put(JSONObject().put("text", msg.content))
            contentsArray.put(JSONObject().put("role", role).put("parts", partsArray))
        }

        val requestJson = JSONObject()
        requestJson.put("contents", contentsArray)

        if (!request.systemPrompt.isNullOrBlank()) {
            val systemParts = JSONArray().put(JSONObject().put("text", request.systemPrompt))
            requestJson.put("system_instruction", JSONObject().put("parts", systemParts))
        }

        val genConfig = JSONObject()
            .put("temperature", request.temperature)
            .put("maxOutputTokens", request.maxTokens)
        requestJson.put("generationConfig", genConfig)

        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                when (response.code) {
                    200 -> {
                        val json = JSONObject(responseBody)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val replyText = parts.getJSONObject(0).optString("text", "")
                                val finishReason = firstCandidate.optString("finishReason", "STOP")
                                return@withContext AiResult.Success(
                                    AiResponse(text = replyText, modelUsed = modelId, rawFinishReason = finishReason)
                                )
                            }
                        }
                        AiResult.MalformedResponse("No response content returned by Gemini")
                    }
                    400 -> AiResult.ProviderError(400, "Bad Request or unsupported model: $modelId")
                    401, 403 -> AiResult.InvalidKey
                    429 -> AiResult.QuotaExceeded
                    else -> AiResult.ProviderError(response.code, "Gemini returned code ${response.code}")
                }
            }
        } catch (e: UnknownHostException) {
            AiResult.Offline
        } catch (e: SocketTimeoutException) {
            AiResult.Timeout
        } catch (e: IOException) {
            AiResult.Offline
        } catch (e: Exception) {
            AiResult.MalformedResponse(e.message ?: "Unexpected error")
        }
    }
}

class OpenAiCompatibleProvider(override val id: String) : AiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(config: AiProviderConfigEntity, apiKey: String): AiResult<Unit> {
        val testRequest = AiRequest(
            messages = listOf(AiMessage(role = "user", content = "Test: please reply 'OK'."))
        )
        return when (val result = generate(testRequest, config, apiKey)) {
            is AiResult.Success -> AiResult.Success(Unit)
            is AiResult.InvalidKey -> AiResult.InvalidKey
            is AiResult.QuotaExceeded -> AiResult.QuotaExceeded
            is AiResult.Offline -> AiResult.Offline
            is AiResult.Timeout -> AiResult.Timeout
            is AiResult.ProviderError -> result
            is AiResult.MalformedResponse -> result
        }
    }

    override suspend fun generate(
        request: AiRequest,
        config: AiProviderConfigEntity,
        apiKey: String
    ): AiResult<AiResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext AiResult.InvalidKey

        val endpoint = config.endpoint.ifBlank { "https://openrouter.ai/api/v1/chat/completions" }
        val model = config.modelId.ifBlank { "gpt-4o-mini" }

        val messagesArray = JSONArray()
        if (!request.systemPrompt.isNullOrBlank()) {
            messagesArray.put(JSONObject().put("role", "system").put("content", request.systemPrompt))
        }
        for (msg in request.messages) {
            messagesArray.put(JSONObject().put("role", msg.role).put("content", msg.content))
        }

        val requestJson = JSONObject()
            .put("model", model)
            .put("messages", messagesArray)
            .put("temperature", request.temperature)
            .put("max_tokens", request.maxTokens)

        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val httpRequestBuilder = Request.Builder()
            .url(endpoint)
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")

        if (id == "openrouter") {
            httpRequestBuilder.addHeader("HTTP-Referer", "https://examrescue.app")
            httpRequestBuilder.addHeader("X-Title", "Exam Rescue Android")
        }

        try {
            client.newCall(httpRequestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                when (response.code) {
                    200 -> {
                        val json = JSONObject(responseBody)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val firstChoice = choices.getJSONObject(0)
                            val messageObj = firstChoice.optJSONObject("message")
                            val text = messageObj?.optString("content", "") ?: ""
                            val finishReason = firstChoice.optString("finish_reason", "stop")
                            return@withContext AiResult.Success(
                                AiResponse(text = text, modelUsed = model, rawFinishReason = finishReason)
                            )
                        }
                        AiResult.MalformedResponse("No response content returned by provider")
                    }
                    401, 403 -> AiResult.InvalidKey
                    429 -> AiResult.QuotaExceeded
                    400 -> AiResult.ProviderError(400, "Bad Request or unsupported model: $model")
                    else -> AiResult.ProviderError(response.code, "Provider returned HTTP code ${response.code}")
                }
            }
        } catch (e: UnknownHostException) {
            AiResult.Offline
        } catch (e: SocketTimeoutException) {
            AiResult.Timeout
        } catch (e: IOException) {
            AiResult.Offline
        } catch (e: Exception) {
            AiResult.MalformedResponse(e.message ?: "Unexpected error")
        }
    }
}

object AiProviderFactory {
    fun getProvider(providerId: String): AiProvider {
        return when (providerId) {
            "gemini" -> GeminiProvider()
            else -> OpenAiCompatibleProvider(providerId)
        }
    }
}
