package io.github.gustavlindberg99.weather

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import java.text.DateFormat
import java.util.*
import kotlin.collections.HashMap

abstract class Widget(
    @LayoutRes private val _layout: Int,
    @IdRes private val _layoutId: Int,
    @IdRes private val _currentTemperature: Int,
    @IdRes private val _maxminTemperatures: Int,
    @IdRes private val _background: Int,
    @IdRes private val _clock: Int,
    @IdRes private val _date: Int
) : AppWidgetProvider(){
    private val _viewVisibilities = HashMap<Int, HashMap<Int, Int>>()
    private val _fontSizes = HashMap<Int, HashMap<Int, Int>>()

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle){
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        this.onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))    //This automatically calls onResize
    }

    override fun onReceive(context: Context, intent: Intent){
        super.onReceive(context, intent)
        if(intent.action.contentEquals("com.sec.android.widgetapp.APPWIDGET_RESIZE")){
            val appWidgetId = intent.getIntExtra("widgetId", 0)
            if(appWidgetId > 0){
                this.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(appWidgetId))    //This automatically calls onResize
            }
        }
    }

    protected open fun onResize(appWidgetId: Int, newWidth: Int, newHeight: Int){}

    protected fun setViewVisibility(appWidgetId: Int, @IdRes viewId: Int, visibility: Int){
        val visibilityMap: HashMap<Int, Int> = this._viewVisibilities[appWidgetId] ?: HashMap()
        visibilityMap[viewId] = visibility
        this._viewVisibilities[appWidgetId] = visibilityMap
    }

    protected fun setFontSize(appWidgetId: Int, @IdRes textViewId: Int, fontSize: Int){
        val fontSizeMap: HashMap<Int, Int> = this._fontSizes[appWidgetId] ?: HashMap()
        fontSizeMap[textViewId] = fontSize
        this._fontSizes[appWidgetId] = fontSizeMap
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray){
        //Send the resize event so that we know the size of the widget
        for(appWidgetId in appWidgetIds){
            val options: Bundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val widgetHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            this.onResize(appWidgetId, widgetWidth, widgetHeight)
        }

        //Update the weather
        val currentLocation: City? = City.currentLocation(context)
        val location: City = if(currentLocation != null) currentLocation else {
            val cities = City.listFromPreferences(context)
            if(cities.isEmpty()){
                return
            }
            cities[0]
        }
        updateWeather(context, appWidgetManager, appWidgetIds, location)    //Update from cache so that it shows something reasonable while waiting for the server
        location.updateWeatherFromServer({this.updateWeather(context, appWidgetManager, appWidgetIds, location)}, {})
    }

    @SuppressLint("DiscouragedApi")
    private fun updateWeather(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, location: City){
        val data: WeatherData = location.weatherData ?: return
        val now: Calendar = Calendar.getInstance()
        now.timeZone = TimeZone.getTimeZone(location.timezone)
        val dayOrNight: String = if(data.currentWeatherCode > 2 && data.currentWeatherCode / 10 != 8) "" else if(now.isDay(data.sunrises[0], data.sunsets[0], location.latitude)) "_day" else "_night"

        //There may be multiple widgets active, so update all of them
        for(appWidgetId in appWidgetIds){
            val views = RemoteViews(context.packageName, this._layout)

            //Set the onclick listener
            val weatherIntent = Intent(context, MainActivity::class.java)
            val pendingWeatherIntent: PendingIntent = PendingIntent.getActivity(context, 0, weatherIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(this._layoutId, pendingWeatherIntent)
            if(this._clock != 0){
                val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                val pendingClockIntent = PendingIntent.getActivity(context, 0, clockIntent, PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(_clock, pendingClockIntent)
            }

            //Update the weather
            setLocationName(views, location.name)
            @DrawableRes val weatherImage = context.resources.getIdentifier("wmo_" + data.currentWeatherCode + dayOrNight, "drawable", context.packageName)
            val weatherDescription: String = context.getString(context.resources.getIdentifier("wmo" + data.currentWeatherCode, "string", context.packageName))
            this.setCurrentWeather(views, weatherImage, weatherDescription)
            views.setTextViewText(this._currentTemperature, Settings.UnitFormatter.temperature(context, data.currentTemperature))
            @DrawableRes val weatherBackground = getBackgroundResource(context, data.currentWeatherCode, now, data.sunrises[0], data.sunsets[0], location.latitude)
            views.setImageViewResource(this._background, this.getBackground(weatherBackground))
            views.setTextViewText(this._maxminTemperatures, Settings.UnitFormatter.temperature(context, data.minTemperature[0]) + "/" + Settings.UnitFormatter.temperature(context, data.maxTemperature[0]))
            this.setDailyWeather(context, views, data.now, data.maxTemperature, data.minTemperature, data.dailyWeatherCode)
            views.setTextViewText(this._date, DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Calendar.getInstance().timeInMillis))

            //Set the visibilities of the views
            val viewVisibilities = this._viewVisibilities[appWidgetId]
            if(viewVisibilities != null){
                for((key, value) in viewVisibilities){
                    views.setViewVisibility(key, value)
                }
            }

            //Set the font sizes of the text views
            val fontSizes = this._fontSizes[appWidgetId]
            if(fontSizes != null){
                for((key, value) in fontSizes){
                    views.setTextViewTextSize(key, TypedValue.COMPLEX_UNIT_SP, value.toFloat())
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    @ColorRes
    @DrawableRes
    protected abstract fun getBackground(@DrawableRes weatherBackground: Int): Int
    protected abstract fun setLocationName(views: RemoteViews, locationName: String)
    protected abstract fun setCurrentWeather(views: RemoteViews, @DrawableRes weatherImage: Int, weatherDescription: String)
    protected abstract fun setDailyWeather(context: Context, views: RemoteViews, now: Calendar, maxTemperatures: List<Double>, minTemperatures: List<Double>, weatherCodes: List<Int>)
}