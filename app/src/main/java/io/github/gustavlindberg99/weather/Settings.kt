package io.github.gustavlindberg99.weather

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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

        const val CELSIUS = 0
        const val FAHRENHEIT = 1
        const val KMH = 0
        const val MS = 1
        const val MPH = 2
        const val MM = 0
        const val INCHES = 1

        fun temperatureUnit(context: Context): Int {
            return preferences(context).getInt(TEMPERATURE_UNIT, CELSIUS)
        }

        fun windSpeedUnit(context: Context): Int {
            return preferences(context).getInt(WIND_SPEED_UNIT, KMH)
        }

        fun precipitationUnit(context: Context): Int {
            return preferences(context).getInt(PRECIPITATION_UNIT, MM)
        }
    }

    object UnitFormatter{
        fun temperature(context: Context, t: Double): String {
            return when(Preference.temperatureUnit(context)) {
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

        fun uvIndex(context: Context, wm2: Double): String {
            val index = (wm2 / 140.0).roundToInt()
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
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_settings)

        //Initialize temperature unit buttons
        val celsius: RadioButton = this.findViewById(R.id.celsius)
        val fahrenheit: RadioButton = this.findViewById(R.id.fahrenheit)
        when(Preference.temperatureUnit(this)){
            Preference.FAHRENHEIT -> fahrenheit.isChecked = true
            else -> celsius.isChecked = true
        }
        celsius.setOnClickListener {preferences(this).edit().putInt(Preference.TEMPERATURE_UNIT, Preference.CELSIUS).apply()}
        fahrenheit.setOnClickListener {preferences(this).edit().putInt(Preference.TEMPERATURE_UNIT, Preference.FAHRENHEIT).apply()}

        //Initialize wind speed unit buttons
        val kmh: RadioButton = this.findViewById(R.id.kmh)
        val ms: RadioButton = this.findViewById(R.id.ms)
        val mph: RadioButton = this.findViewById(R.id.mph)
        when(Preference.windSpeedUnit(this)){
            Preference.MS -> ms.isChecked = true
            Preference.MPH -> mph.isChecked = true
            else -> kmh.isChecked = true
        }
        kmh.setOnClickListener {preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.KMH).apply()}
        ms.setOnClickListener {preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.MS).apply()}
        mph.setOnClickListener {preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.MPH).apply()}

        //Initialize precipitation unit buttons
        val mm: RadioButton = this.findViewById(R.id.mm)
        val inches: RadioButton = this.findViewById(R.id.inches)
        when(Preference.precipitationUnit(this)){
            Preference.INCHES -> inches.isChecked = true
            else -> mm.isChecked = true
        }
        mm.setOnClickListener {preferences(this).edit().putInt(Preference.PRECIPITATION_UNIT, Preference.MM).apply()}
        inches.setOnClickListener {preferences(this).edit().putInt(Preference.PRECIPITATION_UNIT, Preference.INCHES).apply()}
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
            if(this.theme.resolveAttribute(R.attr.dialogPreferredPadding, value, true)){
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