package io.github.gustavlindberg99.weather

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import java.util.*
import kotlin.math.roundToInt

class WeatherFragment: Fragment(){
    //Size 7
    private val _dailySummaries: List<TableRow> get() = listOf(
        this._fragmentView.findViewById(R.id.day_0_summary),
        this._fragmentView.findViewById(R.id.day_1_summary),
        this._fragmentView.findViewById(R.id.day_2_summary),
        this._fragmentView.findViewById(R.id.day_3_summary),
        this._fragmentView.findViewById(R.id.day_4_summary),
        this._fragmentView.findViewById(R.id.day_5_summary),
        this._fragmentView.findViewById(R.id.day_6_summary)
    )
    private val _dayNames: List<Button> get() = this._dailySummaries.map{it.findViewById(R.id.name)}
    private val _dayWeathers: List<ImageView> get() = this._dailySummaries.map{it.findViewById(R.id.weather)}
    private val _dayTemperatures: List<TextView> get() = this._dailySummaries.map{it.findViewById(R.id.temperature)}

    private val _hourlyWeatherBars: MutableList<LinearLayout?> = mutableListOf(null, null, null, null, null, null, null)
    private val _selectedHours: MutableList<Int> = mutableListOf(-1, -1, -1, -1, -1, -1, -1)
    private val _sunriseViews: MutableList<Button?> = mutableListOf(null, null, null, null, null, null, null)
    private val _sunsetViews: MutableList<Button?> = mutableListOf(null, null, null, null, null, null, null)
    private val _sunriseBorders: MutableList<View?> = mutableListOf(null, null, null, null, null, null, null)
    private val _sunsetBorders: MutableList<View?> = mutableListOf(null, null, null, null, null, null, null)

    //Size 7x24
    private val _hours: List<MutableList<Button>> = (1..7).map{mutableListOf()}
    private val _hourBorders: List<MutableList<View>> = (1..7).map{mutableListOf()}

