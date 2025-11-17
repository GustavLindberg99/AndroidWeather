package io.github.gustavlindberg99.weather

import android.content.SharedPreferences
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.util.*
import kotlin.math.roundToInt

class WeatherData(private val _json: String, timezone: String) {
    val now: Calendar

    //Current variables
    val currentTemperature: Double
    val currentApparentTemperature: Double
    val currentWeatherCode: Int
    val currentWindSpeed: Double
    val currentWindDirection: Double
    val currentHumidity: Int
    val currentPrecipitation: Double
    val currentPressure: Double
    val currentUvIndex: Int
    val currentCloudCover: Int
    val currentDewPoint: Double
    val currentPrecipitationProbability: Int
    val currentAmericanAqi: Int
    val currentEuropeanAqi: Int
    val currentIsDay: Boolean
    val currentSeaTemperature: Double    //NaN in locations that aren't by the sea

    //Hourly variables (length: 168)
    val hourlyWeatherCode: List<Int>
    val hourlyTemperature: List<Double>
    val hourlyWindSpeed: List<Double>
    val hourlyWindDirection: List<Double>
    val hourlyUvIndex: List<Int>
    val hourlyPrecipitationProbability: List<Int>
    private val hourlyIsDay: List<Boolean>

    //Daily variables (length: 7)
    val sunrises: List<Calendar?>
    val sunsets: List<Calendar?>
    val maxTemperature: List<Double>
    val minTemperature: List<Double>
    val dailyWeatherCode: List<Int>

