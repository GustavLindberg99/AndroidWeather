package io.github.gustavlindberg99.weather

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@Throws(ParseException::class)
fun parseDate(date: String, timezone: String): Calendar? {
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    format.timeZone = TimeZone.getTimeZone(timezone)
    val calendar = Calendar.getInstance()
    calendar.timeZone = TimeZone.getTimeZone(timezone)
    calendar.time = format.parse(date.replace('T', ' ')) ?: throw ParseException("Null returned when parsing date $date", 0)
    if(calendar.timeInMillis <= 0){
        //The API returns a date at or before the Unix epoch when it should return an invalid date.
        return null
    }
    return calendar
}