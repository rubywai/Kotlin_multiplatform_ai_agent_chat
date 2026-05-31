package com.rubylearner.kmpagent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteUiState(
    val notes: List<Note> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val editingId: Long? = null,
    val draftTitle: String = "",
    val draftContent: String = "",
)

class NoteViewModel(
    private val api: NoteApi = NoteApi(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _state = MutableStateFlow(NoteUiState())
    val state: StateFlow<NoteUiState> = _state.asStateFlow()

    fun setTitle(value: String) = _state.update { it.copy(draftTitle = value) }
    fun setContent(value: String) = _state.update { it.copy(draftContent = value) }

    fun startEdit(note: Note) = _state.update {
        it.copy(editingId = note.id, draftTitle = note.title, draftContent = note.content)
    }

    fun cancelEdit() = _state.update {
        it.copy(editingId = null, draftTitle = "", draftContent = "")
    }

    fun refresh() {
        launchSafe {
            _state.update { it.copy(loading = true, error = null) }
            val notes = api.list()
            _state.update { it.copy(notes = notes, loading = false) }
        }
    }

    fun submit() {
        val current = _state.value
        if (current.draftTitle.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return
        }
        val draft = NoteDraft(current.draftTitle.trim(), current.draftContent.trim())
        launchSafe {
            if (current.editingId == null) api.create(draft) else api.update(current.editingId, draft)
            cancelEdit()
            val notes = api.list()
            _state.update { it.copy(notes = notes) }
        }
    }

    fun delete(id: Long) {
        launchSafe {
            api.delete(id)
            val notes = api.list()
            _state.update { it.copy(notes = notes) }
        }
    }

    private fun launchSafe(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.message ?: "Unknown error") }
            }
        }
    }
}
