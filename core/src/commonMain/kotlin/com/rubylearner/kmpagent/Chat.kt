package com.rubylearner.kmpagent

import kotlinx.serialization.Serializable

/** A single turn in the conversation. role is "user" or "assistant". */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

/** Sent by clients to POST /chat — the full conversation so far. */
@Serializable
data class ChatRequest(
    val messages: List<ChatMessage>,
)

/** A tool the agent invoked while producing its answer (for display in the UI). */
@Serializable
data class ToolCallInfo(
    val name: String,
    val arguments: String,
    val result: String,
)

/** The agent's final answer plus any tools it used to get there. */
@Serializable
data class ChatResponse(
    val reply: String,
    val toolCalls: List<ToolCallInfo> = emptyList(),
)
