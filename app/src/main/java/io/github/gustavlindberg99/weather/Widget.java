package io.github.gustavlindberg99.weather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

public abstract class Widget extends AppWidgetProvider{
    private final @LayoutRes int _layout;
    private final @IdRes int _layoutId, _currentTemperature, _maxminTemperatures, _background, _clock, _date;
    private final HashMap<Integer, HashMap<Integer, Integer>> _viewVisibilities = new HashMap<>(), _fontSizes = new HashMap<>();

    public Widget(@LayoutRes int layout, @IdRes int layoutId, @IdRes int currentTemperature, @IdRes int maxminTemperatures, @IdRes int background, @IdRes int clock, @IdRes int date){
        this._layout = layout;
        this._layoutId = layoutId;
        this._currentTemperature = currentTemperature;
        this._maxminTemperatures = maxminTemperatures;
        this._background = background;
        this._clock = clock;
        this._date = date;
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions){
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        this.onUpdate(context, appWidgetManager, new int[]{appWidgetId});    //This automatically calls onResize
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);

        if(intent.getAction().contentEquals("com.sec.android.widgetapp.APPWIDGET_RESIZE")) {
            final int appWidgetId = intent.getIntExtra("widgetId", 0);
            if(appWidgetId > 0){
                this.onUpdate(context, AppWidgetManager.getInstance(context), new int[]{appWidgetId});    //This automatically calls onResize
            }
        }
    }

    protected void onResize(int appWidgetId, int newWidth, int newHeight){}

    protected void setViewVisibility(int appWidgetId, @IdRes int viewId, int visibility){
        if(!this._viewVisibilities.containsKey(appWidgetId)){
            this._viewVisibilities.put(appWidgetId, new HashMap<>());
        }
        Objects.requireNonNull(this._viewVisibilities.get(appWidgetId)).put(viewId, visibility);
    }

    protected void setFontSize(int appWidgetId, @IdRes int textViewId, int fontSize){
        if(!this._fontSizes.containsKey(appWidgetId)){
            this._fontSizes.put(appWidgetId, new HashMap<>());
        }
        Objects.requireNonNull(this._fontSizes.get(appWidgetId)).put(textViewId, fontSize);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
        //Send the resize event so that we know the size of the widget
        for(int appWidgetId: appWidgetIds){
            final Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            final int widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            final int widgetHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            this.onResize(appWidgetId, widgetWidth, widgetHeight);
        }

        //Update the weather
        final City currentLocation = City.currentLocation(context);
        City location;
        if(currentLocation != null){
            location = currentLocation;
        }
        else{
            final City[] cities = City.listFromPreferences(context);
            if(cities.length == 0){
                return;
            }
            location = cities[0];
        }
        this.updateWeather(context, appWidgetManager, appWidgetIds, location);    //Update from cache so that it shows something reasonable while waiting for the server
        location.updateWeatherFromServer(() -> this.updateWeather(context, appWidgetManager, appWidgetIds, location), () -> {});
    }

    private void updateWeather(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, @NonNull City location){
        final @Nullable WeatherData data = location.weatherData;
        if(data == null){
            return;
        }

        final Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone(location.timezone));
        final String dayOrNight = (data.currentWeatherCode <= 2 || data.currentWeatherCode / 10 == 8) ? (WeatherFragment.isDay(now, data.sunrises[0], data.sunsets[0], location.latitude) ? "_day" : "_night") : "";

        //There may be multiple widgets active, so update all of them
        for(int appWidgetId: appWidgetIds){
            RemoteViews views = new RemoteViews(context.getPackageName(), this._layout);

            //Set the onclick listener
            Intent weatherIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingWeatherIntent = PendingIntent.getActivity(context, 0, weatherIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(this._layoutId, pendingWeatherIntent);

            if(this._clock != 0){
                Intent clockIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                PendingIntent pendingClockIntent = PendingIntent.getActivity(context, 0, clockIntent, PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(this._clock, pendingClockIntent);
            }

            //Update the weather
            this.setLocationName(views, location.name);
            final @DrawableRes int weatherImage = context.getResources().getIdentifier("wmo_" + data.currentWeatherCode + dayOrNight, "drawable", context.getPackageName());
            final String weatherDescription = context.getString(context.getResources().getIdentifier("wmo" + data.currentWeatherCode, "string", context.getPackageName()));
            this.setCurrentWeather(views, weatherImage, weatherDescription);
            views.setTextViewText(this._currentTemperature, Settings.UnitFormatter.temperature(context, data.currentTemperature));
            final @DrawableRes int weatherBackground = WeatherFragment.getBackgroundResource(context, data.currentWeatherCode, now, data.sunrises[0], data.sunsets[0], location.latitude);
            views.setImageViewResource(this._background, this.getBackground(weatherBackground));
            views.setTextViewText(this._maxminTemperatures, Settings.UnitFormatter.temperature(context, data.minTemperature[0]) + "/" + Settings.UnitFormatter.temperature(context, data.maxTemperature[0]));
            this.setDailyWeather(context, views, data.now, data.maxTemperature, data.minTemperature, data.dailyWeatherCode);
            views.setTextViewText(this._date, DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Calendar.getInstance().getTimeInMillis()));

            //Set the visibilities of the views
            final @Nullable HashMap<Integer, Integer> viewVisibilities = this._viewVisibilities.get(appWidgetId);
            if(viewVisibilities != null){
                for(Map.Entry<Integer, Integer> viewVisibility: viewVisibilities.entrySet()){
                    views.setViewVisibility(viewVisibility.getKey(), viewVisibility.getValue());
                }
            }

            //Set the font sizes of the text views
            final @Nullable HashMap<Integer, Integer> fontSizes = this._fontSizes.get(appWidgetId);
            if(fontSizes != null){
                for(Map.Entry<Integer, Integer> fontSize: fontSizes.entrySet()){
                    views.setTextViewTextSize(fontSize.getKey(), TypedValue.COMPLEX_UNIT_SP, fontSize.getValue());
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    protected abstract @ColorRes @DrawableRes int getBackground(@DrawableRes int weatherBackground);
    protected abstract void setLocationName(RemoteViews views, String locationName);
    protected abstract void setCurrentWeather(RemoteViews views, @DrawableRes int weatherImage, String weatherDescription);
    protected abstract void setDailyWeather(Context context, RemoteViews views, Calendar now, double[] maxTemperatures, double[] minTemperatures, int[] weatherCodes);
}