    init {
        val data = JSONObject(this._json)
        val currentWeather = data.getJSONObject("current_weather")
        val hourly = data.getJSONObject("hourly")
        val daily = data.getJSONObject("daily")
        this.now = parseDate(currentWeather.getString("time"), timezone) ?: Calendar.getInstance()
        this.now.timeZone = TimeZone.getTimeZone(timezone)
        val currentHour: Int = now[Calendar.HOUR_OF_DAY]

        //Hourly variables
        val hourlyWeatherCode = mutableListOf<Int>()
        val hourlyTemperature = mutableListOf<Double>()
        val hourlyWindSpeed = mutableListOf<Double>()
        val hourlyWindDirection = mutableListOf<Double>()
        val hourlyUvIndex = mutableListOf<Int>()
        val hourlyPrecipitationProbability = mutableListOf<Int>()
        val hourlyIsDay = mutableListOf<Boolean>()
        for (i in 0..167) {
            hourlyTemperature.add(nullSafe(hourly.getJSONArray("temperature_2m"), i))
            val hourlyCloudCover = totalCloudCover(
                nullSafe<Int>(
                    hourly.getJSONArray("cloudcover_low"),
                    i
                ).coerceIn(0..100),
                nullSafe<Int>(hourly.getJSONArray("cloudcover_mid"), i).coerceIn(0..100)
            )
            val hourlyPrecipitation: Double =
                nullSafe<Double>(hourly.getJSONArray("precipitation"), i).coerceAtLeast(0.0)
            hourlyWeatherCode.add(
                weatherCodeFromData(
                    nullSafe(
                        hourly.getJSONArray("weathercode"),
                        i
                    ), hourlyCloudCover, hourlyPrecipitation
                )
            )
            hourlyWindSpeed.add(
                nullSafe<Double>(
                    hourly.getJSONArray("windspeed_10m"),
                    i
                ).coerceAtLeast(0.0)
            )
            if (hourlyWindSpeed[i] != 0.0) {    //If this is zero the wind direction will be null which would cause an error if the code below is executed
                hourlyWindDirection.add(nullSafe(hourly.getJSONArray("winddirection_10m"), i))
            }
            else {
                hourlyWindDirection.add(0.0)    //If there is no wind, add zero to preserve the structure of the array (it doesn't matter if it's zero or anything else because it won't be displayed anyway)
            }
            hourlyUvIndex.add(
                nullSafe<Double>(hourly.getJSONArray("uv_index"), i).roundToInt().coerceAtLeast(0)
            )
            hourlyPrecipitationProbability.add(
                nullSafe<Int>(
                    hourly.getJSONArray("precipitation_probability"),
                    i
                ).coerceIn(0..100)
            )
            hourlyIsDay.add(nullSafe<Int>(hourly.getJSONArray("is_day"), i) != 0)
        }
        this.hourlyWeatherCode = hourlyWeatherCode
        this.hourlyTemperature = hourlyTemperature
        this.hourlyWindSpeed = hourlyWindSpeed
        this.hourlyWindDirection = hourlyWindDirection
        this.hourlyUvIndex = hourlyUvIndex
        this.hourlyPrecipitationProbability = hourlyPrecipitationProbability
        this.hourlyIsDay = hourlyIsDay

        //Daily variables
        val sunrises = mutableListOf<Calendar?>()
        val sunsets = mutableListOf<Calendar?>()
        val maxTemperature = mutableListOf<Double>()
        val minTemperature = mutableListOf<Double>()
        val dailyWeatherCode = mutableListOf<Int>()
        for (i in 0..6) {
            sunrises.add(parseDate(daily.getJSONArray("sunrise").getString(i), timezone))
            sunsets.add(parseDate(daily.getJSONArray("sunset").getString(i), timezone))
            maxTemperature.add(daily.getJSONArray("temperature_2m_max").getDouble(i))
            minTemperature.add(daily.getJSONArray("temperature_2m_min").getDouble(i))

            //Use my own algorithm for determining the daily weather code because the one provided by the API gives to high priority to how the weather will be during the night
            dailyWeatherCode.add(
                if (i > 0 || currentHour < 10) {
                    combinedWeatherCode(hourlyWeatherCode.subList(24 * i + 10, 24 * i + 20))
                }
                else if (currentHour < 20) {
                    combinedWeatherCode(hourlyWeatherCode.subList(currentHour, 20))
                }
                else {
                    combinedWeatherCode(hourlyWeatherCode.subList(currentHour, 24))
                }
            )
        }
        this.sunrises = sunrises
        this.sunsets = sunsets
        this.maxTemperature = maxTemperature
        this.minTemperature = minTemperature
        this.dailyWeatherCode = dailyWeatherCode

        //Current variables
        this.currentTemperature = currentWeather.getDouble("temperature")
        this.currentCloudCover = totalCloudCover(
            hourly.getJSONArray("cloudcover_low").getInt(currentHour).coerceIn(0..100),
            hourly.getJSONArray("cloudcover_mid").getInt(currentHour).coerceIn(0..100)
        )
        this.currentPrecipitation =
            hourly.getJSONArray("precipitation").getDouble(currentHour).coerceAtLeast(0.0)
        this.currentWeatherCode = weatherCodeFromData(
            currentWeather.getInt("weathercode"),
            this.currentCloudCover,
            this.currentPrecipitation
        )
        this.currentWindSpeed = currentWeather.getDouble("windspeed").coerceAtLeast(0.0)
        this.currentWindDirection = currentWeather.getDouble("winddirection")
        this.currentHumidity =
            hourly.getJSONArray("relativehumidity_2m").getInt(currentHour).coerceIn(0..100)
        this.currentApparentTemperature =
            hourly.getJSONArray("apparent_temperature").getInt(currentHour).toDouble()
        this.currentPressure =
            hourly.getJSONArray("pressure_msl").getDouble(currentHour).coerceAtLeast(0.0)
        this.currentUvIndex =
            hourly.getJSONArray("uv_index").getDouble(currentHour).roundToInt().coerceAtLeast(0)
        this.currentDewPoint = hourly.getJSONArray("dewpoint_2m").getDouble(currentHour)
        this.currentPrecipitationProbability =
            hourly.getJSONArray("precipitation_probability").getInt(currentHour).coerceIn(0..100)
        this.currentAmericanAqi = hourly.getJSONArray("us_aqi").getInt(currentHour).coerceAtLeast(0)
        this.currentEuropeanAqi =
            hourly.getJSONArray("european_aqi").getInt(currentHour).coerceAtLeast(0)
        this.currentSeaTemperature =
            hourly.getJSONArray("sea_surface_temperature").optDouble(currentHour)
        val sunrise: Calendar? = this.sunrises[0]
        val sunriseHour: Int? = sunrise?.get(Calendar.HOUR_OF_DAY)
        val sunset: Calendar? = this.sunsets[0]
        val sunsetHour: Int? = sunset?.get(Calendar.HOUR_OF_DAY)
        this.currentIsDay = if (currentHour == sunriseHour && currentHour == sunsetHour) {
            if (sunrise < sunset) sunrise < now && sunset > now
            else now in sunset..sunrise
        }
        else if (currentHour == sunriseHour) {
            sunrise < now
        }
        else if (currentHour == sunsetHour) {
            sunset > now
        }
        else {
            hourly.getJSONArray("is_day").getInt(currentHour) != 0
        }
    }

