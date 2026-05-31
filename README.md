# MyApplication — KMP Notes + AI Agent

A Kotlin Multiplatform / Compose Multiplatform demo targeting **Android, iOS, Web, and a Ktor server**.

It has two features, sharing one codebase across all client platforms:

1. **Notes CRUD** — create / read / update / delete notes, backed by a Ktor + SQLite server.
2. **AI agent chat** — a chat assistant powered by the **DeepSeek API** with **tool calling**. The agent can:
   - `get_weather` — current weather for a city (via [Open-Meteo](https://open-meteo.com/), no key)
   - `get_crypto_price` — current USD price of a coin (via [CoinGecko](https://www.coingecko.com/), no key)
   - `get_current_notes` — read the user's notes from the same SQLite store

The full agent loop (model call → tool execution → loop until final answer) runs **server-side**, so the DeepSeek API key never reaches the clients.

## Architecture

```
core/                 Shared Kotlin: Note, Chat models (@Serializable), used by server + clients
server/               Ktor server: Notes CRUD + /chat agent, SQLite via Exposed
app/shared/           Compose Multiplatform UI + Ktor client (Notes + Chat screens)
app/androidApp/       Android entry point
app/iosApp/           iOS entry point (SwiftUI host)
app/webApp/           Web entry point (Kotlin/Wasm + Kotlin/JS)
```

### HTTP API

| Method | Path          | Description                          |
|--------|---------------|--------------------------------------|
| GET    | `/notes`      | List all notes                       |
| POST   | `/notes`      | Create a note (`{title, content}`)   |
| GET    | `/notes/{id}` | Get one note                         |
| PUT    | `/notes/{id}` | Update a note                        |
| DELETE | `/notes/{id}` | Delete a note                        |
| POST   | `/chat`       | Run the AI agent (`{messages:[...]}`)|

## Setup

### 1. DeepSeek API key (required for chat)

The server reads the key from the `DEEPSEEK_API_KEY` environment variable. It is **not** stored in the repo.

```bash
export DEEPSEEK_API_KEY=sk-your-key-here
```

See [`.env.example`](./.env.example). Without the key, the server still runs and the Notes CRUD works, but `/chat` returns `503`.

> The clients use platform-aware base URLs for local dev: Android emulator → `http://10.0.2.2:8080`, iOS simulator / Web → `http://localhost:8080`.

### 2. Run the server

```bash
DEEPSEEK_API_KEY=sk-your-key-here ./gradlew :server:run
```

Listens on `http://0.0.0.0:8080`. Creates `notes.db` (SQLite) and writes chat I/O to `logs/chat.log`.

### 3. Run a client

- **Android:** `./gradlew :app:androidApp:installDebug` (or run from Android Studio)
- **Web (Wasm):** `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun` → http://localhost:8081/
- **Web (JS):** `./gradlew :app:webApp:jsBrowserDevelopmentRun`
- **iOS:** open [`app/iosApp`](./app/iosApp) in Xcode and run on a simulator

## Running tests

- Android: `./gradlew :app:shared:testAndroidHostTest`
- Server: `./gradlew :server:test`
- Web (Wasm / JS): `./gradlew :app:shared:wasmJsTest` / `:app:shared:jsTest`
- iOS: `./gradlew :app:shared:iosSimulatorArm64Test`

## Notes on secrets & logging

- `DEEPSEEK_API_KEY` is supplied via the environment only — never commit it.
- `logs/`, `*.db`, and `.env` are gitignored.
- `logs/chat.log` records chat inputs, tool calls, and outputs in plaintext (handy for debugging; rotates daily, keeps 14 days).

---

Built with [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform), [Ktor](https://ktor.io/),
and [Kotlin/Wasm](https://kotl.in/wasm/).
