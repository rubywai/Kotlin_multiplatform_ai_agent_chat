package com.rubylearner.kmpagent.agent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

// --- OpenAI-compatible DTOs (DeepSeek implements this shape) ---

@Serializable
data class DeepSeekMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall,
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String, // raw JSON string
)

@Serializable
data class ToolDef(
    val type: String = "function",
    val function: FunctionSpec,
)

@Serializable
data class FunctionSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val tools: List<ToolDef>? = null,
    val temperature: Double = 0.4,
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<Choice>,
)

@Serializable
private data class Choice(
    val message: DeepSeekMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)

/** Thin wrapper over DeepSeek's /chat/completions endpoint. */
class DeepSeekClient(
    private val apiKey: String,
    private val model: String = "deepseek-chat",
    private val baseUrl: String = "https://api.deepseek.com",
    val http: HttpClient = defaultClient(),
) {
    suspend fun complete(messages: List<DeepSeekMessage>, tools: List<ToolDef>): DeepSeekMessage {
        val response: ChatCompletionResponse = http.post("$baseUrl/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(ChatCompletionRequest(model = model, messages = messages, tools = tools))
        }.body()
        return response.choices.first().message
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    // Include default values like type="function" on tools/tool_calls,
                    // but omit null fields (content/tool_calls/tool_call_id) DeepSeek rejects.
                    encodeDefaults = true
                    explicitNulls = false
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
            }
        }
    }
}