    fun currentDayOrNight(): String =
        if (this.currentWeatherCode > 2 && this.currentWeatherCode / 10 != 8) {
            ""
        }
        else if (this.currentIsDay) {
            "_day"
        }
        else {
            "_night"
        }

    fun hourlyDayOrNight(hour: Int): String =
        if (this.hourlyWeatherCode[hour] > 2 && this.hourlyWeatherCode[hour] / 10 != 8) {
            ""
        }
        else if (this.hourlyIsDay[hour]) {
            "_day"
        }
        else {
            "_night"
        }

    fun dailyDayOrNight(day: Int, startHour: Int = 0): String =
        if (this.dailyWeatherCode[day] > 2 && this.dailyWeatherCode[day] / 10 != 8) {
            ""
        }
        else if (this.hourlyIsDay.subList(day + startHour, day + 24).any { it }) {
            "_day"
        }
        else {
            "_night"
        }

    fun putToBundle(bundle: Bundle, key: String?) {
        bundle.putString(key, _json)
    }

    fun putToPreferences(preferencesEditor: SharedPreferences.Editor, key: String?) {
        preferencesEditor.putString(key, _json)
    }
}


//This function is needed because a bug in the API (https://github.com/open-meteo/open-meteo/issues/71). If the value is null when it's not supposed to be, the function returns the non-null value that's closest in time.
@Throws(JSONException::class)
private inline fun <reified T : Number> nullSafe(array: JSONArray, index: Int): T {
    val getValueAtIndex = { i: Int ->
        when (T::class) {
            Byte::class -> array.getInt(i).toByte()
            Double::class -> array.getDouble(i)
            Float::class -> array.getDouble(i).toFloat()
            Int::class -> array.getInt(i)
            Long::class -> array.getLong(i)
            Short::class -> array.getInt(i).toShort()
            else -> throw UnsupportedOperationException()
        } as T
    }
    if (array.isNull(index)) {
        var low = index - 1
        var high = index + 1
        while (low >= 0 || high < array.length()) {
            if (low >= 0 && !array.isNull(low)) {
                return getValueAtIndex(low)
            }
            if (high < array.length() && !array.isNull(high)) {
                return getValueAtIndex(high)
            }
            low--
            high++
        }
    }
    return getValueAtIndex(index)
}

private fun combinedWeatherCode(weatherCodes: List<Int>): Int {
    val isFog: (Int) -> Boolean = { it / 10 == 4 }
    val isDrizzle: (Int) -> Boolean = { it == 51 || it == 53 || it == 55 }
    val isFreezingDrizzle: (Int) -> Boolean = { it == 56 || it == 57 }
    val isThunderstorm: (Int) -> Boolean = { it / 10 == 9 }
    val isRain: (Int) -> Boolean = { listOf(61, 63, 65, 80, 81, 82).contains(it) }
    val isFreezingRain: (Int) -> Boolean = { it == 66 || it == 67 }
    val isSnow: (Int) -> Boolean = { it / 10 == 7 || it == 85 || it == 86 }
    val isSun: (Int) -> Boolean = { it <= 2 || it / 10 == 8 }

    //Thunderstorms
    val thunderstorm: Int? = weatherCodes.sorted().find(isThunderstorm)
    if (thunderstorm != null) {
        return thunderstorm
    }

    //Rain and snow
    val rainCount: Int = weatherCodes.count(isRain)
    val snowCount: Int = weatherCodes.count(isSnow)
    if (rainCount > 0 || snowCount > 0) {
        val hasSun: Boolean = weatherCodes.any(isSun)
        //Rain
        if (rainCount >= snowCount) {
            val intensities: List<Int> = weatherCodes.filter(isRain).map {
                when (it) {
                    61, 80 -> 1
                    63, 81 -> 2
                    65, 82 -> 3
                    else -> 0
                }
            }
            return when (intensities.average().roundToInt()) {
                1 -> if (hasSun) 80 else 61
                2 -> if (hasSun) 81 else 63
                3 -> if (hasSun) 82 else 65
                else -> 63
            }
        }
        //Snow
        else {
            val existingSnowCodes: List<Int> = weatherCodes.filter(isSnow)
            if (existingSnowCodes.all { it == 77 }) return 77    //Only return snow grains (code 77) if the only snow that day is snow grains
            val intensities: List<Int> = existingSnowCodes.map {
                when (it) {
                    71 -> 1
                    73, 77, 85 -> 2
                    75, 86 -> 3
                    else -> 0
                }
            }
            return when (intensities.average().roundToInt()) {
                1 -> if (hasSun) 85 else 71
                2 -> if (hasSun) 85 else 73
                3 -> if (hasSun) 86 else 75
                else -> 73
            }
        }
    }

    //Freezing rain
    if (weatherCodes.any(isFreezingRain)) {
        return if (weatherCodes.count { it == 65 } >= weatherCodes.count { it == 66 }) 65 else 66
    }

    //Drizzle
    if (weatherCodes.any(isDrizzle)) {
        return ((weatherCodes.filter(isDrizzle).average() - 1.0) / 2.0).roundToInt() * 2 + 1
    }

    //Freezing drizzle
    if (weatherCodes.any(isFreezingDrizzle)) {
        return if (weatherCodes.count { it == 56 } >= weatherCodes.count { it == 57 }) 56 else 57
    }

    //Fog
    if (weatherCodes.any(isFog)) {
        return if (weatherCodes.count { it == 45 } >= weatherCodes.count { it == 48 }) 45 else 48
    }

    //Sun and clouds (at this point the only remaining values should be 0, 1, 2 and 3 if all the input values are valid WMO codes)
    return weatherCodes.average().roundToInt()
}

