package io.github.gustavlindberg99.weather

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import java.util.*
import kotlin.math.ceil

@SuppressLint("DiscouragedApi")
@DrawableRes
fun getBackgroundResource(context: Context, data: WeatherData): Int {
    val day = data.currentIsDay
    val resourceName: String = when(data.currentWeatherCode / 10) {
        0 -> "bg_" + data.currentWeatherCode
        4 -> "bg_fog"
        5, 6, 8 -> {
            if(data.currentWeatherCode == 85 || data.currentWeatherCode == 86) "bg_snow"
            else "bg_rain"
        }
        7 -> "bg_snow"
        9 -> "bg_thunderstorm"
        else -> return 0
    } + if(day) "_day" else "_night"
    return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
}

@DrawableRes
fun removeAnimation(@DrawableRes resourceId: Int): Int {
    return when(resourceId){
        R.drawable.bg_rain_day, R.drawable.bg_thunderstorm_day -> R.drawable.bg_rain_day_0
        R.drawable.bg_rain_night, R.drawable.bg_thunderstorm_night -> R.drawable.bg_rain_night_0
        R.drawable.bg_snow_day -> R.drawable.bg_snow_day_0
        R.drawable.bg_snow_night -> R.drawable.bg_snow_night_0
        else -> resourceId
    }
}

@ColorInt
fun medianColor(context: Context, @DrawableRes image: Int): Int {
    val bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, removeAnimation(image))
    val resolution = 20    //Don't look at all pixels for performance reasons (this function is very slow otherwise, and no one will notice if the median isn't exact)
    val width: Int = bitmap.width
    val height: Int = bitmap.height
    val size: Int = ceil(width.toDouble() / resolution).toInt() * ceil(height.toDouble() / resolution).toInt()
    val red = mutableListOf<Int>()
    val green = mutableListOf<Int>()
    val blue = mutableListOf<Int>()
    var x = 0
    while(x < width){
        var y = 0
        while(y < height){
            @ColorInt val color = bitmap.getPixel(x, y)
            red.add(color and 0x00FF0000)     //This is actually red * 255**2, but it doesn't matter since we're only interested in sorting it
            green.add(color and 0x0000FF00)     //This is actually green * 255, but it doesn't matter since we're only interested in sorting it
            blue.add(color and 0x000000FF)
            y += resolution
        }
        x += resolution
    }
    red.sort()
    green.sort()
    blue.sort()
    return -0x1000000 or red[size / 2] or green[size / 2] or blue[size / 2]     //0xFF000000 for the alpha channel (which is always maximal for the images we use this function on), -0x1000000 is the signed version of 0xFF000000
}

fun dpToPx(dp: Double): Int {
    return (dp * Resources.getSystem().displayMetrics.density + 0.5).toInt()
}