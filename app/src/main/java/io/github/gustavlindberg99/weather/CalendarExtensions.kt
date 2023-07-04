package io.github.gustavlindberg99.weather

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun Calendar.isDay(sunrise: Calendar?, sunset: Calendar?, latitude: Double): Boolean {
    if(sunrise?.timeInMillis == sunset?.timeInMillis){    //True if they're both null or if none are null and they're the same
        return if(this[Calendar.MONTH] >= Calendar.APRIL && this[Calendar.MONTH] <= Calendar.SEPTEMBER) latitude > 0 else latitude < 0
    }
    else if(sunrise == null){
        return this.timeInMillis <= sunset!!.timeInMillis    //sunset can't be null, otherwise either sunrise is not null and this block isn't executed or they're both null and the function returned in the previous block
    }
    else if(sunset == null){
        return this.timeInMillis > sunrise.timeInMillis
    }
    else if(sunrise.timeInMillis > sunset.timeInMillis){
        return this.timeInMillis > sunrise.timeInMillis || this.timeInMillis <= sunset.timeInMillis
    }
    return this.timeInMillis > sunrise.timeInMillis && this.timeInMillis <= sunset.timeInMillis    //This is the by far most common (sunrise comes before sunset), all the above are edge cases that can occur in polar areas.
}

@Throws(ParseException::class)
fun parseDate(date: String, timezone: String): Calendar? {
    if(date.startsWith("1900")){    //The API returns a date in 1900 when it should return an invalid date.
        return null
    }
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    format.timeZone = TimeZone.getTimeZone(timezone)
    val calendar = Calendar.getInstance()
    calendar.timeZone = TimeZone.getTimeZone(timezone)
    calendar.time = format.parse(date.replace('T', ' ')) ?: throw ParseException("Null returned when parsing date $date", 0)
    return calendar
}