private fun totalCloudCover(cloudCoverLow: Int, cloudCoverMid: Int): Int {
    val sunCoverLow = 100 - cloudCoverLow
    val sunCoverMid = 100 - cloudCoverMid
    val totalSunCover = sunCoverLow * sunCoverMid / 100
    return 100 - totalSunCover
}

/**
 * Since the weather code from the API isn't always accurate, correct the weather code so that:
 *  - Cloudy or not is based on the cloud cover
 *  - Light or heavy rain is based on the amount of precipitation
 *  - The icon with a sun and rain for showers is only shown if the cloud cover isn't too high
 *  - Drizzle is always converted to rain because the API often returns drizzle incorrectly, and there's no reliable way of knowing if it's actually drizzle
 *
 * @param weatherCode   The weather code provided by the API, which isn't always accurate.
 * @param cloudCover    The cloud cover.
 * @param precipitation The precipitation.
 *
 * @return A weather code which is more accurate.
 */
private fun weatherCodeFromData(weatherCode: Int, cloudCover: Int, precipitation: Double): Int {
    if (listOf(45, 48, 56, 57, 66, 67, 77, 95, 96, 99).contains(weatherCode)) {
        return weatherCode    //For fog, thunderstorms, freezing rain/drizzle and snow grains, just keep the weather code given by the API
    }
    val intensity: Int = when (precipitation) {
        0.0 -> 0    //None
        in 0.0..0.3 -> 1    //Light
        in 0.3..1.0 -> 2    //Medium
        else -> 3    //Heavy
    }
    val isDrizzle: Boolean = weatherCode / 10 == 5
    val isRain: Boolean = weatherCode / 10 == 6 || listOf(80, 81, 82).contains(weatherCode)
    val isSnow: Boolean = weatherCode / 10 == 7 || listOf(85, 86).contains(weatherCode)
    val isSunny: Boolean = cloudCover < 75 && (weatherCode < 10 || weatherCode / 10 == 8)
    return if (isSunny) when (intensity) {
        1 -> if (isSnow) 85 else 80
        2 -> if (isSnow) 85 else 81
        3 -> if (isSnow) 86 else 82
        else -> when (cloudCover) {
            in 0..25 -> 0
            in 26..50 -> 1
            in 51..75 -> 2
            else -> 3
        }
    }
    else if (isSnow) when (intensity) {
        2 -> 73
        3 -> 75
        else -> 71
    }
    else if (isDrizzle || isRain) when (intensity) {
        1 -> 61
        2 -> 63
        3 -> 65
        else -> 3
    }
    else 3
}

fun SharedPreferences.getWeatherData(key: String, timezone: String): WeatherData? {
    val json: String = this.getString(key, null) ?: return null
    return try {
        WeatherData(json, timezone)
    }
    catch (_: JSONException) {
        null
    }
    catch (_: ParseException) {
        null
    }
}

fun SharedPreferences.Editor.putWeatherData(key: String, weatherData: WeatherData?) {
    weatherData?.putToPreferences(this, key)
}

fun Bundle.getWeatherData(key: String, timezone: String): WeatherData? {
    val json: String = this.getString(key) ?: return null
    return try {
        WeatherData(json, timezone)
    }
    catch (_: JSONException) {
        null
    }
    catch (_: ParseException) {
        null
    }
}

fun Bundle.putWeatherData(key: String, weatherData: WeatherData?) {
    weatherData?.putToBundle(this, key)
}