package com.rubylearner.kmpagent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json

class NoteApi(
    private val client: HttpClient = defaultClient(),
    private val root: String = "$baseUrl/notes",
) {
    suspend fun list(): List<Note> = client.get(root).body()

    suspend fun create(draft: NoteDraft): Note =
        client.post(root) {
            contentType(ContentType.Application.Json)
            setBody(draft)
        }.body()

    suspend fun update(id: Long, draft: NoteDraft): Note =
        client.put("$root/$id") {
            contentType(ContentType.Application.Json)
            setBody(draft)
        }.body()

    suspend fun delete(id: Long): Boolean =
        client.delete("$root/$id").status.isSuccess()

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) { json() }
        }
    }
}
