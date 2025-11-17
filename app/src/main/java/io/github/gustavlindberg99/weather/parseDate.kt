package io.github.gustavlindberg99.weather

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Parses the given date into a Calendar object.
 *
 * @param date      A date string of the format `"yyyy-MM-dd HH:mm"`.
 * @param timezone  The timezone that the times are given in.
 *
 * @return The parsed date as a Calendar object, or null if the date is before 2000. This is because the open-meteo API returns a date sometime a long time ago when it should return an invalid date (usually around the Unix epoch, but sometimes not exactly at the Unix epoch).
 */
fun parseDate(date: String, timezone: String): Calendar? {
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    format.timeZone = TimeZone.getTimeZone(timezone)
    val calendar = Calendar.getInstance()
    calendar.timeZone = TimeZone.getTimeZone(timezone)
    calendar.time = format.parse(date.replace('T', ' '))
        ?: throw ParseException("Null returned when parsing date $date", 0)
    if (calendar[Calendar.YEAR] < 2000) {
        return null
    }
    return calendar
}