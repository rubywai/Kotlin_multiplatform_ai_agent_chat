package com.rubylearner.kmpagent

import com.rubylearner.kmpagent.agent.ChatAgent
import com.rubylearner.kmpagent.agent.CryptoTool
import com.rubylearner.kmpagent.agent.DeepSeekClient
import com.rubylearner.kmpagent.agent.NotesTool
import com.rubylearner.kmpagent.agent.WeatherTool
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val repo = NoteRepository()

    val deepSeekKey = System.getenv("DEEPSEEK_API_KEY")
    val agent = deepSeekKey?.takeIf { it.isNotBlank() }?.let { key ->
        val deepSeek = DeepSeekClient(apiKey = key)
        // Reuse the DeepSeek client's HTTP client (it has JSON + timeout configured) for tool calls.
        ChatAgent(
            deepSeek = deepSeek,
            tools = listOf(
                WeatherTool(deepSeek.http),
                CryptoTool(deepSeek.http),
                NotesTool(repo),
            ),
        )
    }

    install(ContentNegotiation) { json() }
    install(CORS) {
        anyHost()
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }

    routing {
        get("/") { call.respondText(sayHello("Ktor")) }

        post("/chat") {
            val agentOrNull = agent
                ?: return@post call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    "DEEPSEEK_API_KEY is not set on the server.",
                )
            val request = call.receive<ChatRequest>()
            call.respond(agentOrNull.run(request.messages))
        }

        route("/notes") {
            get {
                call.respond(repo.all())
            }
            post {
                val draft = call.receive<NoteDraft>()
                call.respond(HttpStatusCode.Created, repo.create(draft))
            }
            get("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val note = repo.find(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(note)
            }
            put("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val draft = call.receive<NoteDraft>()
                val updated = repo.update(id, draft)
                    ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(updated)
            }
            delete("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                if (repo.delete(id)) call.respond(HttpStatusCode.NoContent)
                else call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
