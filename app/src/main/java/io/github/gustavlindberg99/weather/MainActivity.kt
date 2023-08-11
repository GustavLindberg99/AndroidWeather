@file:Suppress("DEPRECATION")

package io.github.gustavlindberg99.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.matthewtamlin.sliding_intro_screen_library.indicators.DotIndicator
import java.io.IOException
import java.util.*

class MainActivity: AppCompatActivity(), LocationListener {
    private val _fragmentAdapter: FragmentAdapter by lazy{FragmentAdapter(this.supportFragmentManager)}
    private val _dotIndicator: DotIndicator by lazy{this.findViewById(R.id.dot_indicator)}
    private var _askForLocationPermission: Boolean = true

    private inner class FragmentAdapter(fm: FragmentManager): FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT){
        private var _currentLocationFragment: WeatherFragment? = null
        private val _otherLocationFragments: BiMap<Int, WeatherFragment> = HashBiMap.create()
        override fun getItem(position: Int): Fragment {
            val currentLocation = City.currentLocation(this@MainActivity)
            val truePosition = if(currentLocation != null) position - 1 else position
            if(truePosition == -1){
                val currentLocationFragment = WeatherFragment.getInstance(currentLocation!!)    //currentLocation can't be null here otherwise truePosition wouldn't be negative and this block won't be entered, see how truePosition is defined above
                this@FragmentAdapter._currentLocationFragment = currentLocationFragment
                return currentLocationFragment
            }
            val fragment = WeatherFragment.getInstance(City.listFromPreferences(this@MainActivity)[truePosition])
            this@FragmentAdapter._otherLocationFragments[truePosition] = fragment
            return fragment
        }

        override fun getItemPosition(fragment: Any): Int {
            if(fragment !is WeatherFragment){
                return POSITION_NONE
            }
            val currentLocationKnown = City.currentLocation(this@MainActivity) != null
            if(fragment === this@FragmentAdapter._currentLocationFragment){
                return if(currentLocationKnown) 0 else POSITION_NONE
            }
            val position = this@FragmentAdapter._otherLocationFragments.inverse()[fragment] ?: return POSITION_NONE
            return position + if(currentLocationKnown) 1 else 0
        }

        override fun getCount(): Int {
            val currentLocationCount = if(City.currentLocation(this@MainActivity) != null) 1 else 0
            val otherLocationsCount = City.listFromPreferences(this@MainActivity).size
            return currentLocationCount + otherLocationsCount
        }

        fun currentLocationFragment(): WeatherFragment? {
            return this@FragmentAdapter._currentLocationFragment
        }

        fun allFragments(): List<WeatherFragment> {
            val fragments: MutableList<WeatherFragment> = mutableListOf()
            val currentLocationFragment = this@FragmentAdapter._currentLocationFragment
            if(currentLocationFragment != null){
                fragments.add(currentLocationFragment)
            }
            for(fragment in this@FragmentAdapter._otherLocationFragments.values){
                fragments.add(fragment)
            }
            return fragments
        }
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_main)

        //Make the background of the status bar show correctly
        this.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        //Set the fragments
        val viewPager: ViewPager = this.findViewById(R.id.viewpager)
        viewPager.adapter = this._fragmentAdapter

        //Set the dot indicator
        this._dotIndicator.numberOfItems = this._fragmentAdapter.count
        this._dotIndicator.setSelectedItem(0, false)
        viewPager.addOnPageChangeListener(object: OnPageChangeListener{
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int){}
            override fun onPageSelected(position: Int){
                this@MainActivity._dotIndicator.setSelectedItem(position, true)
            }
            override fun onPageScrollStateChanged(state: Int){}
        })

        //Initialize the settings button
        val settingsLauncher = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            for(fragment in this._fragmentAdapter.allFragments()){
                fragment.refreshFromCache()
            }
        }
        this.findViewById<View>(R.id.settings_button).setOnClickListener{settingsLauncher.launch(Intent(this, Settings::class.java))}

        //Initialize the city list button
        val cityListLauncher = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result: ActivityResult ->
            this._fragmentAdapter.notifyDataSetChanged()
            this._dotIndicator.numberOfItems = this._fragmentAdapter.count
            val intent = result.data
            if(intent != null){
                val selectedIndex = intent.getIntExtra(CityList.SELECTED_CITY, viewPager.currentItem)
                this._dotIndicator.setSelectedItem(selectedIndex, false)
                viewPager.setCurrentItem(selectedIndex, false)
            }
        }
        this.findViewById<View>(R.id.city_list_button).setOnClickListener{cityListLauncher.launch(Intent(this, CityList::class.java))}

        //Initialize the background
        @ColorInt val color = getColor(R.color.skyBlue)
        this.findViewById<ImageView>(R.id.background_gradient).setColorFilter(color)
        this.findViewById<ImageView>(R.id.background_color).setColorFilter(color)
    }

    override fun onResume(){
        super.onResume()

        //Update the weather at all locations
        for(city in City.listFromPreferences(this)){
            city.updateWeatherFromServer({city.updateCachedWeather()}, {})
        }

        //Request a location update
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(this._askForLocationPermission){
                this._askForLocationPermission = false
                this.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
            }
            else if(City.listFromPreferences(this).isEmpty()){
                this.findViewById<View>(R.id.city_list_button).callOnClick()
            }
        }
        else{
            //LocationManager.NETWORK_PROVIDER is much faster than LocationManager.GPS_PROVIDER
            (this.getSystemService(LOCATION_SERVICE) as LocationManager).requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 100f, this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(City.listFromPreferences(this).isEmpty()){
                this.findViewById<View>(R.id.city_list_button).callOnClick()
            }
        }
        else {
            (this.getSystemService(LOCATION_SERVICE) as LocationManager).requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 100f, this)
        }
    }

    override fun onLocationChanged(location: Location){
        val previousLocationKnown = City.currentLocation(this) != null
        Thread{
            try{
                val address = Geocoder(this).getFromLocation(location.latitude, location.longitude, 1)!![0]
                val city = City(this, address.cityName(this), location.latitude, location.longitude, TimeZone.getDefault().id)
                city.updateWeatherFromServer({
                    city.saveAsCurrentLocation()
                    val currentLocationFragment = this._fragmentAdapter.currentLocationFragment()
                    currentLocationFragment?.setLocation(city)
                    if(!previousLocationKnown){
                        this._fragmentAdapter.notifyDataSetChanged()
                        this._dotIndicator.numberOfItems = this._fragmentAdapter.count
                    }
                }, {})
            }
            catch(ignore: IOException){
                //If finding the location failed, just keep the previous location
            }
        }.start()
    }

    //These are abstract on some versions of Android but not others, so they must be implemented otherwise it will give a runtime error, see https://stackoverflow.com/a/67900719/4284627
    override fun onProviderEnabled(provider: String){}
    override fun onProviderDisabled(provider: String){}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle){}

    fun selectedCity(): City? {
        var selectedIndex: Int = this._dotIndicator.selectedItemIndex
        val currentLocation: City? = City.currentLocation(this)
        if(currentLocation != null){
            selectedIndex--
        }
        val cities: List<City> = City.listFromPreferences(this)
        if(selectedIndex >= cities.size){
            selectedIndex = cities.size - 1
        }
        if(selectedIndex == -1){
            return currentLocation
        }
        return cities[selectedIndex]
    }
}