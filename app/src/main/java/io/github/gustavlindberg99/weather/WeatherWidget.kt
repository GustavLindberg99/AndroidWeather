package io.github.gustavlindberg99.weather

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import java.util.*

private val dayViews = listOf(
    R.id.weather_widget_day_0,
    R.id.weather_widget_day_1,
    R.id.weather_widget_day_2,
    R.id.weather_widget_day_3,
    R.id.weather_widget_day_4,
    R.id.weather_widget_day_5
)

abstract class WeatherWidget : Widget(
    R.layout.widget_weather,
    R.id.weather_widget,
    R.id.weather_widget_temperature,
    R.id.weather_widget_maxmin_temperatures,
    R.id.weather_widget_background,
    0,
    0
) {
    class Colored : WeatherWidget() {
        @ColorRes
        @DrawableRes
        override fun getBackground(@DrawableRes weatherBackground: Int): Int =
            if (weatherBackground == 0) R.color.skyBlue else weatherBackground
    }

    class Transparent : WeatherWidget() {
        @ColorRes
        override fun getBackground(@DrawableRes weatherBackground: Int): Int = R.color.transparent
    }

    class SemiTransparent : WeatherWidget() {
        @ColorRes
        override fun getBackground(@DrawableRes weatherBackground: Int): Int =
            R.color.semiTransparent
    }

    override fun onResize(appWidgetId: Int, newWidth: Int, newHeight: Int) {
        val isSmall = newWidth < 300
        val isLarge = !isSmall && newHeight > 90
        this.setViewVisibility(
            appWidgetId,
            R.id.weather_widget_daily,
            if (isLarge) View.VISIBLE else View.GONE
        )
        this.setViewVisibility(
            appWidgetId,
            R.id.weather_widget_small_location_name,
            if (isSmall) View.VISIBLE else View.GONE
        )
        this.setViewVisibility(
            appWidgetId,
            R.id.weather_widget_details,
            if (isSmall) View.GONE else View.VISIBLE
        )
        this.setViewVisibility(
            appWidgetId,
            R.id.weather_widget_maxmin_temperatures,
            if (isSmall && newHeight < 75) View.GONE else View.VISIBLE
        )
        for (i in dayViews.indices) {
            this.setViewVisibility(
                appWidgetId,
                dayViews[i],
                if (newWidth > 75 * (i + 1)) View.VISIBLE else View.GONE
            )
        }
    }

    override fun setLocationName(views: RemoteViews, locationName: String) {
        views.setTextViewText(R.id.weather_widget_small_location_name, locationName)
        views.setTextViewText(R.id.weather_widget_large_location_name, locationName)
    }

    override fun setCurrentWeather(
        views: RemoteViews,
        @DrawableRes weatherImage: Int,
        weatherDescription: String
    ) {
        views.setImageViewResource(R.id.weather_widget_weather_image, weatherImage)
        views.setContentDescription(R.id.weather_widget_weather_image, weatherDescription)
        views.setTextViewText(R.id.weather_widget_weather_description, weatherDescription)
    }

    @SuppressLint("DiscouragedApi")
    override fun setDailyWeather(
        context: Context,
        views: RemoteViews,
        now: Calendar,
        maxTemperatures: List<Double>,
        minTemperatures: List<Double>,
        weatherCodes: List<Int>
    ) {
        val currentDayOfTheWeek = now[Calendar.DAY_OF_WEEK]
        val daysOfWeek = listOf(
            R.string.sunday_short,
            R.string.monday_short,
            R.string.tuesday_short,
            R.string.wednesday_short,
            R.string.thursday_short,
            R.string.friday_short,
            R.string.saturday_short
        )
        for (i in 0..5) {
            val dayOfWeek = context.getString(daysOfWeek[(currentDayOfTheWeek + i) % 7])
            val weatherCode: Int = weatherCodes[i + 1]
            val minTemperature: Double = minTemperatures[i + 1]
            val maxTemperature: Double = maxTemperatures[i + 1]
            val dayOrNight = if (weatherCode <= 2 || weatherCode / 10 == 8) "_day" else ""
            @DrawableRes val weatherImage = context.resources.getIdentifier(
                "wmo_$weatherCode$dayOrNight",
                "drawable",
                context.packageName
            )
            views.setTextViewText(
                dayViews[i],
                String.format(
                    Locale.US,
                    "%s\n%s/%s",
                    dayOfWeek,
                    Settings.UnitFormatter.temperature(context, minTemperature),
                    Settings.UnitFormatter.temperature(context, maxTemperature)
                )
            )
            views.setTextViewCompoundDrawables(dayViews[i], 0, 0, 0, weatherImage)
        }
    }
}