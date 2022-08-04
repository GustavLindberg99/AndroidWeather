package io.github.gustavlindberg99.weather;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import java.util.Calendar;
import java.util.Locale;

public abstract class WeatherWidget extends Widget{
    public static class Colored extends WeatherWidget{
        @Override
        protected @ColorRes @DrawableRes int getBackground(@DrawableRes int weatherBackground){
            if(weatherBackground == 0){
                return R.color.skyBlue;
            }
            return weatherBackground;
        }
    }

    public static class Transparent extends WeatherWidget{
        @Override
        protected @ColorRes int getBackground(@DrawableRes int weatherBackground){
            return R.color.transparent;
        }
    }

    public static class SemiTransparent extends WeatherWidget{
        @Override
        protected @ColorRes int getBackground(@DrawableRes int weatherBackground){
            return R.color.semiTransparent;
        }
    }

    private static final @IdRes int[] dayViews = new int[]{
        R.id.weather_widget_day_0,
        R.id.weather_widget_day_1,
        R.id.weather_widget_day_2,
        R.id.weather_widget_day_3,
        R.id.weather_widget_day_4,
        R.id.weather_widget_day_5
    };

    public WeatherWidget(){
        super(R.layout.widget_weather, R.id.weather_widget, R.id.weather_widget_temperature, R.id.weather_widget_maxmin_temperatures, R.id.weather_widget_background, 0, 0);
    }

    @Override
    protected void onResize(int appWidgetId, int newWidth, int newHeight){
        final boolean isSmall = newWidth < 300;
        final boolean isLarge = !isSmall && newHeight > 90;

        this.setViewVisibility(appWidgetId, R.id.weather_widget_daily, isLarge ? View.VISIBLE : View.GONE);
        this.setViewVisibility(appWidgetId, R.id.weather_widget_small_location_name, isSmall ? View.VISIBLE : View.GONE);
        this.setViewVisibility(appWidgetId, R.id.weather_widget_details, isSmall ? View.GONE : View.VISIBLE);
        this.setViewVisibility(appWidgetId, R.id.weather_widget_maxmin_temperatures, (isSmall && newHeight < 75) ? View.GONE : View.VISIBLE);

        for(int i = 0; i < dayViews.length; i++){
            this.setViewVisibility(appWidgetId, dayViews[i], (newWidth > 75 * (i + 1)) ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void setLocationName(RemoteViews views, String locationName){
        views.setTextViewText(R.id.weather_widget_small_location_name, locationName);
        views.setTextViewText(R.id.weather_widget_large_location_name, locationName);
    }

    @Override
    protected void setCurrentWeather(RemoteViews views, @DrawableRes int weatherImage, String weatherDescription){
        views.setImageViewResource(R.id.weather_widget_weather_image, weatherImage);
        views.setContentDescription(R.id.weather_widget_weather_image, weatherDescription);
        views.setTextViewText(R.id.weather_widget_weather_description, weatherDescription);
    }

    @Override
    protected void setDailyWeather(Context context, RemoteViews views, Calendar now, double[] maxTemperatures, double[] minTemperatures, int[] weatherCodes){
        final int currentDayOfTheWeek = now.get(Calendar.DAY_OF_WEEK);
        final @StringRes int[] daysOfWeek = new int[]{
            R.string.sunday_short,
            R.string.monday_short,
            R.string.tuesday_short,
            R.string.wednesday_short,
            R.string.thursday_short,
            R.string.friday_short,
            R.string.saturday_short
        };
        for(int i = 0; i < 6; i++){
            final String dayOfWeek = context.getString(daysOfWeek[(currentDayOfTheWeek + i) % 7]);
            final int weatherCode = weatherCodes[i];
            final double minTemperature = minTemperatures[i];
            final double maxTemperature = maxTemperatures[i];

            final String dayOrNight = (weatherCode <= 2 || weatherCode / 10 == 8) ? "_day" : "";
            final @DrawableRes int weatherImage = context.getResources().getIdentifier("wmo_" + weatherCode + dayOrNight, "drawable", context.getPackageName());

            views.setTextViewText(dayViews[i], String.format(Locale.US, "%s\n%s/%s", dayOfWeek, Settings.UnitFormatter.temperature(context, minTemperature), Settings.UnitFormatter.temperature(context, maxTemperature)));
            views.setTextViewCompoundDrawables(dayViews[i], 0, 0, 0, weatherImage);
        }
    }
}