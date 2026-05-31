package com.rubylearner.kmpagent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ChatApi(
    private val client: HttpClient = defaultClient(),
    private val endpoint: String = "$baseUrl/chat",
) {
    suspend fun send(messages: List<ChatMessage>): ChatResponse =
        client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(messages))
        }.body()

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                // The agent may make several round-trips to DeepSeek + tool APIs.
                requestTimeoutMillis = 90_000
            }
        }
    }
}
