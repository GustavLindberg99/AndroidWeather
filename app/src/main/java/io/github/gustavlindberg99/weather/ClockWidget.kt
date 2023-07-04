package io.github.gustavlindberg99.weather

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import java.util.*

class ClockWidget : Widget(R.layout.widget_clock, R.id.clock_widget_weather, R.id.clock_widget_weather, 0, 0, R.id.clock_widget_clock, R.id.clock_widget_date){
    override fun onResize(appWidgetId: Int, newWidth: Int, newHeight: Int){
        val fontSize = if(newWidth < 300 || newHeight < 60) 32 else 48
        setFontSize(appWidgetId, R.id.clock_widget_weather, fontSize)
        setFontSize(appWidgetId, R.id.clock_widget_clock, fontSize)
    }

    @ColorRes
    override fun getBackground(@DrawableRes weatherBackground: Int): Int {
        return R.color.transparent
    }

    override fun setLocationName(views: RemoteViews, locationName: String){
        views.setTextViewText(R.id.clock_widget_location, locationName)
    }

    override fun setCurrentWeather(views: RemoteViews, @DrawableRes weatherImage: Int, weatherDescription: String){
        views.setTextViewCompoundDrawables(R.id.clock_widget_weather, weatherImage, 0, 0, 0)
    }

    override fun setDailyWeather(context: Context, views: RemoteViews, now: Calendar, maxTemperatures: List<Double>, minTemperatures: List<Double>, weatherCodes: List<Int>){}
}