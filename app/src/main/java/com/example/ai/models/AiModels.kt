package com.example.ai.models

enum class ProviderType(
    val id: String,
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val isCustom: Boolean
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-2.5-flash",
        isCustom = false
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultEndpoint = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "google/gemini-2.5-flash",
        isCustom = false
    ),
    FEATHERLESS(
        id = "featherless",
        displayName = "Featherless AI",
        defaultEndpoint = "https://api.featherless.ai/v1/chat/completions",
        defaultModel = "meta-llama/Meta-Llama-3.1-8B-Instruct",
        isCustom = false
    ),
    XAI(
        id = "xai",
        displayName = "xAI (Grok)",
        defaultEndpoint = "https://api.x.ai/v1/chat/completions",
        defaultModel = "grok-2-mini",
        isCustom = false
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom OpenAI-Compatible",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini",
        isCustom = true
    );

    companion object {
        fun fromId(id: String): ProviderType {
            return entries.firstOrNull { it.id == id } ?: GEMINI
        }
    }
}

sealed class AiResult<out T> {
    data class Success<out T>(val data: T) : AiResult<T>()
    object Offline : AiResult<Nothing>()
    object InvalidKey : AiResult<Nothing>()
    object QuotaExceeded : AiResult<Nothing>()
    object Timeout : AiResult<Nothing>()
    data class ProviderError(val code: Int, val message: String) : AiResult<Nothing>()
    data class MalformedResponse(val details: String) : AiResult<Nothing>()
}

data class AiMessage(
    val role: String, // system, user, assistant
    val content: String
)

data class AiRequest(
    val messages: List<AiMessage>,
    val systemPrompt: String? = null,
    val maxTokens: Int = 1000,
    val temperature: Float = 0.7f,
    val responseLanguage: String = "ENGLISH" // TAMIL or ENGLISH
)

data class AiResponse(
    val text: String,
    val modelUsed: String,
    val rawFinishReason: String? = null
)
