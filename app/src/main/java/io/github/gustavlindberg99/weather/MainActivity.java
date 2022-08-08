package io.github.gustavlindberg99.weather;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.matthewtamlin.sliding_intro_screen_library.indicators.DotIndicator;

import java.io.IOException;
import java.util.Objects;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements LocationListener{
    private FragmentAdapter _fragmentAdapter;
    private DotIndicator _dotIndicator;
    private boolean _askForLocationPermission = true;

    @SuppressWarnings("deprecation")
    private class FragmentAdapter extends FragmentPagerAdapter{
        private @Nullable WeatherFragment _currentLocationFragment = null;
        private final BiMap<Integer, WeatherFragment> _otherLocationFragments = HashBiMap.create();

        public FragmentAdapter(@NonNull FragmentManager fm){
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public @NonNull Fragment getItem(int position){
            final @Nullable City currentLocation = City.currentLocation(MainActivity.this);
            if(currentLocation != null){
                position--;
            }
            if(position == -1){
                this._currentLocationFragment = WeatherFragment.getInstance(Objects.requireNonNull(currentLocation));
                return this._currentLocationFragment;
            }
            final WeatherFragment fragment = WeatherFragment.getInstance(City.listFromPreferences(MainActivity.this)[position]);
            this._otherLocationFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemPosition(@NonNull Object fragment){
            if(!(fragment instanceof WeatherFragment)){
                return POSITION_NONE;
            }

            final boolean currentLocationKnown = City.currentLocation(MainActivity.this) != null;
            if(fragment == this._currentLocationFragment){
                if(currentLocationKnown){
                    return 0;
                }
                else{
                    return POSITION_NONE;
                }
            }

            final Integer position = this._otherLocationFragments.inverse().get(fragment);
            if(position == null){
                return POSITION_NONE;
            }
            return position + (currentLocationKnown ? 1 : 0);
        }

        @Override
        public int getCount(){
            final int currentLocationCount = (City.currentLocation(MainActivity.this) != null) ? 1 : 0;
            final int otherLocationsCount = City.listFromPreferences(MainActivity.this).length;
            return currentLocationCount + otherLocationsCount;
        }

        public @Nullable WeatherFragment currentLocationFragment(){
            return this._currentLocationFragment;
        }

        public WeatherFragment[] allFragments(){
            final int currentLocationFragmentCount = (this._currentLocationFragment != null) ? 1 : 0;
            final int otherLocationFragmentCount = this._otherLocationFragments.size();
            final int fragmentCount = currentLocationFragmentCount + otherLocationFragmentCount;
            WeatherFragment[] fragments = new WeatherFragment[fragmentCount];
            int i = 0;
            if(this._currentLocationFragment != null){
                fragments[i] = this._currentLocationFragment;
                i++;
            }
            for(WeatherFragment fragment: this._otherLocationFragments.values()){
                fragments[i] = fragment;
                i++;
            }
            return fragments;
        }
    }

    public City selectedCity(){
        int selectedIndex = this._dotIndicator.getSelectedItemIndex();
        final @Nullable City currentLocation = City.currentLocation(MainActivity.this);
        if(currentLocation != null){
            selectedIndex--;
        }
        final City[] cities = City.listFromPreferences(this);
        if(selectedIndex >= cities.length){
            selectedIndex = cities.length - 1;
        }
        if(selectedIndex == -1){
            return currentLocation;
        }
        return cities[selectedIndex];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        //Make the background of the status bar show correctly
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        //Set the fragments
        ViewPager viewPager = this.findViewById(R.id.viewpager);
        this._fragmentAdapter = new FragmentAdapter(this.getSupportFragmentManager());
        viewPager.setAdapter(this._fragmentAdapter);

        //Set the dot indicator
        this._dotIndicator = this.findViewById(R.id.dot_indicator);
        this._dotIndicator.setNumberOfItems(this._fragmentAdapter.getCount());
        this._dotIndicator.setSelectedItem(0, false);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){}

            @Override
            public void onPageSelected(int position){
                MainActivity.this._dotIndicator.setSelectedItem(position, true);
            }

            @Override
            public void onPageScrollStateChanged(int state){}
        });

        //Initialize the settings button
        final ActivityResultLauncher<Intent> settingsLauncher = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (ActivityResult result) -> {
            for(WeatherFragment fragment: this._fragmentAdapter.allFragments()){
                fragment.refreshFromCache();
            }
        });
        this.findViewById(R.id.settings_button).setOnClickListener((View v) ->
            settingsLauncher.launch(new Intent(this, Settings.class))
        );

        //Initialize the city list button
        final ActivityResultLauncher<Intent> cityListLauncher = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (ActivityResult result) -> {
            this._fragmentAdapter.notifyDataSetChanged();
            this._dotIndicator.setNumberOfItems(this._fragmentAdapter.getCount());
            final Intent intent = result.getData();
            if(intent != null){
                final int selectedIndex = intent.getIntExtra(CityList.SELECTED_CITY, viewPager.getCurrentItem());
                this._dotIndicator.setSelectedItem(selectedIndex, false);
                viewPager.setCurrentItem(selectedIndex, false);
            }
        });
        this.findViewById(R.id.city_list_button).setOnClickListener((View v) ->
            cityListLauncher.launch(new Intent(this, CityList.class))
        );

        //Initialize the background
        final @ColorInt int color = this.getColor(R.color.skyBlue);
        ((ImageView) this.findViewById(R.id.background_gradient)).setColorFilter(color);
        ((ImageView) this.findViewById(R.id.background_color)).setColorFilter(color);
    }

    @Override
    protected void onResume(){
        super.onResume();

        //Update the weather at all locations
        for(City city: City.listFromPreferences(this)){
            city.updateWeatherFromServer(city::updateCachedWeather, () -> {});
        }

        //Request a location update
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(this._askForLocationPermission){
                this._askForLocationPermission = false;
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
            else if(City.listFromPreferences(this).length == 0){
                this.findViewById(R.id.city_list_button).callOnClick();
            }
        }
        else{
            ((LocationManager) this.getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(City.listFromPreferences(this).length == 0){
                this.findViewById(R.id.city_list_button).callOnClick();
            }
        }
        else{
            ((LocationManager) this.getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    @Override
    public void onLocationChanged(Location location){
        final boolean previousLocationKnown = (City.currentLocation(this) != null);
        new Thread(() -> {
            try{
                final Address address = new Geocoder(this).getFromLocation(location.getLatitude(), location.getLongitude(), 1).get(0);
                City city = new City(this, AddressFunctions.cityNameFromAddress(this, address), location.getLatitude(), location.getLongitude(), TimeZone.getDefault().getID());

                city.updateWeatherFromServer(() -> {
                    city.saveAsCurrentLocation();

                    final WeatherFragment currentLocationFragment = this._fragmentAdapter.currentLocationFragment();
                    if(currentLocationFragment != null){
                        currentLocationFragment.setLocation(city);
                    }
                    if(!previousLocationKnown){
                        this._fragmentAdapter.notifyDataSetChanged();
                        this._dotIndicator.setNumberOfItems(this._fragmentAdapter.getCount());
                    }
                }, () -> {
                    //If finding the weather at the new location failed, just keep the previous location
                });
            }
            catch(IOException ignore){
                //If finding the location failed, just keep the previous location
            }
        }).start();
    }
}