package io.github.gustavlindberg99.weather;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Ints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class WeatherData{
    private final String _json;

    public final @NonNull Calendar now;
    public final double latitude;

    //Current variables
    public final double currentTemperature, currentApparentTemperature;
    public final int currentWeatherCode;
    public final double currentWindSpeed, currentWindDirection;
    public final int currentHumidity;
    public final double currentPrecipitation;
    public final double currentPressure;
    public final double currentRadiation;
    public final int currentCloudCover;
    public final double currentDewPoint;

    //Hourly variables
    public final int[] hourlyWeatherCode = new int[168];
    public final double[] hourlyTemperature = new double[168];
    public final double[] hourlyWindSpeed = new double[168], hourlyWindDirection = new double[168];
    public final int[] hourlyCloudCover = new int[168];
    public final double[] hourlyRadiation = new double[168];

    //Daily variables
    public final Calendar[] sunrises = new Calendar[7], sunsets = new Calendar[7];
    public final double[] maxTemperature = new double[7], minTemperature = new double[7];
    public final int[] dailyWeatherCode = new int[7];

    public WeatherData(String json, String timezone) throws JSONException, ParseException{
        this._json = json;
        final JSONObject data = new JSONObject(json);
        final JSONObject currentWeather = data.getJSONObject("current_weather");
        final JSONObject hourly = data.getJSONObject("hourly");
        final JSONObject daily = data.getJSONObject("daily");
        this.latitude = data.getDouble("latitude");

        this.now = MoreObjects.firstNonNull(parseDate(currentWeather.getString("time"), timezone), Calendar.getInstance());
        this.now.setTimeZone(TimeZone.getTimeZone(timezone));
        final int currentHour = this.now.get(Calendar.HOUR_OF_DAY);

        //Current variables
        this.currentTemperature = currentWeather.getDouble("temperature");
        this.currentCloudCover = totalCloudCover(hourly.getJSONArray("cloudcover_low").getInt(currentHour), hourly.getJSONArray("cloudcover_mid").getInt(currentHour));
        this.currentWeatherCode = weatherCodeFromCloudCover(currentWeather.getInt("weathercode"), this.currentCloudCover);
        this.currentWindSpeed = currentWeather.getDouble("windspeed");
        this.currentWindDirection = currentWeather.getDouble("winddirection");
        this.currentHumidity = hourly.getJSONArray("relativehumidity_2m").getInt(currentHour);
        this.currentApparentTemperature = hourly.getJSONArray("apparent_temperature").getInt(currentHour);
        this.currentPrecipitation = hourly.getJSONArray("precipitation").getDouble(currentHour + 1);
        this.currentPressure = hourly.getJSONArray("pressure_msl").getDouble(currentHour);
        this.currentRadiation = hourly.getJSONArray("shortwave_radiation").getDouble(currentHour + 1);
        this.currentDewPoint = hourly.getJSONArray("dewpoint_2m").getDouble(currentHour);

        //Hourly variables
        for(int i = 0; i < 168; i++){
            this.hourlyTemperature[i] = nullSafeDouble(hourly.getJSONArray("temperature_2m"), i);
            this.hourlyCloudCover[i] = totalCloudCover(nullSafeInt(hourly.getJSONArray("cloudcover_low"), i), nullSafeInt(hourly.getJSONArray("cloudcover_mid"), i));
            this.hourlyWeatherCode[i] = weatherCodeFromCloudCover(nullSafeInt(hourly.getJSONArray("weathercode"), i), this.hourlyCloudCover[i]);
            this.hourlyWindSpeed[i] = nullSafeDouble(hourly.getJSONArray("windspeed_10m"), i);
            if(this.hourlyWindSpeed[i] != 0){    //If this is zero the wind direction will be null which would cause an error if the code below is executed
                this.hourlyWindDirection[i] = nullSafeDouble(hourly.getJSONArray("winddirection_10m"), i);
            }
            this.hourlyRadiation[i] = nullSafeDouble(hourly.getJSONArray("shortwave_radiation"), i);
        }

        //Daily variables
        for(int i = 0; i < 7; i++){
            this.sunrises[i] = parseDate(daily.getJSONArray("sunrise").getString(i), timezone);
            this.sunsets[i] = parseDate(daily.getJSONArray("sunset").getString(i), timezone);
            this.maxTemperature[i] = daily.getJSONArray("temperature_2m_max").getDouble(i);
            this.minTemperature[i] = daily.getJSONArray("temperature_2m_min").getDouble(i);

            //Use my own algorithm for determining the daily weather code because the one provided by the API gives to high priority to how the weather will be during the night
            if(i > 0 || currentHour < 10){
                this.dailyWeatherCode[i] = combinedWeatherCode(hourlyWeatherCode[24 * i + 10], hourlyWeatherCode[24 * i + 13], hourlyWeatherCode[24 * i + 16], hourlyWeatherCode[24 * i + 19]);
            }
            else if(currentHour < 13){
                this.dailyWeatherCode[i] = combinedWeatherCode(hourlyWeatherCode[13], hourlyWeatherCode[16], hourlyWeatherCode[19]);
            }
            else if(currentHour < 16){
                this.dailyWeatherCode[i] = combinedWeatherCode(hourlyWeatherCode[16], hourlyWeatherCode[19]);
            }
            else{
                this.dailyWeatherCode[i] = combinedWeatherCode(hourlyWeatherCode[currentHour + 1], hourlyWeatherCode[currentHour + 2], hourlyWeatherCode[currentHour + 3]);
            }
        }
    }

    //This function and the next one are needed because a bug in the API (https://github.com/open-meteo/open-meteo/issues/71). If the value is null when it's not supposed to be, the function returns the non-null value that's closest in time.
    private static double nullSafeDouble(JSONArray array, int index) throws JSONException{
        if(array.isNull(index)){
            for(int low = index - 1, high = index + 1; low >= 0 || high < array.length(); low--, high++){
                if(low >= 0 && !array.isNull(low)){
                    return array.getDouble(low);
                }
                if(high < array.length() && !array.isNull(high)){
                    return array.getDouble(high);
                }
            }
        }
        return array.getDouble(index);
    }

    private static int nullSafeInt(JSONArray array, int index) throws JSONException{
        if(array.isNull(index)){
            for(int low = index - 1, high = index + 1; low >= 0 || high < array.length(); low--, high++){
                if(low >= 0 && !array.isNull(low)){
                    return array.getInt(low);
                }
                if(high < array.length() && !array.isNull(high)){
                    return array.getInt(high);
                }
            }
        }
        return array.getInt(index);
    }

    private static int totalCloudCover(int cloudCoverLow, int cloudCoverMid){
        final int sunCoverLow = 100 - cloudCoverLow;
        final int sunCoverMid = 100 - cloudCoverMid;
        final int totalSunCover = (sunCoverLow * sunCoverMid) / 100;
        return 100 - totalSunCover;
    }

    //We need to get the weather code from the cloud cover manually because sometime the weather code from the API is 3 (cloudy) when there are only very high clouds that aren't visible. The cloudCover parameter takes this into account by only using cloudcover_low and cloudcover_mid.
    private static int weatherCodeFromCloudCover(int weatherCode, int cloudCover){
        if(weatherCode > 3){
            return weatherCode;
        }
        else if(cloudCover > 75){
            return 3;
        }
        else if(cloudCover > 50){
            return 2;
        }
        else if(cloudCover > 25){
            return 1;
        }
        else{
            return 0;
        }
    }

    public void putToBundle(Bundle bundle, String key){
        bundle.putString(key, this._json);
    }

    public static @Nullable WeatherData getFromBundle(Bundle bundle, String key, String timezone){
        try{
            return new WeatherData(Objects.requireNonNull(bundle.getString(key)), timezone);
        }
        catch(JSONException | ParseException | NullPointerException ignore){
            return null;
        }
    }

    public void putToPreferences(SharedPreferences.Editor preferencesEditor, String key){
        preferencesEditor.putString(key, this._json);
    }

    public static @Nullable WeatherData getFromPreferences(SharedPreferences preferences, String key, String timezone){
        try{
            return new WeatherData(Objects.requireNonNull(preferences.getString(key, null)), timezone);
        }
        catch(JSONException | ParseException | NullPointerException ignore){
            return null;
        }
    }

    private static @Nullable Calendar parseDate(String date, String timezone) throws ParseException{
        if(date.startsWith("1900")){    //The API returns a date in 1900 when it should return an invalid date.
            return null;
        }
        final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar calendar = Calendar.getInstance();
        try{
            calendar.setTimeZone(TimeZone.getTimeZone(timezone));
            calendar.setTime(Objects.requireNonNull(format.parse(date.replace('T', ' '))));
        }
        catch(NullPointerException ignore){
            throw new ParseException("Null returned when parsing date " + date, 0);
        }
        return calendar;
    }

    private static int combinedWeatherCode(int... weatherCode){
        ArrayList<Integer> weatherCodeList = new ArrayList<>(Ints.asList(weatherCode));
        final List<Integer> orderedFog = Arrays.asList(0, 1, 2, 3, 45, 48);
        final List<Integer> orderedDrizzle = Arrays.asList(0, 1, 2, 3, 51, 56, 53, 57, 55);
        final List<Integer> orderedThunderstorms = Arrays.asList(3, 95, 96, 99);
        final List<Integer> orderedRain = Arrays.asList(0, 1, 2, 3, 80, 61, 81, 63, 82, 65, 95, 96, 99);
        final List<Integer> orderedFreezingRain = Arrays.asList(0, 1, 2, 3, 66, 67, 95, 96, 99);
        final List<Integer> orderedSnow = Arrays.asList(0, 1, 2, 3, 85, 71, 73, 86, 77, 75);

        for(List<Integer> l: Arrays.asList(orderedFog, orderedDrizzle, orderedThunderstorms, orderedRain, orderedFreezingRain, orderedSnow)){
            //If all the elements in weatherCodeList are of a certain type (fog, drizzle, rain or snow), take the average of them in the order defined above
            if(l.containsAll(weatherCodeList)){
                int sum = 0;
                for(Integer i: weatherCodeList){
                    sum += l.indexOf(i);
                }
                final int toReturn = l.get((int) Math.round((double) sum / weatherCodeList.size()));
                if(Collections.disjoint(Ints.asList(weatherCode), Arrays.asList(0, 1, 2, 80, 81, 82, 85, 86))){
                    //Don't say there will be sun if there won't
                    switch(toReturn){
                    case 80:
                        return 61;
                    case 81:
                        return 63;
                    case 82:
                        return 65;
                    case 85:
                        return 71;
                    case 86:
                        return 75;
                    }
                }
                return toReturn;
            }
            //Otherwise remove all elements of that specific type and try with another type
            else{
                weatherCodeList.removeAll(l.subList(4, l.size()));
            }
        }
        return -1;    //This shouldn't happen if all arguments are valid WMO codes
    }
}