    private lateinit var _fragmentView: View
    private val _dayList: TableLayout get() = this._fragmentView.findViewById(R.id.day_list)
    private val _backgroundImage: ImageView get() = this.requireActivity().findViewById(R.id.background_image)
    private val _backgroundGradient: ImageView get() = this.requireActivity().findViewById(R.id.background_gradient)
    private val _backgroundColor: ImageView get() = this.requireActivity().findViewById(R.id.background_color)
    private val _locationView: TextView get() = this._fragmentView.findViewById(R.id.location)
    private val _currentSunrise: TextView get() = this._fragmentView.findViewById(R.id.current_sunrise)
    private val _currentSunset: TextView get() = this._fragmentView.findViewById(R.id.current_sunset)
    private val _currentTemperature: TextView get() = this._fragmentView.findViewById(R.id.current_temperature)
    private val _currentWeather: TextView get() = this._fragmentView.findViewById(R.id.current_weather)
    private val _currentWind: TextView get() = this._fragmentView.findViewById(R.id.current_wind)
    private val _currentHumidity: TextView get() = this._fragmentView.findViewById(R.id.current_humidity)
    private val _currentApparentTemperature: TextView get() = this._fragmentView.findViewById(R.id.current_apparent_temperature)
    private val _currentPrecipitation: TextView get() = this._fragmentView.findViewById(R.id.current_precipitation)
    private val _currentPressure: TextView get() = this._fragmentView.findViewById(R.id.current_pressure)
    private val _currentUVIndex: TextView get() = this._fragmentView.findViewById(R.id.current_uv_index)
    private val _currentCloudCover: TextView get() = this._fragmentView.findViewById(R.id.current_cloud_cover)
    private val _currentDewPoint: TextView get() = this._fragmentView.findViewById(R.id.current_dew_point)
    private val _currentPrecipitationProbability: TextView get() = this._fragmentView.findViewById(R.id.current_precipitation_probability)
    private val _currentAirQualityIndex: TextView get() = this._fragmentView.findViewById(R.id.current_air_quality_index)
    private lateinit var _city: City
    private var _showErrorMessage: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        this._city = City.fromBundle(this.requireActivity(), this.arguments)!!    //This can only be null if the bundle that was sent is invalid, which shouldn't happen unless there is a bug
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.fragment_weather, container, false)
        this._fragmentView = fragmentView

        //Initialize the variables that point to views
        for(i in 0..6){
            this._dayNames[i].setOnClickListener{
                val hourlyWeatherBar = this._hourlyWeatherBars[i]
                if(hourlyWeatherBar != null){
                    hourlyWeatherBar.visibility = if(hourlyWeatherBar.visibility == View.GONE) View.VISIBLE else View.GONE
                }
                this.createHourlyWeatherBar(i)
            }
        }
        this.createHourlyWeatherBar(0)    //This automatically calls refreshFromCache (which is needed because onResume won't be called until the fragment is fully visible, so without this it would say unknown location while scrolling to the next one)
        return fragmentView
    }

    private fun createHourlyWeatherBar(day: Int){    //Create these dynamically for performance reasons
        if(this._hourlyWeatherBars[day] == null || this._dayList.indexOfChild(this._hourlyWeatherBars[day]) == -1){
            val hourlyWeatherBar: LinearLayout = View.inflate(this.requireActivity(), R.layout.hourly_weather_bar, null) as LinearLayout
            hourlyWeatherBar.layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
            this._hours[day].clear()
            this._hourBorders[day].clear()
            for(i in 0..23){
                this._hours[day].add(hourlyWeatherBar.findViewById<TableRow>(R.id.daily_weather_bar).getChildAt(i) as Button)
                this._hourBorders[day].add(hourlyWeatherBar.findViewById<TableRow>(R.id.hourly_borders).getChildAt(i))
            }
            val index = this._dayList.indexOfChild(_dailySummaries[day])
            this._dayList.addView(hourlyWeatherBar, index + 1)
            this._hourlyWeatherBars[day] = hourlyWeatherBar
        }
        this.refreshFromCache()    //To show the weather in the views we just created
    }

    override fun onResume(){
        super.onResume()
        this._showErrorMessage = true
        this._city = this._city.fromUpdatedList()
        this.createHourlyWeatherBar(0)    //This is needed because sometimes the hourly weather bar is deleted while the fragment is invisible
        //refreshFromServer is automatically called in createHourlyWeatherBar
        this.refreshFromServer()
    }

    override fun onConfigurationChanged(newConfig: Configuration){
        super.onConfigurationChanged(newConfig)
        this.refreshFromCache()
    }

    fun setLocation(city: City){
        this._city = city
        this.updateWeather(city.weatherData)
    }

    private fun refreshFromServer(){
        this._city.updateWeatherFromServer({this.setLocation(this._city)}, {
            if(this._showErrorMessage && this.activity != null) {
                Toast.makeText(this.requireActivity(), R.string.noInternetConnection, Toast.LENGTH_LONG).show()
                this._showErrorMessage = false
            }
        })
    }

    fun refreshFromCache(){
        this.updateWeather(this._city.weatherData)
    }

    @SuppressLint("SetTextI18n", "DiscouragedApi")
    private fun updateWeather(data: WeatherData?){
        if(this.activity == null){
            return
        }
        this._locationView.text = this._city.name
        if(this._city.isCurrentLocation){
            this._locationView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.position, 0, 0, 0)
        }
        if(data == null) {
            this._backgroundImage.setImageResource(R.color.skyBlue)
            @ColorInt val color = requireActivity().getColor(R.color.skyBlue)
            this._backgroundGradient.setColorFilter(color)
            this._backgroundColor.setColorFilter(color)
            return
        }
        val now: Calendar = data.now
        val currentHour: Int = now[Calendar.HOUR_OF_DAY]

        //Update the labels with the days of the week
        val dayOfTheWeek: Int = now[Calendar.DAY_OF_WEEK]
        val daysOfWeek = listOf(
            R.string.sunday,
            R.string.monday,
            R.string.tuesday,
            R.string.wednesday,
            R.string.thursday,
            R.string.friday,
            R.string.saturday
        )
        for(i in 1..6) {
            this._dayNames[i].setText(daysOfWeek[(dayOfTheWeek + i - 1) % 7])
        }

        //Current weather
        @StringRes val currentWeatherStringId = this.resources.getIdentifier("wmo" + data.currentWeatherCode, "string", this.requireActivity().packageName)
        val currentWeatherString = if(currentWeatherStringId == 0) "--" else this.getString(currentWeatherStringId)
        val currentWindDirectionStringId = this.resources.getIdentifier("wind" + (data.currentWindDirection / 22.5 + 1).roundToInt() % 16, "string", this.requireActivity().packageName)
        val currentWindString = if(data.currentWindSpeed == 0.0) Settings.UnitFormatter.windSpeed(this.requireActivity(), data.currentWindSpeed) else String.format(Locale.US, "%s %s", this.getString(currentWindDirectionStringId), Settings.UnitFormatter.windSpeed(this.requireActivity(), data.currentWindSpeed))
        val firstSunrise: Calendar? = data.sunrises[0]
        val firstSunset: Calendar? = data.sunsets[0]
        this._currentSunrise.text = if(firstSunrise == null) "--" else Settings.UnitFormatter.time(this.requireActivity(), firstSunrise[Calendar.HOUR_OF_DAY], firstSunrise[Calendar.MINUTE])
        this._currentSunset.text = if(firstSunset == null) "--" else Settings.UnitFormatter.time(this.requireActivity(), firstSunset[Calendar.HOUR_OF_DAY], firstSunset[Calendar.MINUTE])
        this._currentTemperature.text = Settings.UnitFormatter.temperature(this.requireActivity(), data.currentTemperature)
        this._currentWeather.text = currentWeatherString
        this._currentWind.text = currentWindString
        this._currentHumidity.text = Settings.UnitFormatter.percentage(data.currentHumidity)
        this._currentApparentTemperature.text = Settings.UnitFormatter.temperature(this.requireActivity(), data.currentApparentTemperature)
        this._currentPrecipitation.text = Settings.UnitFormatter.precipitation(this.requireActivity(), data.currentPrecipitation)
        this._currentPressure.text = Settings.UnitFormatter.pressure(data.currentPressure)
        this._currentUVIndex.text = Settings.UnitFormatter.uvIndex(this.requireActivity(), data.currentUvIndex)
        this._currentCloudCover.text = Settings.UnitFormatter.percentage(data.currentCloudCover)
        this._currentDewPoint.text = Settings.UnitFormatter.temperature(this.requireActivity(), data.currentDewPoint)
        this._currentPrecipitationProbability.text = Settings.UnitFormatter.percentage(data.currentPrecipitationProbability)
        this._currentAirQualityIndex.text = Settings.UnitFormatter.airQualityIndex(this.requireActivity(), data.currentAmericanAqi, data.currentEuropeanAqi)

        //Update the background (only if this fragment is the one that's currently visible)
        if(this._city == (this.requireActivity() as MainActivity).selectedCity()){
            @DrawableRes val backgroundImage = getBackgroundResource(this.requireActivity(), data)
            if(backgroundImage == 0){
                this._backgroundImage.setImageResource(R.color.skyBlue)
                @ColorInt val color = this.requireActivity().getColor(R.color.skyBlue)
                this._backgroundGradient.setColorFilter(color)
                this._backgroundColor.setColorFilter(color)
            }
            else{
                this._backgroundImage.setImageResource(backgroundImage)
                val backgroundDrawable: Drawable = this._backgroundImage.drawable
                if(backgroundDrawable is AnimationDrawable){
                    backgroundDrawable.start()
                }
                @ColorInt val medianColor = medianColor(this.requireActivity(), backgroundImage)    //Use the median instead of the average, otherwise the stars in the clear night background will cause the average color to be very bright
                this._backgroundGradient.setColorFilter(medianColor)
                this._backgroundColor.setColorFilter(medianColor)
            }
        }

        //Hourly
        for(day in 0..6){
            val hourlyWeatherBar = this._hourlyWeatherBars[day] ?: continue
            val hourlyDetailsBar: TableLayout = hourlyWeatherBar.findViewById(R.id.hourly_details)
            for(hour in 0..23){
                val hourOfDay: Int = (if(day == 0) currentHour + 1 else 0) + hour    //Hour of day, can be greater than 24 if the view is for an hour tomorrow showing in the bar for today
                val i: Int = day * 24 + hourOfDay     //Index of the current hour in the weekly length-168 arrays

                //Hours in weather bar
                this._hours[day][hour].text = String.format(Locale.US, "%s\n%s", Settings.UnitFormatter.time(this.requireActivity(), hourOfDay), Settings.UnitFormatter.temperature(this.requireActivity(), data.hourlyTemperature[i]))
                this._hours[day][hour].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, this.resources.getIdentifier("wmo_" + data.hourlyWeatherCode[i] + data.hourlyDayOrNight(i), "drawable", requireActivity().packageName))

                //Hourly details bar
                val windDirectionStringId = this.resources.getIdentifier("wind" + (data.hourlyWindDirection[i] / 22.5 + 1).roundToInt() % 16, "string", requireActivity().packageName)
                val windSpeed = data.hourlyWindSpeed[i]
                val windString = if(windSpeed == 0.0) Settings.UnitFormatter.windSpeed(requireActivity(), windSpeed) else String.format(Locale.US, "%s %s", this.getString(windDirectionStringId), Settings.UnitFormatter.windSpeed(requireActivity(), windSpeed))
                val precipitationProbability = Settings.UnitFormatter.percentage(data.hourlyPrecipitationProbability[i])
                val uvIndex = Settings.UnitFormatter.uvIndex(requireActivity(), data.hourlyUvIndex[i])
                val windView: TextView = hourlyDetailsBar.findViewById(R.id.wind)
                val precipitationProbabilityView: TextView = hourlyDetailsBar.findViewById(R.id.precipitation_probability)
                val uvIndexView: TextView = hourlyDetailsBar.findViewById(R.id.uv_index)
                if(this._selectedHours[day] == hour){
                    windView.text = windString
                    precipitationProbabilityView.text = precipitationProbability
                    uvIndexView.text = uvIndex
                }
                this._hours[day][hour].setOnClickListener{
                    if(this._selectedHours[day] != -1){
                        this._hourBorders[day][this._selectedHours[day]].visibility = View.VISIBLE
                    }
                    if(this._selectedHours[day] == hour){
                        this._selectedHours[day] = -1
                        hourlyDetailsBar.visibility = View.GONE
                    }
                    else{
                        this._selectedHours[day] = hour
                        hourlyDetailsBar.visibility = View.VISIBLE
                        this._hourBorders[day][hour].visibility = View.INVISIBLE
                        windView.text = windString
                        precipitationProbabilityView.text = precipitationProbability
                        uvIndexView.text = uvIndex
                    }
                }
            }

            //Sunrise and sunset
            var sunrise: Calendar? = data.sunrises[day]
            var sunset: Calendar? = data.sunsets[day]
            val hourlyWeatherBarLayout: TableRow = hourlyWeatherBar.findViewById(R.id.daily_weather_bar)
            val borders: TableRow = hourlyWeatherBar.findViewById(R.id.hourly_borders)
            if(this._sunriseViews[day] != null){
                hourlyWeatherBarLayout.removeView(this._sunriseViews[day])
                this._sunriseViews[day] = null
                borders.removeView(this._sunriseBorders[day])
                this._sunriseBorders[day] = null
            }
            if(this._sunsetViews[day] != null){
                hourlyWeatherBarLayout.removeView(this._sunsetViews[day])
                this._sunsetViews[day] = null
                borders.removeView(this._sunsetBorders[day])
                this._sunsetBorders[day] = null
            }
            if(sunrise != null){
                var offset = 0
                if(day == 0){
                    if(sunrise < now){
                        sunrise = data.sunrises[1]
                        offset = 23 - currentHour
                    }
                    else{
                        offset = -1 - currentHour
                    }
                }
                if(sunrise != null){
                    val sunriseView = Button(requireActivity(), null, R.attr.buttonBarButtonStyle)
                    this._sunriseViews[day] = sunriseView
                    sunriseView.isEnabled = false
                    sunriseView.setTextColor(this.requireActivity().getColor(R.color.white))
                    sunriseView.text = Settings.UnitFormatter.time(this.requireActivity(), sunrise[Calendar.HOUR_OF_DAY], sunrise[Calendar.MINUTE]) + "\n" + this.getString(R.string.sunrise)
                    sunriseView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.sunrise)
                    val index: Int = sunrise[Calendar.HOUR_OF_DAY] + offset + 1
                    hourlyWeatherBarLayout.addView(sunriseView, index)

                    val sunriseBorder = View(requireActivity())
                    this._sunriseBorders[day] = sunriseBorder
                    sunriseBorder.setBackgroundColor(requireActivity().getColor(R.color.white))
                    sunriseBorder.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, dpToPx(0.5))
                    borders.addView(sunriseBorder, index)
                }
            }
            if(sunset != null){
                var offset = 0
                if(day == 0){
                    if(sunset < now){
                        sunset = data.sunsets[1]
                        offset = 23 - currentHour
                    }
                    else{
                        offset = -1 - currentHour
                    }
                }
                if(sunset != null){
                    val sunsetView = Button(requireActivity(), null, R.attr.buttonBarButtonStyle)
                    this._sunsetViews[day] = sunsetView
                    sunsetView.isEnabled = false
                    sunsetView.setTextColor(this.requireActivity().getColor(R.color.white))
                    sunsetView.text = Settings.UnitFormatter.time(this.requireActivity(), sunset[Calendar.HOUR_OF_DAY], sunset[Calendar.MINUTE]) + "\n" + this.getString(R.string.sunset)
                    sunsetView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.sunset)
                    val index: Int = sunset[Calendar.HOUR_OF_DAY] + offset + 1 + if(sunrise != null && sunrise < sunset) 1 else 0
                    hourlyWeatherBarLayout.addView(sunsetView, index)

                    val sunsetBorder = View(requireActivity())
                    this._sunsetBorders[day] = sunsetBorder
                    sunsetBorder.setBackgroundColor(requireActivity().getColor(R.color.white))
                    sunsetBorder.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, dpToPx(0.5))
                    borders.addView(sunsetBorder, index)
                }
            }
        }

        //Daily
        for(i in 0..6){
            val dayOrNight: String = data.dailyDayOrNight(i, if(i == 0) currentHour else 0)
            this._dayTemperatures[i].text = String.format(Locale.US, "%s/%s", Settings.UnitFormatter.temperature(requireActivity(), data.minTemperature[i]), Settings.UnitFormatter.temperature(this.requireActivity(), data.maxTemperature[i]))
            this._dayWeathers[i].setImageResource(this.resources.getIdentifier("wmo_" + data.dailyWeatherCode[i] + dayOrNight, "drawable", this.requireActivity().packageName))
            this._dayWeathers[i].contentDescription = this.getString(this.resources.getIdentifier("wmo" + data.dailyWeatherCode[i], "string", this.requireActivity().packageName))
        }
    }

    companion object {
        fun getInstance(city: City): WeatherFragment {
            val toReturn = WeatherFragment()
            toReturn.arguments = city.toBundle()
            return toReturn
        }
    }
}