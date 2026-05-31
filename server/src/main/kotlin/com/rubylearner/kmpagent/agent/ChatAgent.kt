package com.rubylearner.kmpagent.agent

import com.rubylearner.kmpagent.ChatMessage
import com.rubylearner.kmpagent.ChatResponse
import com.rubylearner.kmpagent.ToolCallInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory

private const val SYSTEM_PROMPT = """
You are a helpful assistant inside a notes app. You can answer questions and use tools.
- Use get_weather for weather questions.
- Use get_crypto_price for cryptocurrency prices.
- Use get_current_notes when the user asks about their notes or to summarize them.
Call tools only when needed, then answer the user clearly and concisely in plain text.
"""

private const val MAX_STEPS = 5

/** Runs the tool-calling loop against DeepSeek until a final text answer is produced. */
class ChatAgent(
    private val deepSeek: DeepSeekClient,
    tools: List<AgentTool>,
) {
    private val toolsByName = tools.associateBy { it.name }
    private val toolDefs = tools.map { it.toToolDef() }
    private val json = Json { ignoreUnknownKeys = true }
    private val log = LoggerFactory.getLogger("ChatLog")

    suspend fun run(history: List<ChatMessage>): ChatResponse {
        val userInput = history.lastOrNull { it.role == "user" }?.content.orEmpty()
        log.info("INPUT  | {}", oneLine(userInput))

        val messages = mutableListOf<DeepSeekMessage>()
        messages += DeepSeekMessage(role = "system", content = SYSTEM_PROMPT.trim())
        history.forEach { messages += DeepSeekMessage(role = it.role, content = it.content) }

        val used = mutableListOf<ToolCallInfo>()

        repeat(MAX_STEPS) {
            val assistant = deepSeek.complete(messages, toolDefs)
            val calls = assistant.toolCalls

            if (calls.isNullOrEmpty()) {
                val reply = assistant.content.orEmpty().trim()
                log.info("OUTPUT | {}", oneLine(reply))
                return ChatResponse(reply = reply, toolCalls = used)
            }

            // Echo the assistant's tool_calls back, then append each tool result.
            messages += assistant
            for (call in calls) {
                val result = invoke(call.function.name, call.function.arguments)
                log.info("TOOL   | {}({}) -> {}", call.function.name, call.function.arguments, oneLine(result))
                used += ToolCallInfo(call.function.name, call.function.arguments, result)
                messages += DeepSeekMessage(
                    role = "tool",
                    toolCallId = call.id,
                    content = result,
                )
            }
        }

        log.warn("OUTPUT | (gave up after {} steps) input was: {}", MAX_STEPS, oneLine(userInput))
        return ChatResponse(
            reply = "I wasn't able to finish that request after several steps.",
            toolCalls = used,
        )
    }

    /** Collapse newlines so each log entry stays on a single line. */
    private fun oneLine(text: String): String = text.replace("\n", " ")

    private suspend fun invoke(name: String, rawArgs: String): String {
        val tool = toolsByName[name] ?: return "Error: unknown tool '$name'."
        return try {
            val args = if (rawArgs.isBlank()) JsonObject(emptyMap())
            else json.decodeFromString(JsonObject.serializer(), rawArgs)
            tool.execute(args)
        } catch (t: Throwable) {
            "Error running '$name': ${t.message}"
        }
    }
}
