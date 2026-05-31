package com.rubylearner.kmpagent

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: Long,
    val title: String,
    val content: String,
)

@Serializable
data class NoteDraft(
    val title: String,
    val content: String,
)
