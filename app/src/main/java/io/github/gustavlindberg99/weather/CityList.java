package io.github.gustavlindberg99.weather;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.AtomicDouble;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CityList extends AppCompatActivity{
    public static final String SELECTED_CITY = "selectedCity";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_city_list);

        //Initialize the Add city button
        final ActivityResultLauncher<Intent> addCityLauncher = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (ActivityResult result) -> {
            final Intent intent = result.getData();
            if(intent != null){
                final City city = City.fromBundle(this, intent.getBundleExtra(AddCity.CITY));
                if(city != null){
                    city.addAsNewLocation();
                    this.addLocationBar(city);
                }
            }
        });
        this.findViewById(R.id.add_city_button).setOnClickListener((View v) ->
            addCityLauncher.launch(new Intent(this, AddCity.class))
        );

        //Create the location bars
        final @Nullable City currentLocation = City.currentLocation(this);
        if(currentLocation != null){
            this.addLocationBar(currentLocation);
        }
        for(City city: City.listFromPreferences(this)){
            this.addLocationBar(city);
        }
    }

    private void addLocationBar(@NonNull City city){
        //Create the view
        View locationBar = View.inflate(this, R.layout.city_list_item, null);
        locationBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WeatherFragment.dpToPx(64)));

        //City name
        TextView cityName = locationBar.findViewById(R.id.city_name);
        cityName.setText(city.name);
        if(city.isCurrentLocation()){
            cityName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.position, 0, 0, 0);
        }

        //Weather
        @DrawableRes @ColorRes int backgroundResource = 0;
        final @Nullable WeatherData data = city.weatherData;
        if(data != null){
            ((TextView) locationBar.findViewById(R.id.city_list_temperature)).setText(Settings.UnitFormatter.temperature(CityList.this, data.currentTemperature));
            backgroundResource = WeatherFragment.getBackgroundResource(CityList.this, data.currentWeatherCode, Calendar.getInstance(), data.sunrises[0], data.sunsets[0], city.latitude);
        }
        if(backgroundResource == 0){
            backgroundResource = R.color.skyBlue;
        }
        final ImageView backgroundView = locationBar.findViewById(R.id.city_list_item_background);
        backgroundView.setImageResource(backgroundResource);
        final Drawable backgroundDrawable = backgroundView.getDrawable();
        if(backgroundDrawable instanceof AnimationDrawable){
            ((AnimationDrawable) backgroundDrawable).start();
        }

        //Drag to delete the location
        LinearLayout locationBarContainer = this.findViewById(R.id.location_bar_container);
        AtomicBoolean isDragging = new AtomicBoolean(false);
        if(!city.isCurrentLocation()){
            AtomicDouble initialX = new AtomicDouble();
            AtomicInteger offset = new AtomicInteger();

            locationBar.setOnTouchListener((View v, MotionEvent event) -> {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) locationBar.getLayoutParams();
                final int newOffset = Math.max(0, (int) Math.round(initialX.get() - event.getX()));
                final int maxMovement = WeatherFragment.dpToPx(5);    //So that it doesn't move back and forth very fast
                if(Math.abs(newOffset - offset.get()) > maxMovement){
                    offset.set(offset.get() + Integer.signum(newOffset - offset.get()) * maxMovement);
                }
                else{
                    offset.set(newOffset);
                }

                switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    isDragging.set(false);
                    initialX.set(event.getX());
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    layoutParams.setMargins(0, 0, 0, 0);
                    if(offset.get() > 0){
                        locationBarContainer.removeView(locationBar);
                        this.fixAddCityButtonPosition();
                        city.removeLocation();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    isDragging.set(true);
                    layoutParams.setMargins(-offset.get(), 0, offset.get(), 0);
                    break;
                default:
                    return false;
                }
                locationBar.setLayoutParams(layoutParams);
                return true;
            });
        }

        //Click event on the location bar
        locationBar.setOnClickListener((View v) -> {
            if(isDragging.get()){
                return;
            }
            final int currentIndex = locationBarContainer.indexOfChild(locationBar);
            if(currentIndex != -1){
                Intent intent = new Intent();
                intent.putExtra(SELECTED_CITY, currentIndex);
                this.setResult(RESULT_OK, intent);
            }
            this.finish();
        });

        //Append the view
        locationBarContainer.addView(locationBar);
        this.fixAddCityButtonPosition();
    }

    private void fixAddCityButtonPosition(){
        final int listHeight = WeatherFragment.dpToPx(64 * ((LinearLayout) this.findViewById(R.id.location_bar_container)).getChildCount() + 96);
        final int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        this.findViewById(R.id.city_list_background).setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (listHeight > screenHeight) ? FrameLayout.LayoutParams.MATCH_PARENT : listHeight));
    }
}