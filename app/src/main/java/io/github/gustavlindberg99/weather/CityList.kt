package io.github.gustavlindberg99.weather

import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.math.roundToInt

class CityList: AppCompatActivity(){
    companion object{
        const val SELECTED_CITY = "selectedCity"
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_city_list)

        //Initialize the Add city button
        val addCityLauncher = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result: ActivityResult ->
            val intent = result.data
            if(intent != null){
                val city = City.fromBundle(this, intent.getBundleExtra(AddCity.CITY))
                if(city != null){
                    city.addAsNewLocation()
                    this.addLocationBar(city)
                }
            }
        }
        this.findViewById<View>(R.id.add_city_button).setOnClickListener{addCityLauncher.launch(Intent(this, AddCity::class.java))}

        //Create the location bars
        val currentLocation = City.currentLocation(this)
        if(currentLocation != null){
            this.addLocationBar(currentLocation)
        }
        for(city in City.listFromPreferences(this)){
            this.addLocationBar(city)
        }
    }

    private fun addLocationBar(city: City){
        //Create the view
        val locationBar: View = View.inflate(this, R.layout.city_list_item, null)
        locationBar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(64.0))

        //City name
        val cityName: TextView = locationBar.findViewById(R.id.city_name)
        cityName.text = city.name
        if(city.isCurrentLocation){
            cityName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.position, 0, 0, 0)
        }

        //Weather
        @DrawableRes @ColorRes var backgroundResource = 0
        val data = city.weatherData
        if(data != null){
            locationBar.findViewById<TextView>(R.id.city_list_temperature).text = Settings.UnitFormatter.temperature(this, data.currentTemperature)
            backgroundResource = getBackgroundResource(this, data)
        }
        if(backgroundResource == 0){
            backgroundResource = R.color.skyBlue
        }
        val backgroundView: ImageView = locationBar.findViewById(R.id.city_list_item_background)
        backgroundView.setImageResource(backgroundResource)
        val backgroundDrawable: Drawable = backgroundView.drawable
        if(backgroundDrawable is AnimationDrawable){
            backgroundDrawable.start()
        }

        //Drag to delete the location
        val locationBarContainer: LinearLayout = this.findViewById(R.id.location_bar_container)
        var isDragging = false
        if(!city.isCurrentLocation){
            var initialX = 0.0
            var offset = 0
            locationBar.setOnTouchListener(fun(view: View, event: MotionEvent): Boolean{
                val layoutParams = locationBar.layoutParams as LinearLayout.LayoutParams
                val newOffset = (initialX - event.x).roundToInt().coerceAtLeast(0)
                val maxMovement = dpToPx(5.0)    //So that it doesn't move back and forth very fast
                if(abs(newOffset - offset) > maxMovement){
                    offset += Integer.signum(newOffset - offset) * maxMovement
                }
                else{
                    offset = newOffset
                }
                when(event.action){
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = false
                        initialX = event.x.toDouble()
                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        layoutParams.setMargins(0, 0, 0, 0)
                        if(offset > 0){
                            locationBarContainer.removeView(locationBar)
                            fixAddCityButtonPosition()
                            city.removeLocation()
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        isDragging = true
                        layoutParams.setMargins(-offset, 0, offset, 0)
                    }
                    else -> return@setOnTouchListener false
                }
                locationBar.layoutParams = layoutParams
                return@setOnTouchListener true
            })
        }

        //Click event on the location bar
        locationBar.setOnClickListener{
            if(isDragging) {
                return@setOnClickListener
            }
            val currentIndex = locationBarContainer.indexOfChild(locationBar)
            if(currentIndex != -1) {
                val intent = Intent()
                intent.putExtra(SELECTED_CITY, currentIndex)
                this.setResult(RESULT_OK, intent)
            }
            finish()
        }

        //Append the view
        locationBarContainer.addView(locationBar)
        fixAddCityButtonPosition()
    }

    private fun fixAddCityButtonPosition(){
        val listHeight = dpToPx((64 * findViewById<LinearLayout>(R.id.location_bar_container).childCount + 96).toDouble())
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        this.findViewById<View>(R.id.city_list_background).layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, if(listHeight > screenHeight) FrameLayout.LayoutParams.MATCH_PARENT else listHeight)
    }
}