package io.github.gustavlindberg99.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class City{
    private final Context _context;
    public final String name;
    public final double latitude, longitude;
    public final String timezone;
    public @Nullable WeatherData weatherData;
    private @Nullable Integer _id = null;    //null if the location isn't saved, -1 if it's the current location

    private static abstract class Attribute{
        public static final String NAME = "name";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String TIMEZONE = "timezone";
        public static final String CACHED_WEATHER = "cachedWeather";
        public static final String ID = "id";
    }

    private static abstract class Preference{
        public static final String COUNT = "count";
        public static final String CURRENT_NAME = "currentName";
        public static final String CURRENT_LATITUDE = "currentLatitude";
        public static final String CURRENT_LONGITUDE = "currentLongitude";
        public static final String CURRENT_TIMEZONE = "currentTimezone";
        public static final String CACHED_WEATHER_AT_CURRENT_LOCATION = "cachedWeather";

        public static String NAME(int id){
            if(id == -1){
                return CURRENT_NAME;
            }
            return "name" + id;
        }
        public static String LATITUDE(int id){
            if(id == -1){
                return CURRENT_LATITUDE;
            }
            return "latitude" + id;
        }
        public static String LONGITUDE(int id){
            if(id == -1){
                return CURRENT_LONGITUDE;
            }
            return "longitude" + id;
        }
        public static String TIMEZONE(int id){
            if(id == -1){
                return CURRENT_TIMEZONE;
            }
            return "timezone" + id;
        }
        public static String CACHED_WEATHER(int id){
            if(id == -1){
                return CACHED_WEATHER_AT_CURRENT_LOCATION;
            }
            return "cachedWeather" + id;
        }
    }

    City(Context context, @NonNull String name, double latitude, double longitude, String timezone, @Nullable WeatherData cachedWeather){
        this._context = context;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
        this.weatherData = cachedWeather;
    }

    City(Context context, @NonNull String name, double latitude, double longitude, String timezone){
        this(context, name, latitude, longitude, timezone, null);
    }

    @Override
    public boolean equals(@Nullable Object otherObject){
        if(!(otherObject instanceof City)){
            return false;
        }
        final City other = (City) otherObject;
        if(this._id == null || other._id == null){
            return this.latitude == other.latitude && this.longitude == other.longitude;
        }
        return this._id.intValue() == other._id.intValue();
    }

    public City fromUpdatedList(){
        if(this._id == null){
            return this;
        }
        if(this._id == -1){
            return City.currentLocation(this._context);
        }
        final City[] cityList = City.listFromPreferences(this._context);
        if(this._id < cityList.length){
            return cityList[this._id];
        }
        else{
            return this;
        }
    }

    public static @Nullable City currentLocation(Context context){
        SharedPreferences preferences = context.getSharedPreferences("cities", Context.MODE_PRIVATE);
        final @Nullable String name = preferences.getString(Preference.CURRENT_NAME, null);
        final float latitude = preferences.getFloat(Preference.CURRENT_LATITUDE, 0);
        final float longitude = preferences.getFloat(Preference.CURRENT_LONGITUDE, 0);
        final String timezone = preferences.getString(Preference.CURRENT_TIMEZONE, TimeZone.getDefault().getID());
        final WeatherData cachedWeather = WeatherData.getFromPreferences(preferences, Preference.CACHED_WEATHER_AT_CURRENT_LOCATION, timezone);

        if(name == null){
            return null;
        }

        City city = new City(context, name, latitude, longitude, timezone, cachedWeather);
        city._id = -1;
        return city;
    }

    public boolean isCurrentLocation(){
        return this._id != null && this._id == -1;
    }

    public static City[] listFromPreferences(Context context){    //Does not include the current location
        SharedPreferences preferences = context.getSharedPreferences("cities", Context.MODE_PRIVATE);
        final int count = preferences.getInt(Preference.COUNT, 0);
        City[] toReturn = new City[count];
        for(int i = 0; i < count; i++){
            final String name = preferences.getString(Preference.NAME(i), context.getString(R.string.unknownLocation));
            final float latitude = preferences.getFloat(Preference.LATITUDE(i), 0);
            final float longitude = preferences.getFloat(Preference.LONGITUDE(i), 0);
            final String timezone = preferences.getString(Preference.TIMEZONE(i), TimeZone.getDefault().getID());
            final WeatherData cachedWeather = WeatherData.getFromPreferences(preferences, Preference.CACHED_WEATHER(i), timezone);
            toReturn[i] = new City(context, name, latitude, longitude, timezone, cachedWeather);
            toReturn[i]._id = i;
        }
        return toReturn;
    }

    public void saveAsCurrentLocation(){
        SharedPreferences.Editor preferenceEditor = this._context.getSharedPreferences("cities", Context.MODE_PRIVATE).edit();
        preferenceEditor.putString(Preference.CURRENT_NAME, this.name);
        preferenceEditor.putFloat(Preference.CURRENT_LATITUDE, (float) this.latitude);
        preferenceEditor.putFloat(Preference.CURRENT_LONGITUDE, (float) this.longitude);
        preferenceEditor.putString(Preference.CURRENT_TIMEZONE, this.timezone);
        if(this.weatherData != null){
            this.weatherData.putToPreferences(preferenceEditor, Preference.CACHED_WEATHER_AT_CURRENT_LOCATION);
        }
        preferenceEditor.apply();
        this._id = -1;
    }

    public void addAsNewLocation(){
        SharedPreferences preferences = this._context.getSharedPreferences("cities", Context.MODE_PRIVATE);
        this._id = preferences.getInt(Preference.COUNT, 0);
        SharedPreferences.Editor preferenceEditor = preferences.edit();
        preferenceEditor.putString(Preference.NAME(this._id), this.name);
        preferenceEditor.putFloat(Preference.LATITUDE(this._id), (float) this.latitude);
        preferenceEditor.putFloat(Preference.LONGITUDE(this._id), (float) this.longitude);
        preferenceEditor.putString(Preference.TIMEZONE(this._id), this.timezone);
        if(this.weatherData != null){
            this.weatherData.putToPreferences(preferenceEditor, Preference.CACHED_WEATHER(this._id));
        }
        preferenceEditor.putInt(Preference.COUNT, this._id + 1);
        preferenceEditor.apply();
    }

    public void removeLocation(){
        if(this._id == null || this._id == -1){
            return;
        }

        SharedPreferences preferences = this._context.getSharedPreferences("cities", Context.MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = preferences.edit();
        final City[] cityList = City.listFromPreferences(this._context);

        //Decrement locations following the one we're deleting (we don't need to explicitly delete this location, it will be overwritten by the next one)
        for(int i = this._id + 1; i < cityList.length; i++){
            final City city = cityList[i];
            preferenceEditor.putString(Preference.NAME(i - 1), city.name);
            preferenceEditor.putFloat(Preference.LATITUDE(i - 1), (float) city.latitude);
            preferenceEditor.putFloat(Preference.LONGITUDE(i - 1), (float) city.longitude);
            preferenceEditor.putString(Preference.TIMEZONE(i - 1), city.timezone);
            if(city.weatherData != null){
                city.weatherData.putToPreferences(preferenceEditor, Preference.CACHED_WEATHER(i - 1));
            }
        }

        //Delete the last city which is now a duplicate of the second last city
        preferenceEditor.remove(Preference.NAME(cityList.length - 1));
        preferenceEditor.remove(Preference.LATITUDE(cityList.length - 1));
        preferenceEditor.remove(Preference.LONGITUDE(cityList.length - 1));
        preferenceEditor.remove(Preference.TIMEZONE(cityList.length - 1));
        preferenceEditor.remove(Preference.CACHED_WEATHER(cityList.length - 1));

        //Update the count
        preferenceEditor.putInt(Preference.COUNT, cityList.length - 1);

        //Apply the changes
        preferenceEditor.apply();
        this._id = null;
    }

    public void updateCachedWeather(){
        if(this._id != null){
            SharedPreferences.Editor preferenceEditor = this._context.getSharedPreferences("cities", Context.MODE_PRIVATE).edit();
            if(this.weatherData != null){
                this.weatherData.putToPreferences(preferenceEditor, Preference.CACHED_WEATHER(this._id));
            }
            preferenceEditor.apply();
        }
    }

    public void updateWeatherFromServer(Runnable successCallback, Runnable errorCallback){
        final String url = "https://api.open-meteo.com/v1/forecast?latitude=" + this.latitude + "&longitude=" + this.longitude + "&timezone=" + this.timezone + "&current_weather=true&hourly=temperature_2m,weathercode,relativehumidity_2m,apparent_temperature,precipitation,pressure_msl,shortwave_radiation,cloudcover,dewpoint_2m,winddirection_10m,windspeed_10m&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset";
        RequestQueue queue = Volley.newRequestQueue(this._context);

        StringRequest request = new StringRequest(Request.Method.GET, url, (String response) -> {
            try{
                //Fix the current time so that it shows the actual current time rather than the last whole hour
                JSONObject data = new JSONObject(response);
                JSONObject currentWeather = data.getJSONObject("current_weather");
                final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone(this.timezone));
                currentWeather.put("time", dateFormat.format(new Date()).replace(" ", "T"));

                //Save the JSON data
                this.weatherData = new WeatherData(data.toString(), this.timezone);
                this.updateCachedWeather();
                successCallback.run();
            }
            catch(JSONException | ParseException ignore){
                errorCallback.run();
            }
        }, (VolleyError error) ->
            //The error handler is also run on 4xx errors (this was unclear from the documentation so I tested it)
            errorCallback.run()
        );

        queue.add(request);
    }

    public Bundle toBundle(){
        Bundle bundle = new Bundle();
        bundle.putString(Attribute.NAME, this.name);
        bundle.putDouble(Attribute.LATITUDE, this.latitude);
        bundle.putDouble(Attribute.LONGITUDE, this.longitude);
        bundle.putString(Attribute.TIMEZONE, this.timezone);
        if(this.weatherData != null){
            this.weatherData.putToBundle(bundle, Attribute.CACHED_WEATHER);
        }
        if(this._id != null){
            bundle.putInt(Attribute.ID, this._id);
        }
        return bundle;
    }

    public static @Nullable City fromBundle(Context context, Bundle bundle){
        if(bundle == null || !bundle.containsKey(Attribute.NAME) || !bundle.containsKey(Attribute.LATITUDE) || !bundle.containsKey(Attribute.LONGITUDE) || !bundle.containsKey(Attribute.TIMEZONE)){
            return null;
        }
        final String timezone = bundle.getString(Attribute.TIMEZONE);
        City city = new City(context, bundle.getString(Attribute.NAME), bundle.getDouble(Attribute.LATITUDE), bundle.getDouble(Attribute.LONGITUDE), timezone, WeatherData.getFromBundle(bundle, Attribute.CACHED_WEATHER, timezone));
        if(bundle.containsKey(Attribute.ID)){
            city._id = bundle.getInt(Attribute.ID);
        }
        return city;
    }
}
