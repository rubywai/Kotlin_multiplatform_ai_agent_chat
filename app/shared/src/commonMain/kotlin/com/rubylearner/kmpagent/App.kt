package com.rubylearner.kmpagent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

private enum class Screen(val label: String) {
    Notes("Notes"),
    Chat("AI Chat"),
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var current by remember { mutableStateOf(Screen.Notes) }

        Column(Modifier.fillMaxSize().safeContentPadding()) {
            TabRow(selectedTabIndex = current.ordinal) {
                Screen.entries.forEach { screen ->
                    Tab(
                        selected = current == screen,
                        onClick = { current = screen },
                        text = { Text(screen.label) },
                    )
                }
            }
            when (current) {
                Screen.Notes -> NotesScreen(Modifier.fillMaxSize())
                Screen.Chat -> ChatScreen(Modifier.fillMaxSize())
            }
        }
    }
}
