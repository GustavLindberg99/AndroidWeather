package io.github.gustavlindberg99.weather;

import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import java.util.Calendar;

public class ClockWidget extends Widget{
    public ClockWidget(){
        super(R.layout.widget_clock, R.id.clock_widget_weather, R.id.clock_widget_weather, 0, 0, R.id.clock_widget_clock, R.id.clock_widget_date);
    }

    @Override
    protected void onResize(int appWidgetId, int newWidth, int newHeight){
        final int fontSize = (newWidth < 300 || newHeight < 60) ? 32 : 48;
        this.setFontSize(appWidgetId, R.id.clock_widget_weather, fontSize);
        this.setFontSize(appWidgetId, R.id.clock_widget_clock, fontSize);
    }

    @Override
    protected @ColorRes int getBackground(@DrawableRes int weatherBackground){
        return R.color.transparent;
    }

    @Override
    protected void setLocationName(RemoteViews views, String locationName){
        views.setTextViewText(R.id.clock_widget_location, locationName);
    }

    @Override
    protected void setCurrentWeather(RemoteViews views, @DrawableRes int weatherImage, String weatherDescription){
        views.setTextViewCompoundDrawables(R.id.clock_widget_weather, weatherImage, 0, 0, 0);
    }

    @Override
    protected void setDailyWeather(Context context, RemoteViews views, Calendar now, double[] maxTemperatures, double[] minTemperatures, int[] weatherCodes){}
}