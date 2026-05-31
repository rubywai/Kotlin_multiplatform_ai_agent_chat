package com.rubylearner.kmpagent.agent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Current crypto price in USD, via the free CoinGecko API (no key needed). */
class CryptoTool(private val http: HttpClient) : AgentTool {
    override val name = "get_crypto_price"
    override val description =
        "Get the current USD price of a cryptocurrency by its CoinGecko id."
    override val parameters: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("coin") {
                put("type", "string")
                put(
                    "description",
                    "CoinGecko coin id, lowercase, e.g. 'bitcoin', 'ethereum', 'solana', 'dogecoin'",
                )
            }
        }
        putJsonArray("required") { add("coin") }
    }

    override suspend fun execute(args: JsonObject): String {
        val coin = args["coin"]?.jsonPrimitive?.content?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: return "Error: a 'coin' argument is required."

        // Response shape: { "bitcoin": { "usd": 65000.0 } }
        val prices: Map<String, Map<String, Double>> =
            http.get("https://api.coingecko.com/api/v3/simple/price") {
                url.parameters.append("ids", coin)
                url.parameters.append("vs_currencies", "usd")
            }.body()

        val usd = prices[coin]?.get("usd")
            ?: return "Could not find a price for '$coin'. Use a CoinGecko id like 'bitcoin'."
        return "$coin is currently \$$usd USD."
    }
}
