package io.github.gustavlindberg99.weather

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class City(private val _context: Context, val name: String, val latitude: Double, val longitude: Double, val timezone: String, var weatherData: WeatherData? = null){
    private var _id: Int? = null    //null if the location isn't saved, -1 if it's the current location

    private object Attribute{
        const val NAME = "name"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val TIMEZONE = "timezone"
        const val CACHED_WEATHER = "cachedWeather"
        const val ID = "id"
    }

    private object Preference{
        const val COUNT = "count"
        const val CURRENT_NAME = "currentName"
        const val CURRENT_LATITUDE = "currentLatitude"
        const val CURRENT_LONGITUDE = "currentLongitude"
        const val CURRENT_TIMEZONE = "currentTimezone"
        const val CACHED_WEATHER_AT_CURRENT_LOCATION = "cachedWeather"

        fun NAME(id: Int): String = if(id == -1) CURRENT_NAME else "name$id"
        fun LATITUDE(id: Int): String = if(id == -1) CURRENT_LATITUDE else "latitude$id"
        fun LONGITUDE(id: Int): String = if(id == -1) CURRENT_LONGITUDE else "longitude$id"
        fun TIMEZONE(id: Int): String = if(id == -1) CURRENT_TIMEZONE else "timezone$id"
        fun CACHED_WEATHER(id: Int): String = if(id == -1) CACHED_WEATHER_AT_CURRENT_LOCATION else "cachedWeather$id"
    }

    override fun equals(other: Any?): Boolean {
        if(other !is City){
            return false
        }
        val thisId: Int? = this._id
        val otherId: Int? = other._id
        if(thisId == null || otherId == null){
            return this.latitude == other.latitude && this.longitude == other.longitude
        }
        return thisId.toInt() == otherId.toInt()
    }

    override fun hashCode(): Int {    //This needs to be defined when equals is defined, see https://stackoverflow.com/a/2265637/4284627. This function was generated automatically by Android Studio by clicking on the warning and choosing "generate hashCode".
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + (this._id ?: 0)
        return result
    }

    fun fromUpdatedList(): City {
        val id: Int = this._id ?: return this
        if(id == -1){
            return City.currentLocation(this._context) ?: this
        }
        val cityList: List<City> = City.listFromPreferences(this._context)
        if(id < cityList.size){
            return cityList[id]
        }
        return this
    }

    val isCurrentLocation: Boolean
        get() = this._id != null && this._id == -1

    fun saveAsCurrentLocation(){
        val preferenceEditor: SharedPreferences.Editor = this._context.getSharedPreferences("cities", Context.MODE_PRIVATE).edit()
        preferenceEditor.putString(Preference.CURRENT_NAME, this.name)
        preferenceEditor.putFloat(Preference.CURRENT_LATITUDE, this.latitude.toFloat())
        preferenceEditor.putFloat(Preference.CURRENT_LONGITUDE, this.longitude.toFloat())
        preferenceEditor.putString(Preference.CURRENT_TIMEZONE, this.timezone)
        preferenceEditor.putWeatherData(Preference.CACHED_WEATHER_AT_CURRENT_LOCATION, this.weatherData)
        preferenceEditor.apply()
        this._id = -1
    }

    fun addAsNewLocation(){
        val preferences: SharedPreferences = this._context.getSharedPreferences("cities", Context.MODE_PRIVATE)
        val id: Int = preferences.getInt(Preference.COUNT, 0)
        this._id = id
        val preferenceEditor: SharedPreferences.Editor = preferences.edit()
        preferenceEditor.putString(Preference.NAME(id), this.name)
        preferenceEditor.putFloat(Preference.LATITUDE(id), this.latitude.toFloat())
        preferenceEditor.putFloat(Preference.LONGITUDE(id), this.longitude.toFloat())
        preferenceEditor.putString(Preference.TIMEZONE(id), this.timezone)
        preferenceEditor.putWeatherData(Preference.CACHED_WEATHER(id), this.weatherData)
        preferenceEditor.putInt(Preference.COUNT, id + 1)
        preferenceEditor.apply()
    }

    fun removeLocation(){
        val id: Int? = this._id
        if(id == null || id == -1){
            return
        }
        val preferences: SharedPreferences = this._context.getSharedPreferences("cities", Context.MODE_PRIVATE)
        val preferenceEditor: SharedPreferences.Editor = preferences.edit()
        val cityList: List<City> = City.listFromPreferences(this._context)

        //Decrement locations following the one we're deleting (we don't need to explicitly delete this location, it will be overwritten by the next one)
        for(i in id + 1 until cityList.size) {
            val city: City = cityList[i]
            preferenceEditor.putString(Preference.NAME(i - 1), city.name)
            preferenceEditor.putFloat(Preference.LATITUDE(i - 1), city.latitude.toFloat())
            preferenceEditor.putFloat(Preference.LONGITUDE(i - 1), city.longitude.toFloat())
            preferenceEditor.putString(Preference.TIMEZONE(i - 1), city.timezone)
            preferenceEditor.putWeatherData(Preference.CACHED_WEATHER(i - 1), city.weatherData)
        }

        //Delete the last city which is now a duplicate of the second last city
        preferenceEditor.remove(Preference.NAME(cityList.size - 1))
        preferenceEditor.remove(Preference.LATITUDE(cityList.size - 1))
        preferenceEditor.remove(Preference.LONGITUDE(cityList.size - 1))
        preferenceEditor.remove(Preference.TIMEZONE(cityList.size - 1))
        preferenceEditor.remove(Preference.CACHED_WEATHER(cityList.size - 1))

        //Update the count
        preferenceEditor.putInt(Preference.COUNT, cityList.size - 1)

        //Apply the changes
        preferenceEditor.apply()
        this._id = null
    }

    fun updateCachedWeather(){
        val id: Int = this._id ?: return
        val preferenceEditor: SharedPreferences.Editor = this._context.getSharedPreferences("cities", Context.MODE_PRIVATE).edit()
        preferenceEditor.putWeatherData(Preference.CACHED_WEATHER(id), this.weatherData)
        preferenceEditor.apply()
    }

    fun updateWeatherFromServer(successCallback: () -> Unit, errorCallback: () -> Unit){
        val urls = listOf(String.format(
            "https://api.open-meteo.com/v1/forecast"+
            "?latitude=%s&longitude=%s&timezone=%s&current_weather=true"+
            "&hourly=temperature_2m,weathercode,relativehumidity_2m,apparent_temperature,precipitation,pressure_msl,uv_index,cloudcover_low,cloudcover_mid,dewpoint_2m,winddirection_10m,windspeed_10m,precipitation_probability,is_day"+
            "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset",
            URLEncoder.encode(latitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(longitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(timezone, StandardCharsets.UTF_8.name())
        ), String.format(
            "https://air-quality-api.open-meteo.com/v1/air-quality"+
            "?latitude=%s&longitude=%s&timezone=%s&current_weather=true"+
            "&hourly=european_aqi,us_aqi",
            URLEncoder.encode(latitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(longitude.toString(), StandardCharsets.UTF_8.name()),
            URLEncoder.encode(timezone, StandardCharsets.UTF_8.name())
        ))

        val queue: RequestQueue = Volley.newRequestQueue(this._context)
        val responses = mutableListOf<String>()

        val handleError: (Exception) -> Unit = {
            //The error handler is also run on 4xx errors (this was unclear from the documentation so I tested it)
            queue.cancelAll{true}
            errorCallback()
        }

        val handleResponse = {lastResponse: String ->
            responses.add(lastResponse)
            if(responses.size == urls.size){
                try{
                    //Merge different responses into one JSON object
                    val data = JSONObject()
                    for(response in responses){
                        data.mergeWith(JSONObject(response))
                    }

                    //Fix the current time so that it shows the actual current time rather than the last whole hour
                    val currentWeather = data.getJSONObject("current_weather")
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    dateFormat.timeZone = TimeZone.getTimeZone(timezone)
                    currentWeather.put("time", dateFormat.format(Date()).replace(" ", "T"))

                    //Save the JSON data
                    this.weatherData = WeatherData(data.toString(), timezone)
                    this.updateCachedWeather()
                    successCallback()
                }
                catch(e: JSONException){
                    handleError(e)
                }
                catch(e: ParseException){
                    handleError(e)
                }
            }
        }

        for(url in urls){
            queue.add(StringRequest(Request.Method.GET, url, handleResponse, handleError))
        }
    }

    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(Attribute.NAME, this.name)
        bundle.putDouble(Attribute.LATITUDE, this.latitude)
        bundle.putDouble(Attribute.LONGITUDE, this.longitude)
        bundle.putString(Attribute.TIMEZONE, this.timezone)
        bundle.putWeatherData(Attribute.CACHED_WEATHER, this.weatherData)
        val id: Int? = this._id
        if(id != null){
            bundle.putInt(Attribute.ID, id)
        }
        return bundle
    }

    companion object {
        fun currentLocation(context: Context): City? {
            val preferences = context.getSharedPreferences("cities", Context.MODE_PRIVATE)
            val name: String = preferences.getString(Preference.CURRENT_NAME, null) ?: return null
            val latitude: Double = preferences.getFloat(Preference.CURRENT_LATITUDE, 0f).toDouble()
            val longitude: Double = preferences.getFloat(Preference.CURRENT_LONGITUDE, 0f).toDouble()
            val timezone: String = preferences.getString(Preference.CURRENT_TIMEZONE, TimeZone.getDefault().id)!!    //This can't be null since the return value of this function can only be null if its second parameter is null
            val cachedWeather = preferences.getWeatherData(Preference.CACHED_WEATHER_AT_CURRENT_LOCATION, timezone)
            val city = City(context, name, latitude, longitude, timezone, cachedWeather)
            city._id = -1
            return city
        }

        fun listFromPreferences(context: Context): List<City> {    //Does not include the current location
            val preferences: SharedPreferences = context.getSharedPreferences("cities", Context.MODE_PRIVATE)
            val count: Int = preferences.getInt(Preference.COUNT, 0)
            val toReturn = mutableListOf<City>()
            for(i in 0 until count) {
                val name: String = preferences.getString(Preference.NAME(i), context.getString(R.string.unknownLocation))!!    //This can't be null since the return value of this function can only be null if its second parameter is null
                val latitude: Double = preferences.getFloat(Preference.LATITUDE(i), 0f).toDouble()
                val longitude: Double = preferences.getFloat(Preference.LONGITUDE(i), 0f).toDouble()
                val timezone: String = preferences.getString(Preference.TIMEZONE(i), TimeZone.getDefault().id)!!    //This can't be null since the return value of this function can only be null if its second parameter is null
                val cachedWeather = preferences.getWeatherData(Preference.CACHED_WEATHER(i), timezone)
                toReturn.add(City(context, name, latitude, longitude, timezone, cachedWeather))
                toReturn[i]._id = i
            }
            return toReturn
        }

        fun fromBundle(context: Context, bundle: Bundle?): City? {
            if(bundle == null || !bundle.containsKey(Attribute.LATITUDE) || !bundle.containsKey(Attribute.LONGITUDE)){
                return null
            }
            val name: String = bundle.getString(Attribute.NAME) ?: return null
            val timezone: String = bundle.getString(Attribute.TIMEZONE) ?: return null
            val city = City(context, name, bundle.getDouble(Attribute.LATITUDE), bundle.getDouble(Attribute.LONGITUDE), timezone, bundle.getWeatherData(Attribute.CACHED_WEATHER, timezone))
            if(bundle.containsKey(Attribute.ID)) {
                city._id = bundle.getInt(Attribute.ID)
            }
            return city
        }
    }
}