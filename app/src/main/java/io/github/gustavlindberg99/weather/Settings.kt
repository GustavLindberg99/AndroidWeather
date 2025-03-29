package io.github.gustavlindberg99.weather

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

private fun preferences(context: Context): SharedPreferences {
    return context.getSharedPreferences("preferences", AppCompatActivity.MODE_PRIVATE)
}

class Settings: AppCompatActivity(){
    private object Preference{
        const val TEMPERATURE_UNIT = "temperatureUnit"
        const val WIND_SPEED_UNIT = "windSpeedUnit"
        const val PRECIPITATION_UNIT = "precipitationUnit"
        const val AIR_QUALITY_INDEX = "airQualityIndex"

        const val CELSIUS = 0
        const val FAHRENHEIT = 1
        const val KMH = 0
        const val MS = 1
        const val MPH = 2
        const val MM = 0
        const val INCHES = 1
        const val AMERICAN_AQI = 0
        const val EUROPEAN_AQI = 1

        fun temperatureUnit(context: Context): Int {
            return preferences(context).getInt(TEMPERATURE_UNIT, CELSIUS)
        }

        fun windSpeedUnit(context: Context): Int {
            return preferences(context).getInt(WIND_SPEED_UNIT, KMH)
        }

        fun precipitationUnit(context: Context): Int {
            return preferences(context).getInt(PRECIPITATION_UNIT, MM)
        }

        fun airQualityIndex(context: Context): Int {
            return preferences(context).getInt(AIR_QUALITY_INDEX, AMERICAN_AQI)
        }
    }

    object UnitFormatter{
        fun temperature(context: Context, t: Double): String {
            return when(Preference.temperatureUnit(context)){
                Preference.FAHRENHEIT -> (t * 1.8 + 32.0).roundToInt().toString() + "°F"
                Preference.CELSIUS -> t.roundToInt().toString() + "°C"
                else -> t.roundToInt().toString() + "°C"
            }
        }

        fun windSpeed(context: Context, s: Double): String {
            return when(Preference.windSpeedUnit(context)){
                Preference.MS -> ceil(s / 3.6).toInt().toString() + " m/s"
                Preference.MPH -> ceil(s * 0.62137).toInt().toString() + " mph"
                Preference.KMH -> ceil(s).toInt().toString() + " km/h"
                else -> ceil(s).toInt().toString() + " km/h"
            }
        }

        fun percentage(p: Int): String {
            return "$p%"
        }

        fun precipitation(context: Context, p: Double): String {
            return when(Preference.precipitationUnit(context)){
                Preference.INCHES -> (p * 0.039370).toString() + " in"
                Preference.MM -> "$p mm"
                else -> "$p mm"
            }
        }

        fun pressure(p: Double): String {
            return "$p hPa"
        }

        fun uvIndex(context: Context, index: Int): String {
            if(index == 0) {
                return "0"
            }
            @StringRes val descriptionId = if(index <= 2) R.string.low
            else if(index <= 5) R.string.moderate
            else if(index <= 7) R.string.high
            else if(index <= 10) R.string.veryHigh
            else R.string.extreme
            return String.format(Locale.US, "%d (%s)", index, context.getString(descriptionId))
        }

        fun time(context: Context, hour: Int, minute: Int? = null): String {
            if(DateFormat.is24HourFormat(context)){
                return String.format(Locale.US, if(minute == null) "%02d" else "%02d.%02d", hour % 24, minute)
            }
            val amOrPm = if(hour % 24 < 12) "AM" else "PM"
            val hour12 = if(hour % 12 == 0) 12 else hour % 12
            val timeAsString = if(minute == null) hour12.toString() else String.format(Locale.US, "%d.%02d", hour12, minute)
            return "$timeAsString $amOrPm"
        }

        fun airQualityIndex(context: Context, americanAqi: Int, europeanAqi: Int): String {
            if(Preference.airQualityIndex(context) == Preference.EUROPEAN_AQI){
                @StringRes val descriptionId = if(europeanAqi < 20) R.string.good
                else if(europeanAqi < 40) R.string.fair
                else if(europeanAqi < 60) R.string.moderate
                else if(europeanAqi < 80) R.string.poor
                else if(europeanAqi < 100) R.string.veryPoor
                else R.string.extremelyPoor
                return String.format(Locale.US, "%d (%s)", europeanAqi, context.getString(descriptionId))
            }
            @StringRes val descriptionId = if(americanAqi < 50) R.string.good
            else if(americanAqi < 100) R.string.moderate
            else if(americanAqi < 150) R.string.unhealthyForSensitiveGroups
            else if(americanAqi < 200) R.string.unhealthy
            else if(americanAqi < 300) R.string.veryUnhealthy
            else R.string.hazardous
            return String.format(Locale.US, "%d (%s)", americanAqi, context.getString(descriptionId))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_settings)

