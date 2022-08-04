package io.github.gustavlindberg99.weather;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddCity extends AppCompatActivity{
    public static final String CITY = "city";

    private LinearLayout _suggestionsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_add_city);

        this._suggestionsLayout = this.findViewById(R.id.city_search_suggestions);

        EditText citySearch = this.findViewById(R.id.city_search);
        citySearch.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){}

            @Override
            public void afterTextChanged(Editable s){
                final String searchQuery = citySearch.getText().toString();
                if(searchQuery.isEmpty()){
                    AddCity.this._suggestionsLayout.removeAllViews();
                    return;
                }

                showMessage(R.string.loading);
                new Thread(() -> {
                    try{
                        List<Address> addresses = new Geocoder(AddCity.this).getFromLocationName(searchQuery, 30);
                        if(!searchQuery.equals(citySearch.getText().toString())){
                            return;    //If the user continued typing, don't do anything here, instead wait for the results of the new query
                        }

                        if(addresses.size() == 0){
                            AddCity.this.runOnUiThread(() -> showMessage(R.string.noResults));
                            return;
                        }

                        List<City> cities = new ArrayList<>();
                        List<String> labels = new ArrayList<>();
                        for(int i = 0; i < addresses.size(); i++){
                            final Address address = addresses.get(i);
                            final String name = AddressFunctions.cityNameFromAddress(AddCity.this, address);

                            if(!address.hasLatitude() || !address.hasLongitude()){
                                continue;
                            }

                            String label = name;
                            if(address.getAdminArea() != null){
                                label += ", " + address.getAdminArea();
                            }
                            if(address.getCountryName() != null){
                                label += ", " + address.getCountryName();
                            }
                            if(labels.contains(label)){    //Avoid duplicates
                                continue;
                            }

                            final String timezone = AddressFunctions.timezoneFromAddress(AddCity.this, address);
                            labels.add(label);
                            City city = new City(AddCity.this, name, address.getLatitude(), address.getLongitude(), timezone);
                            city.updateWeatherFromServer(() -> {}, () -> {});
                            cities.add(city);

                            if(!searchQuery.equals(citySearch.getText().toString())){
                                return;    //If the user continued typing, don't do anything here, instead wait for the results of the new query
                            }
                        }
                        AddCity.this.runOnUiThread(() -> setSuggestions(cities, labels));
                    }
                    catch(IOException ignore){
                        if(searchQuery.equals(citySearch.getText().toString())){
                            AddCity.this.runOnUiThread(() -> showMessage(R.string.noInternetConnection));
                        }
                    }
                }).start();
            }
        });
    }

    private void setSuggestions(List<City> cities, List<String> labels){
        this._suggestionsLayout.removeAllViews();

        for(int i = 0; i < cities.size(); i++){
            final City city = cities.get(i);
            TextView label = new TextView(this);
            label.setText(labels.get(i));
            label.setPadding(0, WeatherFragment.dpToPx(8), 0, WeatherFragment.dpToPx(8));
            label.setTextSize(20);
            this._suggestionsLayout.addView(label);

            label.setOnClickListener((View v) -> {
                Intent intent = new Intent();
                intent.putExtra(CITY, city.toBundle());
                this.setResult(RESULT_OK, intent);
                this.finish();
            });
        }
    }

    private void showMessage(int stringResource){
        this._suggestionsLayout.removeAllViews();
        TextView label = new TextView(this);
        label.setText(this.getString(stringResource));
        label.setPadding(0, WeatherFragment.dpToPx(8), 0, WeatherFragment.dpToPx(8));
        label.setTextSize(20);
        this._suggestionsLayout.addView(label);
    }
}