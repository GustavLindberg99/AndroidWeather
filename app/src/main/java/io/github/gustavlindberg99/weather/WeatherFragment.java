package io.github.gustavlindberg99.weather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherFragment extends Fragment{
    private final Button[] _dayNames = new Button[7];
    private final ImageView[] _dayWeathers = new ImageView[7];
    private final TextView[] _dayTemperatures = new TextView[7];

    private TableLayout _dayList;
    private final TableRow[] _dailySummaries = new TableRow[7];
    private final LinearLayout[] _hourlyWeatherBars = new LinearLayout[7];
    private final Button[][] _hours = new Button[7][24];
    private final View[][] _hourBorders = new View[7][24];
    private final int[] _selectedHours = {-1, -1, -1, -1, -1, -1, -1};

    private final Button[] _sunriseViews = new Button[7], _sunsetViews = new Button[7];
    private final View[] _sunriseBorders = new View[7], _sunsetBorders = new View[7];

    private ImageView _backgroundImage, _backgroundGradient, _backgroundColor;
    private TextView _locationView;

    private TextView _currentSunrise, _currentSunset, _currentTemperature, _currentWeather, _currentWind, _currentHumidity, _currentApparentTemperature, _currentPrecipitation, _currentPressure, _currentUVIndex, _currentCloudCover, _currentDewPoint;

    private City _city;

    private boolean _showErrorMessage = true;

    public static WeatherFragment getInstance(City city){
        WeatherFragment toReturn = new WeatherFragment();
        toReturn.setArguments(city.toBundle());
        return toReturn;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        final City city = City.fromBundle(this.requireActivity(), this.getArguments());
        if(city != null){
            this._city = city;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View view = inflater.inflate(R.layout.fragment_weather, container, false);

        //Initialize the variables that point to views
        this._dayList = view.findViewById(R.id.day_list);
        for(int i = 0; i < 7; i++){
            this._dailySummaries[i] = view.findViewById(this.getResources().getIdentifier("day_" + i + "_summary", "id", this.requireActivity().getPackageName()));
            this._dayNames[i] = this._dailySummaries[i].findViewById(R.id.name);
            this._dayWeathers[i] = this._dailySummaries[i].findViewById(R.id.weather);
            this._dayTemperatures[i] = this._dailySummaries[i].findViewById(R.id.temperature);

            final int day = i;
            this._dayNames[day].setOnClickListener((View v) -> {
                if(this._hourlyWeatherBars[day] != null){
                    if(this._hourlyWeatherBars[day].getVisibility() == View.GONE){
                        this._hourlyWeatherBars[day].setVisibility(View.VISIBLE);
                    }
                    else{
                        this._hourlyWeatherBars[day].setVisibility(View.GONE);
                    }
                }
                this.createHourlyWeatherBar(day);
            });
        }

        this._backgroundImage = this.requireActivity().findViewById(R.id.background_image);
        this._backgroundGradient = this.requireActivity().findViewById(R.id.background_gradient);
        this._backgroundColor = this.requireActivity().findViewById(R.id.background_color);
        this._locationView = view.findViewById(R.id.location);
        this._currentSunrise = view.findViewById(R.id.current_sunrise);
        this._currentSunset = view.findViewById(R.id.current_sunset);
        this._currentTemperature = view.findViewById(R.id.current_temperature);
        this._currentWeather = view.findViewById(R.id.current_weather);
        this._currentWind = view.findViewById(R.id.current_wind);
        this._currentHumidity = view.findViewById(R.id.current_humidity);
        this._currentApparentTemperature = view.findViewById(R.id.current_apparent_temperature);
        this._currentPrecipitation = view.findViewById(R.id.current_precipitation);
        this._currentPressure = view.findViewById(R.id.current_pressure);
        this._currentUVIndex = view.findViewById(R.id.current_uv_index);
        this._currentCloudCover = view.findViewById(R.id.current_cloud_cover);
        this._currentDewPoint = view.findViewById(R.id.current_dew_point);

        this.createHourlyWeatherBar(0);    //This automatically calls refreshFromCache (which is needed because onResume won't be called until the fragment is fully visible, so without this it would say unknown location while scrolling to the next one)

        return view;
    }

    private void createHourlyWeatherBar(int day){    //Create these dynamically for performance reasons
        if(this._hourlyWeatherBars[day] == null || this._dayList.indexOfChild(this._hourlyWeatherBars[day]) == -1){
            this._hourlyWeatherBars[day] = (LinearLayout) View.inflate(this.requireActivity(), R.layout.hourly_weather_bar, null);
            this._hourlyWeatherBars[day].setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

            for(int i = 0; i < 24; i++){
                this._hours[day][i] = (Button) ((TableRow) this._hourlyWeatherBars[day].findViewById(R.id.daily_weather_bar)).getChildAt(i);
                this._hourBorders[day][i] = ((TableRow) this._hourlyWeatherBars[day].findViewById(R.id.hourly_borders)).getChildAt(i);
            }

            final int index = this._dayList.indexOfChild(this._dailySummaries[day]);
            this._dayList.addView(this._hourlyWeatherBars[day], index + 1);
        }

        this.refreshFromCache();    //To show the weather in the views we just created
    }

    @Override
    public void onResume(){
        super.onResume();
        this._showErrorMessage = true;
        this._city = this._city.fromUpdatedList();
        this.createHourlyWeatherBar(0);    //This is needed because sometimes the hourly weather bar is deleted while the fragment is invisible
        //refreshFromServer is automatically called in createHourlyWeatherBar
        this.refreshFromServer();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.refreshFromCache();
    }

    public void setLocation(City city){
        this._city = city;
        this.updateWeather(city.weatherData);
    }

    public void refreshFromServer(){
        this._city.updateWeatherFromServer(() -> this.setLocation(this._city), () -> {
            if(this._showErrorMessage && this.getActivity() != null){
                Toast.makeText(this.requireActivity(), R.string.noInternetConnection, Toast.LENGTH_LONG).show();
                this._showErrorMessage = false;
            }
        });
    }

    public void refreshFromCache(){
        this.updateWeather(this._city.weatherData);
    }

    @SuppressLint("SetTextI18n")
    private void updateWeather(@Nullable WeatherData data){
        if(this.getActivity() == null){
            return;
        }

        this._locationView.setText(this._city.name);
        if(this._city.isCurrentLocation()){
            this._locationView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.position, 0, 0, 0);
        }

        if(data == null){
            this._backgroundImage.setImageResource(R.color.skyBlue);
            final @ColorInt int color = this.requireActivity().getColor(R.color.skyBlue);
            this._backgroundGradient.setColorFilter(color);
            this._backgroundColor.setColorFilter(color);

            return;
        }

        final Calendar now = data.now;
        final int currentHour = now.get(Calendar.HOUR_OF_DAY);

        //Update the labels with the days of the week
        final int dayOfTheWeek = now.get(Calendar.DAY_OF_WEEK);
        final @StringRes int[] daysOfWeek = new int[]{
            R.string.sunday,
            R.string.monday,
            R.string.tuesday,
            R.string.wednesday,
            R.string.thursday,
            R.string.friday,
            R.string.saturday
        };
        for(int i = 1; i < 7; i++){
            this._dayNames[i].setText(daysOfWeek[(dayOfTheWeek + i - 1) % 7]);
        }

        //Current weather
        {
            final int weatherStringId = this.getResources().getIdentifier("wmo" + data.currentWeatherCode, "string", this.requireActivity().getPackageName());
            final String weatherString = (weatherStringId == 0) ? "--" : this.getString(weatherStringId);

            final int windDirectionStringId = this.getResources().getIdentifier("wind" + (Math.round(data.currentWindDirection / 22.5 + 1) % 16), "string", this.requireActivity().getPackageName());
            final String windString = (data.currentWindSpeed == 0) ? Settings.UnitFormatter.windSpeed(this.requireActivity(), data.currentWindSpeed) : String.format(Locale.US, "%s %s", this.getString(windDirectionStringId), Settings.UnitFormatter.windSpeed(this.requireActivity(), data.currentWindSpeed));

            this._currentSunrise.setText(data.sunrises[0] == null ? "--" : String.format(Locale.US, "%02d.%02d", data.sunrises[0].get(Calendar.HOUR_OF_DAY), data.sunrises[0].get(Calendar.MINUTE)));
            this._currentSunset.setText(data.sunsets[0] == null ? "--" : String.format(Locale.US, "%02d.%02d", data.sunsets[0].get(Calendar.HOUR_OF_DAY), data.sunsets[0].get(Calendar.MINUTE)));
            this._currentTemperature.setText(Settings.UnitFormatter.temperature(this.requireActivity(), data.currentTemperature));
            this._currentWeather.setText(weatherString);
            this._currentWind.setText(windString);
            this._currentHumidity.setText(Settings.UnitFormatter.percentage(data.currentHumidity));
            this._currentApparentTemperature.setText(Settings.UnitFormatter.temperature(this.requireActivity(), data.currentApparentTemperature));
            this._currentPrecipitation.setText(Settings.UnitFormatter.precipitation(this.requireActivity(), data.currentPrecipitation));
            this._currentPressure.setText(Settings.UnitFormatter.pressure(data.currentPressure));
            this._currentUVIndex.setText(Settings.UnitFormatter.uvIndex(this.requireActivity(), data.currentRadiation));
            this._currentCloudCover.setText(Settings.UnitFormatter.percentage(data.currentCloudCover));
            this._currentDewPoint.setText(Settings.UnitFormatter.temperature(this.requireActivity(), data.currentDewPoint));

            //Update the background (only if this fragment is the one that's currently visible)
            if(this._city.equals(((MainActivity) this.requireActivity()).selectedCity())){
                final @DrawableRes int backgroundImage = getBackgroundResource(this.requireActivity(), data.currentWeatherCode, now, data.sunrises[0], data.sunsets[0], data.latitude);
                if(backgroundImage == 0){
                    this._backgroundImage.setImageResource(R.color.skyBlue);
                    final @ColorInt int color = this.requireActivity().getColor(R.color.skyBlue);
                    this._backgroundGradient.setColorFilter(color);
                    this._backgroundColor.setColorFilter(color);
                }
                else{
                    this._backgroundImage.setImageResource(backgroundImage);
                    final Drawable backgroundDrawable = this._backgroundImage.getDrawable();
                    if(backgroundDrawable instanceof AnimationDrawable){
                        ((AnimationDrawable) backgroundDrawable).start();
                    }
                    final @ColorInt int medianColor = this.medianColor(backgroundImage);    //Use the median instead of the average, otherwise the stars in the clear night background will cause the average color to be very bright
                    this._backgroundGradient.setColorFilter(medianColor);
                    this._backgroundColor.setColorFilter(medianColor);
                }
            }
        }

        //Hourly
        for(int d = 0; d < 7; d++){
            final int day = d;
            if(this._hourlyWeatherBars[day] == null){
                continue;
            }
            final TableLayout hourlyDetailsBar = this._hourlyWeatherBars[day].findViewById(R.id.hourly_details);

            for(int h = 0; h < 24; h++){
                final int hour = h;    //Index of the current hour view in its day bar (ignoring sunrise and sunset views)
                final int hourOfDay = ((day == 0) ? currentHour + 1 : 0) + hour;    //Hour of day, can be greater than 24 if the view is for an hour tomorrow showing in the bar for today
                final int i = day * 24 + hourOfDay;    //Index of the current hour in the weekly length-168 arrays

                final Calendar time = Calendar.getInstance();    //A Calendar object (initialized below) containing the time associated with the current view
                time.setTimeZone(TimeZone.getTimeZone(this._city.timezone));
                time.set(Calendar.HOUR_OF_DAY, i);    //If i is greater than 24, it increases the day automatically
                time.set(Calendar.MINUTE, 0);
                time.set(Calendar.SECOND, 0);

                //Hours in weather bar
                final String dayOrNight = (data.hourlyWeatherCode[i] <= 2 || data.hourlyWeatherCode[i] / 10 == 8) ? (isDay(time, data.sunrises[i / 24], data.sunsets[i / 24], data.latitude) ? "_day" : "_night") : "";
                this._hours[day][hour].setText(String.format(Locale.US, "%02d\n%s", hourOfDay % 24, Settings.UnitFormatter.temperature(this.requireActivity(), data.hourlyTemperature[i])));
                this._hours[day][hour].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, this.getResources().getIdentifier("wmo_" + data.hourlyWeatherCode[i] + dayOrNight, "drawable", this.requireActivity().getPackageName()));

                //Hourly details bar
                final int windDirectionStringId = this.getResources().getIdentifier("wind" + (Math.round(data.hourlyWindDirection[i] / 22.5 + 1) % 16), "string", this.requireActivity().getPackageName());
                final double windSpeed = data.hourlyWindSpeed[i];
                final String windString = (windSpeed == 0) ? Settings.UnitFormatter.windSpeed(this.requireActivity(), windSpeed) : String.format(Locale.US, "%s %s", this.getString(windDirectionStringId), Settings.UnitFormatter.windSpeed(this.requireActivity(), windSpeed));
                final String cloudCover = Settings.UnitFormatter.percentage(data.hourlyCloudCover[i]);
                final String uvIndex = Settings.UnitFormatter.uvIndex(this.requireActivity(), data.hourlyRadiation[i]);

                if(this._selectedHours[day] == hour){
                    ((TextView) hourlyDetailsBar.findViewById(R.id.wind)).setText(windString);
                    ((TextView) hourlyDetailsBar.findViewById(R.id.cloud_cover)).setText(cloudCover);
                    ((TextView) hourlyDetailsBar.findViewById(R.id.uv_index)).setText(uvIndex);
                }

                this._hours[day][hour].setOnClickListener((View v) -> {
                    if(this._selectedHours[day] != -1){
                        this._hourBorders[day][this._selectedHours[day]].setVisibility(View.VISIBLE);
                    }
                    if(this._selectedHours[day] == hour){
                        this._selectedHours[day] = -1;
                        hourlyDetailsBar.setVisibility(View.GONE);
                    }
                    else{
                        this._selectedHours[day] = hour;
                        hourlyDetailsBar.setVisibility(View.VISIBLE);
                        this._hourBorders[day][hour].setVisibility(View.INVISIBLE);

                        ((TextView) hourlyDetailsBar.findViewById(R.id.wind)).setText(windString);
                        ((TextView) hourlyDetailsBar.findViewById(R.id.cloud_cover)).setText(cloudCover);
                        ((TextView) hourlyDetailsBar.findViewById(R.id.uv_index)).setText(uvIndex);
                    }
                });
            }

            //Sunrise and sunset
            Calendar sunrise = data.sunrises[day];
            Calendar sunset = data.sunsets[day];
            final TableRow hourlyWeatherBarLayout = this._hourlyWeatherBars[day].findViewById(R.id.daily_weather_bar);
            final TableRow borders = this._hourlyWeatherBars[day].findViewById(R.id.hourly_borders);
            if(this._sunriseViews[day] != null){
                hourlyWeatherBarLayout.removeView(this._sunriseViews[day]);
                this._sunriseViews[day] = null;
                borders.removeView(this._sunriseBorders[day]);
                this._sunriseBorders[day] = null;
            }
            if(this._sunsetViews[day] != null){
                hourlyWeatherBarLayout.removeView(this._sunsetViews[day]);
                this._sunsetViews[day] = null;
                borders.removeView(this._sunsetBorders[day]);
                this._sunsetBorders[day] = null;
            }
            if(sunrise != null){
                int offset = 0;
                if(day == 0){
                    if(sunrise.getTimeInMillis() < now.getTimeInMillis()){
                        sunrise = data.sunrises[1];
                        offset = 23 - currentHour;
                    }
                    else{
                        offset = -1 - currentHour;
                    }
                }
                if(sunrise != null){
                    this._sunriseViews[day] = new Button(this.requireActivity(), null, R.attr.buttonBarButtonStyle);
                    this._sunriseViews[day].setEnabled(false);
                    this._sunriseViews[day].setTextColor(this.requireActivity().getColor(R.color.white));
                    this._sunriseViews[day].setText(String.format(Locale.US, "%02d.%02d\n", sunrise.get(Calendar.HOUR_OF_DAY), sunrise.get(Calendar.MINUTE)) + this.getString(R.string.sunrise));
                    this._sunriseViews[day].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.sunrise);
                    final int index = sunrise.get(Calendar.HOUR_OF_DAY) + offset + 1;
                    hourlyWeatherBarLayout.addView(this._sunriseViews[day], index);

                    this._sunriseBorders[day] = new View(this.requireActivity());
                    this._sunriseBorders[day].setBackgroundColor(this.requireActivity().getColor(R.color.white));
                    this._sunriseBorders[day].setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, dpToPx(0.5)));
                    borders.addView(this._sunriseBorders[day], index);
                }
            }
            if(sunset != null){
                int offset = 0;
                if(day == 0){
                    if(sunset.getTimeInMillis() < now.getTimeInMillis()){
                        sunset = data.sunsets[1];
                        offset = 23 - currentHour;
                    }
                    else{
                        offset = -1 - currentHour;
                    }
                }
                if(sunset != null){
                    this._sunsetViews[day] = new Button(this.requireActivity(), null, R.attr.buttonBarButtonStyle);
                    this._sunsetViews[day].setEnabled(false);
                    this._sunsetViews[day].setTextColor(this.requireActivity().getColor(R.color.white));
                    this._sunsetViews[day].setText(String.format(Locale.US, "%02d.%02d\n", sunset.get(Calendar.HOUR_OF_DAY), sunset.get(Calendar.MINUTE)) + this.getString(R.string.sunset));
                    this._sunsetViews[day].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.sunset);
                    final int index = sunset.get(Calendar.HOUR_OF_DAY) + offset + 1 + ((sunrise != null && sunrise.getTimeInMillis() < sunset.getTimeInMillis()) ? 1 : 0);
                    hourlyWeatherBarLayout.addView(this._sunsetViews[day], index);

                    this._sunsetBorders[day] = new View(this.requireActivity());
                    this._sunsetBorders[day].setBackgroundColor(this.requireActivity().getColor(R.color.white));
                    this._sunsetBorders[day].setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, dpToPx(0.5)));
                    borders.addView(this._sunsetBorders[day], index);
                }
            }
        }

        //Daily
        for(int i = 0; i < 7; i++){
            boolean isEvening;
            if((i == 0 || data.sunrises[i] == null) && data.sunsets[i] == null){
                if(now.get(Calendar.MONTH) >= Calendar.APRIL && now.get(Calendar.MONTH) <= Calendar.SEPTEMBER){
                    isEvening = data.latitude < 0;
                }
                else{
                    isEvening = data.latitude > 0;
                }
            }
            else{
                isEvening = i == 0 && now.getTimeInMillis() > data.sunsets[0].getTimeInMillis();
            }

            final String dayOrNight = (data.dailyWeatherCode[i] <= 2 || data.dailyWeatherCode[i] / 10 == 8) ? (isEvening ? "_night" : "_day") : "";
            this._dayTemperatures[i].setText(String.format(Locale.US, "%s/%s", Settings.UnitFormatter.temperature(this.requireActivity(), data.minTemperature[i]), Settings.UnitFormatter.temperature(this.requireActivity(), data.maxTemperature[i])));
            this._dayWeathers[i].setImageResource(this.getResources().getIdentifier("wmo_" + data.dailyWeatherCode[i] + dayOrNight, "drawable", this.requireActivity().getPackageName()));
            this._dayWeathers[i].setContentDescription(this.getString(this.getResources().getIdentifier("wmo" + data.dailyWeatherCode[i], "string", this.requireActivity().getPackageName())));
        }
    }

    private @ColorInt int medianColor(@DrawableRes int image){
        final Bitmap bitmap = BitmapFactory.decodeResource(this.requireActivity().getResources(), removeAnimation(image));
        final int resolution = 20;    //Don't look at all pixels for performance reasons (this function is very slow otherwise, and no one will notice if the median isn't exact)
        final int width = bitmap.getWidth(), height = bitmap.getHeight();
        final int size = (int) Math.ceil((double) width / resolution) * (int) Math.ceil((double) height / resolution);

        int[] red = new int[size], green = new int[size], blue = new int[size];
        for(int x = 0, i = 0; x < width; x += resolution){
            for(int y = 0; y < height; y += resolution, i++){
                final @ColorInt int color = bitmap.getPixel(x, y);
                red[i] = color & 0x00FF0000;    //This is actually red*255^2, but it doesn't matter since we're only interested in sorting it
                green[i] = color & 0x0000FF00;    //This is actually green*255, but it doesn't matter since we're only interested in sorting it
                blue[i] = color & 0x000000FF;
            }
        }
        Arrays.sort(red);
        Arrays.sort(green);
        Arrays.sort(blue);
        return 0xFF000000 | red[size / 2] | green[size / 2] | blue[size / 2];    //0xFF000000 for the alpha channel (which is always maximal for the images we use this function on)
    }

    public static boolean isDay(Calendar time, @Nullable Calendar sunrise, @Nullable Calendar sunset, double latitude){
        if(sunrise != null && sunset != null && sunrise.getTimeInMillis() == sunset.getTimeInMillis()){
            sunrise = sunset = null;
        }
        if(sunrise == null && sunset == null){
            if(time.get(Calendar.MONTH) >= Calendar.APRIL && time.get(Calendar.MONTH) <= Calendar.SEPTEMBER){
                return latitude > 0;
            }
            else{
                return latitude < 0;
            }
        }
        else if(sunrise == null){
            return time.getTimeInMillis() <= sunset.getTimeInMillis();
        }
        else if(sunset == null){
            return time.getTimeInMillis() > sunrise.getTimeInMillis();
        }
        else if(sunrise.getTimeInMillis() > sunset.getTimeInMillis()){
            return time.getTimeInMillis() > sunrise.getTimeInMillis() || time.getTimeInMillis() <= sunset.getTimeInMillis();
        }
        return time.getTimeInMillis() > sunrise.getTimeInMillis() && time.getTimeInMillis() <= sunset.getTimeInMillis();    //This is the by far most common (sunrise comes before sunset), all the above are edge cases that can occur in polar areas.
    }

    public static @DrawableRes int getBackgroundResource(Context context, int weatherCode, Calendar now, Calendar sunrise, Calendar sunset, double latitude){
        final boolean day = isDay(now, sunrise, sunset, latitude);

        String resourceName;
        switch(weatherCode / 10){
        case 0:
            resourceName = "bg_" + weatherCode;
            break;
        case 4:
            resourceName = "bg_fog";
            break;
        case 5:
        case 6:
        case 8:
            if(weatherCode != 85 && weatherCode != 86){    //85 and 86 are snow, so if that's the case don't break and continue to the next part
                resourceName = "bg_rain";
                break;
            }
        case 7:
            resourceName = "bg_snow";
            break;
        case 9:
            resourceName = "bg_thunderstorm";
            break;
        default:
            return 0;
        }
        final String dayOrNight = day ? "day" : "night";
        resourceName += "_" + dayOrNight;
        return context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
    }

    private static @DrawableRes int removeAnimation(@DrawableRes int resourceId){
        if(resourceId == R.drawable.bg_rain_day || resourceId == R.drawable.bg_thunderstorm_day){
            return R.drawable.bg_rain_day_0;
        }
        else if(resourceId == R.drawable.bg_rain_night || resourceId == R.drawable.bg_thunderstorm_night){
            return R.drawable.bg_rain_night_0;
        }
        else if(resourceId == R.drawable.bg_snow_day){
            return R.drawable.bg_snow_day_0;
        }
        else if(resourceId == R.drawable.bg_snow_night){
            return R.drawable.bg_snow_night_0;
        }
        return resourceId;
    }

    public static int dpToPx(double dp){
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density + 0.5);
    }
}