        //Initialize temperature unit buttons
        val celsius: RadioButton = this.findViewById(R.id.celsius)
        val fahrenheit: RadioButton = this.findViewById(R.id.fahrenheit)
        when(Preference.temperatureUnit(this)){
            Preference.CELSIUS -> celsius.isChecked = true
            Preference.FAHRENHEIT -> fahrenheit.isChecked = true
        }
        celsius.setOnClickListener{preferences(this).edit().putInt(Preference.TEMPERATURE_UNIT, Preference.CELSIUS).apply()}
        fahrenheit.setOnClickListener{preferences(this).edit().putInt(Preference.TEMPERATURE_UNIT, Preference.FAHRENHEIT).apply()}

        //Initialize wind speed unit buttons
        val kmh: RadioButton = this.findViewById(R.id.kmh)
        val ms: RadioButton = this.findViewById(R.id.ms)
        val mph: RadioButton = this.findViewById(R.id.mph)
        when(Preference.windSpeedUnit(this)){
            Preference.KMH -> kmh.isChecked = true
            Preference.MS -> ms.isChecked = true
            Preference.MPH -> mph.isChecked = true
        }
        kmh.setOnClickListener{preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.KMH).apply()}
        ms.setOnClickListener{preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.MS).apply()}
        mph.setOnClickListener{preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.MPH).apply()}

        //Initialize precipitation unit buttons
        val mm: RadioButton = this.findViewById(R.id.mm)
        val inches: RadioButton = this.findViewById(R.id.inches)
        when(Preference.precipitationUnit(this)){
            Preference.MM -> mm.isChecked = true
            Preference.INCHES -> inches.isChecked = true
        }
        mm.setOnClickListener{preferences(this).edit().putInt(Preference.PRECIPITATION_UNIT, Preference.MM).apply()}
        inches.setOnClickListener{preferences(this).edit().putInt(Preference.PRECIPITATION_UNIT, Preference.INCHES).apply()}

        //Initialize air quality index buttons
        val americanAqi: RadioButton = this.findViewById(R.id.americanAqi)
        val europeanAqi: RadioButton = this.findViewById(R.id.europeanAqi)
        when(Preference.airQualityIndex(this)){
            Preference.AMERICAN_AQI -> americanAqi.isChecked = true
            Preference.EUROPEAN_AQI -> europeanAqi.isChecked = true
        }
        americanAqi.setOnClickListener{preferences(this).edit().putInt(Preference.AIR_QUALITY_INDEX, Preference.AMERICAN_AQI).apply()}
        europeanAqi.setOnClickListener{preferences(this).edit().putInt(Preference.AIR_QUALITY_INDEX, Preference.EUROPEAN_AQI).apply()}

        //Initialize help, feedback and about buttons
        this.findViewById<Button>(R.id.help_button).setOnClickListener{
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/GustavLindberg99/AndroidWeather/blob/master/README.md"))
            startActivity(browserIntent)
        }
        this.findViewById<Button>(R.id.feedback_button).setOnClickListener{
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/GustavLindberg99/AndroidWeather/issues"))
            startActivity(browserIntent)
        }
        this.findViewById<Button>(R.id.about_button).setOnClickListener{
            val textView = TextView(this)
            textView.text = HtmlCompat.fromHtml(String.format(this.getString(R.string.about_string), BuildConfig.VERSION_NAME, "https://github.com/GustavLindberg99/AndroidWeather", "https://open-meteo.com/", "https://github.com/GustavLindberg99/AndroidWeather/blob/master/LICENSE"), HtmlCompat.FROM_HTML_MODE_LEGACY)
            textView.setTextColor(Color.BLACK)
            textView.setLinkTextColor(Color.BLUE)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            val value = TypedValue()
            if(this.theme.resolveAttribute(androidx.appcompat.R.attr.dialogPreferredPadding, value, true)){
                val padding = TypedValue.complexToDimensionPixelSize(value.data, this.resources.displayMetrics)
                textView.setPadding(padding, dpToPx(8.0), padding, 0)
            }
            textView.movementMethod = LinkMovementMethod.getInstance()
            AlertDialog.Builder(this)
                    .setTitle(R.string.about)
                    .setView(textView)
                    .setPositiveButton(R.string.ok, {_: DialogInterface?, _: Int ->})
                    .create()
                    .show()
        }
    }
}