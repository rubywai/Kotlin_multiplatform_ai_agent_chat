package com.rubylearner.kmpagent.agent

import com.rubylearner.kmpagent.NoteRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** Reads the user's notes from the same SQLite store the CRUD API uses. */
class NotesTool(private val repo: NoteRepository) : AgentTool {
    override val name = "get_current_notes"
    override val description =
        "Get the list of the user's currently saved notes (title and content)."
    override val parameters: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {}
    }

    override suspend fun execute(args: JsonObject): String {
        val notes = repo.all()
        if (notes.isEmpty()) return "The user has no notes saved."
        return notes.joinToString("\n") { "- #${it.id} ${it.title}: ${it.content}" }
    }
}
