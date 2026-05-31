package com.rubylearner.kmpagent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A message as shown in the chat transcript, including tools the agent used. */
data class ChatTurn(
    val role: String,
    val content: String,
    val toolCalls: List<ToolCallInfo> = emptyList(),
)

data class ChatUiState(
    val turns: List<ChatTurn> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val api: ChatApi = ChatApi(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun setInput(value: String) = _state.update { it.copy(input = value) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.sending) return

        val userTurn = ChatTurn(role = "user", content = text)
        _state.update {
            it.copy(turns = it.turns + userTurn, input = "", sending = true, error = null)
        }

        scope.launch {
            try {
                val history = _state.value.turns.map { ChatMessage(it.role, it.content) }
                val response = api.send(history)
                _state.update {
                    it.copy(
                        turns = it.turns + ChatTurn(
                            role = "assistant",
                            content = response.reply,
                            toolCalls = response.toolCalls,
                        ),
                        sending = false,
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(sending = false, error = t.message ?: "Request failed") }
            }
        }
    }
}
