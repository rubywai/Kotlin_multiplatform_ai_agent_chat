package com.rubylearner.kmpagent.agent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Current weather for a city, via the free Open-Meteo API (no key needed). */
class WeatherTool(private val http: HttpClient) : AgentTool {
    override val name = "get_weather"
    override val description = "Get the current weather (temperature and wind) for a given city."
    override val parameters: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("city") {
                put("type", "string")
                put("description", "City name, e.g. 'London' or 'Yangon'")
            }
        }
        putJsonArray("required") { add("city") }
    }

    override suspend fun execute(args: JsonObject): String {
        val city = args["city"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: return "Error: a 'city' argument is required."

        val geo: GeoResponse = http.get("https://geocoding-api.open-meteo.com/v1/search") {
            url.parameters.append("name", city)
            url.parameters.append("count", "1")
        }.body()
        val place = geo.results?.firstOrNull()
            ?: return "Could not find a location named '$city'."

        val forecast: ForecastResponse = http.get("https://api.open-meteo.com/v1/forecast") {
            url.parameters.append("latitude", place.latitude.toString())
            url.parameters.append("longitude", place.longitude.toString())
            url.parameters.append("current", "temperature_2m,wind_speed_10m,weather_code")
        }.body()
        val c = forecast.current

        val where = listOfNotNull(place.name, place.country).joinToString(", ")
        return "Weather in $where: ${c.temperature}°C, wind ${c.windSpeed} km/h, " +
            "conditions: ${describe(c.weatherCode)}."
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "clear sky"
        1, 2, 3 -> "partly cloudy"
        45, 48 -> "foggy"
        in 51..67 -> "rainy"
        in 71..77 -> "snowy"
        in 80..82 -> "rain showers"
        in 95..99 -> "thunderstorm"
        else -> "unknown"
    }

    @Serializable
    private data class GeoResponse(val results: List<GeoResult>? = null)

    @Serializable
    private data class GeoResult(
        val latitude: Double,
        val longitude: Double,
        val name: String,
        val country: String? = null,
    )

    @Serializable
    private data class ForecastResponse(val current: CurrentWeather)

    @Serializable
    private data class CurrentWeather(
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("wind_speed_10m") val windSpeed: Double,
        @SerialName("weather_code") val weatherCode: Int,
    )
}
