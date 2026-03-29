package io.github.gustavlindberg99.weather

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The number of times to retry fetching data from the server on network errors.
 */
private const val NUMBER_OF_RETRIES = 2

/**
 * Class that handles fetching data from the server.
 *
 * @param _context          The Android context.
 * @param _city             The city to fetch data for.
 * @param _successCallback  The callback to run when the data is successfully fetched.
 * @param _errorCallback    The callback to run when an error occurs
 */
class DataFetcher(
    private val _context: Context,
    private val _city: City,
    private val _successCallback: (WeatherData) -> Unit,
    private val _errorCallback: (String) -> Unit
) {
    private val _queue: RequestQueue = Volley.newRequestQueue(this._context)
    private val _responses = mutableListOf<String>()
    private var _errorCount: Int = 0

    private val _urls: List<String> = listOf(
        String.format(
            "https://api.open-meteo.com/v1/forecast" +
            "?latitude=%s&longitude=%s&timezone=%s&current_weather=true" +
            "&hourly=temperature_2m,weathercode,relativehumidity_2m,apparent_temperature,precipitation,pressure_msl,uv_index,cloudcover_low,cloudcover_mid,dewpoint_2m,winddirection_10m,windspeed_10m,precipitation_probability,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset",
            URLEncoder.encode(this._city.latitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(this._city.longitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(this._city.timezone, StandardCharsets.UTF_8.name())
        ), String.format(
            "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=%s&longitude=%s&timezone=%s&current_weather=false" +
            "&hourly=european_aqi,us_aqi",
            URLEncoder.encode(this._city.latitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(this._city.longitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(this._city.timezone, StandardCharsets.UTF_8.name())
        ), String.format(
            "https://marine-api.open-meteo.com/v1/marine" +
            "?latitude=%s&longitude=%s&timezone=%s&hourly=sea_surface_temperature",
            URLEncoder.encode(this._city.latitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(this._city.longitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(this._city.timezone, StandardCharsets.UTF_8.name())
        )
    )

    /**
     * Handles any error that occurs when fetching the data, both network errors and parsing errors.
     *
     * The error handler is also run on 4xx errors (this was unclear from the Volley documentation so I tested it)
     *
     * @param e The error that occurred.
     */
    private fun handleError(e: Exception) {
        this._queue.cancelAll { true }
        this._errorCount++
        if (this._errorCount <= NUMBER_OF_RETRIES) {
            this.fetch()
        }
        else {
            val errorMessage =
                if (e is VolleyError) this._context.getString(R.string.noInternetConnection)
                else String.format(
                    this._context.getString(R.string.serverError),
                    e.message ?: this._context.getString(R.string.unknownError)
                )
            this._errorCallback(errorMessage)
        }
    }

    /**
     * Handles the response from the server.
     *
     * @param lastResponse The response from the server.
     */
    private fun handleResponse(lastResponse: String) {
        this._responses.add(lastResponse)
        if (this._responses.size == this._urls.size) {
            try {
                //Merge different responses into one JSON object
                val data = JSONObject()
                for (response in this._responses) {
                    data.mergeWith(JSONObject(response))
                }

                //Fix the current time so that it shows the actual current time rather than the last whole hour
                val currentWeather = data.getJSONObject("current_weather")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                dateFormat.timeZone = TimeZone.getTimeZone(this._city.timezone)
                currentWeather.put("time", dateFormat.format(Date()).replace(" ", "T"))

                //Save the JSON data
                val weatherData = WeatherData(data.toString(), this._city.timezone)
                this._successCallback(weatherData)
            }
            catch (e: JSONException) {
                handleError(e)
            }
            catch (e: ParseException) {
                handleError(e)
            }
        }
    }

    /**
     * Fetches the data from the server.
     */
    public fun fetch() {
        for (url in this._urls) {
            this._queue.add(
                StringRequest(
                    Request.Method.GET,
                    url,
                    this::handleResponse,
                    this::handleError
                )
            )
        }
    }
}