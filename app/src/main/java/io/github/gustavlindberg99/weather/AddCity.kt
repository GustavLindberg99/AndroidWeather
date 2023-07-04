package io.github.gustavlindberg99.weather

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class AddCity : AppCompatActivity(){
    companion object{
        const val CITY = "city"
    }
    private val _suggestionsLayout: LinearLayout by lazy{this.findViewById(R.id.city_search_suggestions)}

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_add_city)

        val citySearch: EditText = this.findViewById(R.id.city_search)
        citySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int){}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int){}
            override fun afterTextChanged(s: Editable){
                val searchQuery = citySearch.text.toString()
                if(searchQuery.isEmpty()){
                    this@AddCity._suggestionsLayout.removeAllViews()
                    return
                }
                this@AddCity.showMessage(R.string.loading)
                Thread(@Suppress("DEPRECATION") fun(){
                    try{
                        val addresses: List<Address> = Geocoder(this@AddCity).getFromLocationName(searchQuery, 30) ?: listOf()
                        if(searchQuery != citySearch.text.toString()){
                            return    //If the user continued typing, don't do anything here, instead wait for the results of the new query
                        }
                        if(addresses.isEmpty()){
                            this@AddCity.runOnUiThread{ this@AddCity.showMessage(R.string.noResults) }
                            return
                        }
                        val cities: MutableList<City> = mutableListOf()
                        val labels: MutableList<String> = mutableListOf()
                        var i = 0
                        while(i < addresses.size){
                            val address = addresses[i]
                            val name: String = address.cityName(this@AddCity)
                            if(!address.hasLatitude() || !address.hasLongitude()){
                                i++
                                continue
                            }
                            var label: String = name
                            if(address.adminArea != null){
                                label += ", " + address.adminArea
                            }
                            if(address.countryName != null){
                                label += ", " + address.countryName
                            }
                            if(labels.contains(label)){    //Avoid duplicates
                                i++
                                continue
                            }
                            val timezone: String = address.timezone(this@AddCity)
                            labels.add(label)
                            val city = City(this@AddCity, name, address.latitude, address.longitude, timezone)
                            city.updateWeatherFromServer({}, {})
                            cities.add(city)
                            if(searchQuery != citySearch.text.toString()){
                                return    //If the user continued typing, don't do anything here, instead wait for the results of the new query
                            }
                            i++
                        }
                        this@AddCity.runOnUiThread{this@AddCity.setSuggestions(cities, labels)}
                    }
                    catch(ignore: IOException){
                        if(searchQuery == citySearch.text.toString()){
                            this@AddCity.runOnUiThread{this@AddCity.showMessage(R.string.noInternetConnection)}
                        }
                    }
                }).start()
            }
        })
    }

    private fun setSuggestions(cities: List<City>, labels: List<String>){
        this._suggestionsLayout.removeAllViews()
        for(i in cities.indices){
            val city: City = cities[i]
            val label = TextView(this)
            label.text = labels[i]
            label.setPadding(0, dpToPx(8.0), 0, dpToPx(8.0))
            label.textSize = 20f
            this._suggestionsLayout.addView(label)
            label.setOnClickListener{
                val intent = Intent()
                intent.putExtra(CITY, city.toBundle())
                this.setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun showMessage(stringResource: Int){
        this._suggestionsLayout.removeAllViews()
        val label = TextView(this)
        label.text = this.getString(stringResource)
        label.setPadding(0, dpToPx(8.0), 0, dpToPx(8.0))
        label.textSize = 20f
        this._suggestionsLayout.addView(label)
    }
}