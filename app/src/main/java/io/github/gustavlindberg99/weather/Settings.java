package io.github.gustavlindberg99.weather;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import java.util.Locale;

public class Settings extends AppCompatActivity{
    private static abstract class Preference{
        public static final String TEMPERATURE_UNIT = "temperatureUnit";
        public static final String WIND_SPEED_UNIT = "windSpeedUnit";
        public static final String PRECIPITATION_UNIT = "precipitationUnit";

        public static final int CELSIUS = 0, FAHRENHEIT = 1;
        public static final int KMH = 0, MS = 1, MPH = 2;
        public static final int MM = 0, INCHES = 1;

        private static int temperatureUnit(Context context){
            return preferences(context).getInt(Preference.TEMPERATURE_UNIT, Preference.CELSIUS);
        }

        private static int windSpeedUnit(Context context){
            return preferences(context).getInt(Preference.WIND_SPEED_UNIT, Preference.KMH);
        }

        private static int precipitationUnit(Context context){
            return preferences(context).getInt(Preference.PRECIPITATION_UNIT, Preference.MM);
        }
    }

    private static SharedPreferences preferences(Context context){
        return context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    }

    public static abstract class UnitFormatter{
        public static String temperature(Context context, double t){
            switch(Preference.temperatureUnit(context)){
            case Preference.FAHRENHEIT:
                return Math.round(t * 1.8 + 32.0) + "°F";
            case Preference.CELSIUS:
            default:
                return Math.round(t) + "°C";
            }
        }

        public static String windSpeed(Context context, double s){
            switch(Preference.windSpeedUnit(context)){
            case Preference.MS:
                return (int) Math.ceil(s / 3.6) + " m/s";
            case Preference.MPH:
                return (int) Math.ceil(s * 0.62137) + " mph";
            case Preference.KMH:
            default:
                return (int) Math.ceil(s) + " km/h";
            }
        }

        public static String percentage(int p){
            return p + "%";
        }

        public static String precipitation(Context context, double p){
            switch(Preference.precipitationUnit(context)){
            case Preference.INCHES:
                return (p * 0.039370) + " in";
            case Preference.MM:
            default:
                return p + " mm";
            }
        }

        public static String pressure(double p){
            return p + " hPa";
        }

        public static String uvIndex(Context context, double wm2){
            final long index = Math.round(wm2 / 140.0);
            if(index == 0){
                return "0";
            }
            @StringRes int descriptionId;
            if(index <= 2){
                descriptionId = R.string.low;
            }
            else if(index <= 5){
                descriptionId = R.string.moderate;
            }
            else if(index <= 7){
                descriptionId = R.string.high;
            }
            else if(index <= 10){
                descriptionId = R.string.veryHigh;
            }
            else{
                descriptionId = R.string.extreme;
            }
            return String.format(Locale.US, "%d (%s)", index, context.getString(descriptionId));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Initialize temperature unit buttons
        RadioButton celsius = this.findViewById(R.id.celsius);
        RadioButton fahrenheit = this.findViewById(R.id.fahrenheit);
        switch(Preference.temperatureUnit(this)){
        case Preference.FAHRENHEIT:
            fahrenheit.setChecked(true);
            break;
        case Preference.CELSIUS:
        default:
            celsius.setChecked(true);
            break;
        }
        celsius.setOnClickListener((View v) -> preferences(this).edit().putInt(Preference.TEMPERATURE_UNIT, Preference.CELSIUS).apply());
        fahrenheit.setOnClickListener((View v) -> preferences(this).edit().putInt(Preference.TEMPERATURE_UNIT, Preference.FAHRENHEIT).apply());

        //Initialize wind speed unit buttons
        RadioButton kmh = this.findViewById(R.id.kmh);
        RadioButton ms = this.findViewById(R.id.ms);
        RadioButton mph = this.findViewById(R.id.mph);
        switch(Preference.windSpeedUnit(this)){
        case Preference.MS:
            ms.setChecked(true);
            break;
        case Preference.MPH:
            mph.setChecked(true);
            break;
        case Preference.KMH:
        default:
            kmh.setChecked(true);
            break;
        }
        kmh.setOnClickListener((View v) -> preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.KMH).apply());
        ms.setOnClickListener((View v) -> preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.MS).apply());
        mph.setOnClickListener((View v) -> preferences(this).edit().putInt(Preference.WIND_SPEED_UNIT, Preference.MPH).apply());

        //Initialize precipitation unit buttons
        RadioButton mm = this.findViewById(R.id.mm);
        RadioButton inches = this.findViewById(R.id.inches);
        switch(Preference.precipitationUnit(this)){
        case Preference.INCHES:
            inches.setChecked(true);
            break;
        case Preference.MM:
        default:
            mm.setChecked(true);
            break;
        }
        mm.setOnClickListener((View v) -> preferences(this).edit().putInt(Preference.PRECIPITATION_UNIT, Preference.MM).apply());
        inches.setOnClickListener((View v) -> preferences(this).edit().putInt(Preference.PRECIPITATION_UNIT, Preference.INCHES).apply());

        this.findViewById(R.id.help_button).setOnClickListener((View v) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/GustavLindberg99/AndroidWeather/blob/main/README.md"));
            startActivity(browserIntent);
        });

        this.findViewById(R.id.feedback_button).setOnClickListener((View v) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/GustavLindberg99/AndroidWeather/issues"));
            startActivity(browserIntent);
        });

        this.findViewById(R.id.about_button).setOnClickListener((View v) -> {
            TextView textView = new TextView(this);
            textView.setText(Html.fromHtml(String.format(this.getString(R.string.about_string), BuildConfig.VERSION_NAME, "https://github.com/GustavLindberg99/AndroidWeather", "https://open-meteo.com/")));
            textView.setTextColor(Color.BLACK);
            textView.setLinkTextColor(Color.BLUE);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            final TypedValue value = new TypedValue();
            if(this.getTheme().resolveAttribute(R.attr.dialogPreferredPadding, value, true)){
                final int padding = TypedValue.complexToDimensionPixelSize(value.data, this.getResources().getDisplayMetrics());
                textView.setPadding(padding, WeatherFragment.dpToPx(8), padding, 0);
            }
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            new AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setView(textView)
                .setPositiveButton(R.string.ok, (DialogInterface dialog, int which) -> {})
                .create()
                .show();
        });
    }
}