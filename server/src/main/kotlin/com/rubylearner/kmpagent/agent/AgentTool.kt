package com.rubylearner.kmpagent.agent

import kotlinx.serialization.json.JsonObject

/** A function the agent can call. Backed by a JSON-schema parameter spec. */
interface AgentTool {
    val name: String
    val description: String
    val parameters: JsonObject

    /** Execute with the model-supplied arguments; return a plain-text result for the LLM. */
    suspend fun execute(args: JsonObject): String

    fun toToolDef(): ToolDef = ToolDef(
        function = FunctionSpec(name = name, description = description, parameters = parameters),
    )
}
