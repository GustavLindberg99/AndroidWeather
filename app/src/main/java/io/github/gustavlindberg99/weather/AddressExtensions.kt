package io.github.gustavlindberg99.weather

import android.content.Context
import android.location.Address
import android.location.Geocoder
import org.apache.commons.text.WordUtils
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * Utility function that returns null if the string is empty or only contains spaces.
 *
 * @param s                 The string the check if it's empty.
 * @param fixCapitalization If true, converts all-caps or all-lowercase strings so that the first letter of each word is captitalized.
 *
 * @return Null if `s` is null, empty, or only spaces, `s` itself otherwise (potentially with capitalization fixed).
 */
private fun nullIfEmpty(s: String?, fixCapitalization: Boolean = true): String? {
    if (s == null || s.trim().isEmpty()) {
        return null
    }
    if (fixCapitalization && (s == s.uppercase(Locale.getDefault()) || s == s.lowercase(Locale.getDefault()))) {
        return WordUtils.capitalizeFully(s.trim())
    }
    return s.trim()
}

/**
 * Gets the name of the city that the address is in.
 *
 * @param context   The Android context.
 */
fun Address.cityName(context: Context): String {
    val locality: String? = nullIfEmpty(this.locality)
    if (locality != null) {
        return locality
    }
    val addressLine: String? = this.getAddressLine(0)
    if (addressLine != null) {
        val matcher = Pattern.compile(
            ",[0-9\\s]*([^0-9,]+)[0-9\\s]*,\\s*" + Pattern.quote(
                this.countryName ?: ""
            ) + "$"
        ).matcher(addressLine)
        if (matcher.find()) {
            val match: String? = nullIfEmpty(matcher.group(1))
            if (match != null) {
                return match
            }
        }
    }
    return nullIfEmpty(this.subLocality) ?: nullIfEmpty(this.subAdminArea)
    ?: nullIfEmpty(this.adminArea) ?: nullIfEmpty(this.countryName, fixCapitalization = false)
    ?: context.getString(R.string.unknownLocation)
}

/**
 * Gets the timezone that the address is in.
 *
 * @param context   The Android context.
 *
 * @throws IOException if getting the non-localized address failed.
 */
fun Address.timezone(context: Context): String {    //There are libraries that can do this, but they only check the coordinates which means they have to use a huge database which is slow and can easily run out of memory. This function is more efficient because since we already have the Address object we can find the timezone by looking at the country/region.
    //Source: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
    val nonLocalizedAddress: Address by lazy(fun(): Address {
        if (this.locale == Locale.US) {
            return this
        }
        @Suppress("DEPRECATION")    //getFromLocation is deprecated
        val results: List<Address> =
            Geocoder(context, Locale.US).getFromLocation(this.latitude, this.longitude, 1)
                ?: listOf()
        if (results.isEmpty()) {
            return this
        }
        return results[0]
    })
    when (this.countryCode) {
        "AD" -> return "Europe/Andorra"
        "AE" -> return "Asia/Dubai"
        "AF" -> return "Asia/Kabul"
        "AG" -> return "America/Antigua"
        "AI" -> return "America/Anguilla"
        "AL" -> return "Europe/Tirane"
        "AM" -> return "Asia/Yerevan"
        "AO" -> return "Africa/Luanda"
        "AQ" -> return if (this.latitude < -88.75) "Antarctica/South_Pole"
        else if (this.longitude > -90 && this.longitude < -20) "Antarctica/Palmer"
        else if (this.longitude > 160 || this.longitude < -150) "Antarctica/McMurdo"
        else if (this.latitude < -80 || this.longitude < 15) "Antarctica/Troll"
        else if (this.longitude < 45) "Antarctica/Syowa"
        else if (this.longitude < 70) "Antarctica/Mawson"
        else if (this.longitude < 100) "Antarctica/Davis"
        else if (this.longitude < 110 && this.latitude < -78) "Antarctica/Troll"
        else if (this.longitude < 135) "Antarctica/Casey"
        else "Antarctica/DumontDUrville"

        "AR" -> return when (nonLocalizedAddress.adminArea?.replace("á", "a")?.replace("é", "e")
            ?.replace("Provincia de ", "")?.replace(" Province", "")) {
            "Buenos Aires" -> "America/Argentina/Buenos_Aires"
            "Catamarca", "Chubut" -> "America/Argentina/Catamarca"
            "Jujuy" -> "America/Argentina/Jujuy"
            "La Rioja" -> "America/Argentina/La_Rioja"
            "Mendoza" -> "America/Argentina/Mendoza"
            "Santa Cruz" -> "America/Argentina/Rio_Gallegos"
            "Salta" -> "America/Argentina/Salta"
            "San Juan" -> "America/Argentina/San_Juan"
            "San Luis" -> "America/Argentina/San_Luis"
            "Tucuman" -> "America/Argentina/Tucuman"
            "Tierra del Fuego" -> "America/Argentina/Ushuaia"
            else -> "America/Argentina/Cordoba"
        }

        "AS" -> return "Pacific/Pago_Pago"
        "AT" -> return "Europe/Vienna"
        "AU" -> {
            if (this.latitude < -50) {
                return "Antarctica/Macquarie"
            }
            when (nonLocalizedAddress.adminArea) {
                "South Australia" -> return "Australia/Adelaide"
                "Queensland" -> return "Australia/Brisbane"
                "Northern Territory" -> return "Australia/Darwin"
                "Tasmania" -> return "Australia/Hobart"
                "Victoria" -> return "Australia/Melbourne"
                "Western Australia" -> return "Australia/Perth"
                "New South Wales", "Australian Capital Territory", "Jervis Bay Territory" -> return if (this.longitude < 141.9 && this.latitude > -32.2 && this.latitude < -31.6) "Australia/Broken_Hill"
                else if (nonLocalizedAddress.locality == "Lord Howe Island") "Australia/Lord_Howe"
                else "Australia/Sydney"
            }
        }

        "AW" -> return "America/Aruba"
        "AX" -> return "Europe/Mariehamn"
        "AZ" -> return "Asia/Baku"
        "BA" -> return "Europe/Sarajevo"
        "BB" -> return "America/Barbados"
        "BD" -> return "Asia/Dhaka"
        "BE" -> return "Europe/Brussels"
        "BF" -> return "Africa/Ouagadougou"
        "BG" -> return "Europe/Sofia"
        "BH" -> return "Asia/Bahrain"
        "BI" -> return "Africa/Bujumbura"
        "BJ" -> return "Africa/Porto-Novo"
        "BL" -> return "America/St_Barthelemy"
        "BM" -> return "Atlantic/Bermuda"
        "BN" -> return "Asia/Brunei"
        "BO" -> return "America/La_Paz"
        "BQ" -> return "America/Kralendijk"
        "BR" -> {
            if (this.longitude > -34) {
                return "America/Noronha"
            }
            return when (nonLocalizedAddress.adminArea?.replace("á", "a")?.replace("ã", "a")
                ?.replace("í", "i")?.replace("ô", "o")?.replace("State of ", "")) {
                "Roraima" -> "America/Boa_Vista"
                "Amapa", "Para" -> "America/Belem"
                "Amazonas" -> if (-5.33 * this.longitude - 2.41 * this.latitude >= 382.97) "America/Eirunepe" else "America/Manaus"
                "Maranhao", "Piaui", "Ceara", "Rio Grande do Norte", "Paraiba" -> "America/Fortaleza"
                "Tocantins" -> "America/Araguaina"
                "Acre" -> "America/Rio_Branco"
                "Rondonia" -> "America/Porto_Velho"
                "Mato Grosso" -> "America/Cuiaba"
                "Pernambuco" -> "America/Recife"
                "Bahia" -> "America/Bahia"
                "Alagoas", "Sergipe" -> "America/Maceio"
                "Mato Grosso do Sul" -> "America/Campo_Grande"
                else -> "America/Sao_Paulo"
            }
        }

        "BS" -> return "America/Nassau"
        "BT" -> return "Asia/Thimphu"
        "BW" -> return "Africa/Gaborone"
        "BY" -> return "Europe/Minsk"
        "BZ" -> return "America/Belize"
        "CA" -> {
            when (nonLocalizedAddress.adminArea?.replace("é", "e")) {
                "Prince Edward Island", "Nova Scotia", "New Brunswick" -> return "America/Halifax"
                "Manitoba" -> return "America/Winnipeg"
                "Alberta" -> return "America/Edmonton"
                "Yukon" -> return "America/Whitehorse"
                "Northwest Territories" -> return "America/Yellowknife"
                "Saskatchewan" -> return if (this.latitude < 53.55 && this.latitude > 53 && this.longitude < -109.55) "America/Edmonton" else "America/Regina"
                "Nunavut" -> return if (nonLocalizedAddress.locality == "Coral Harbour") "America/Coral_Harbour"
                else if (this.longitude > -85 || nonLocalizedAddress.locality == "Arctic Bay") "America/Iqaluit"
                else if (this.longitude < -102 || (this.latitude > 67 && this.latitude < 73 && this.longitude < -89)) "America/Cambridge_Bay"
                else "America/Rankin_Inlet"

                "Newfoundland and Labrador" -> return if (this.longitude > -57.1 && this.latitude < 53.5) "America/St_Johns" else "America/Goose_Bay"
                "Quebec" -> return if (this.longitude > -51.6) "America/Blanc-Sablon" else "America/Montreal"
                "Ontario" -> return if (this.longitude > -90) {
                    if (this.latitude > 52.9 && this.latitude < 54 && this.longitude < -88) "America/Winnipeg" else "America/Toronto"
                }
                else {
                    if (this.longitude > -91 && this.latitude > 51 && this.latitude < 51.7) "America/Atikokan"
                    else if (this.longitude > -91 && this.latitude < 49.2) "America/Toronto"
                    else if (this.longitude > -92 && this.latitude < 49) "America/Atikokan"
                    else "America/Winnipeg"
                }

                "British Columbia" -> return if (this.latitude < 50 && this.longitude > -116.88 && (this.longitude < -116.4 || (this.latitude < 49.3 && this.longitude < -116))) "America/Creston"
                else if (this.longitude > -116.88 || (this.latitude > 51 && this.longitude > -117.7)) "America/Edmonton"
                else if (-6.24 * this.longitude - 8.63 * this.latitude < 284.65) "America/Fort_Nelson"
                else "America/Vancouver"
            }
        }

        "CC" -> return "Indian/Cocos"
        "CD" -> return "Africa/Kinshasa"
        "CF" -> return "Africa/Bangui"
        "CG" -> return "Africa/Brazzaville"
        "CH" -> return "Europe/Zurich"
        "CI" -> return "Africa/Abidjan"
        "CK" -> return "Pacific/Rarotonga"
        "CL" -> return if (this.longitude < -90) "Pacific/Easter"
        else if ((nonLocalizedAddress.adminArea
                ?: "").contains("Magallanes")
        ) "America/Punta_Arenas"
        else "America/Santiago"

        "CM" -> return "Africa/Douala"
        "CN" -> return "Asia/Shanghai"
        "CO" -> return "America/Bogota"
        "CR" -> return "America/Costa_Rica"
        "CU" -> return "America/Havana"
        "CV" -> return "Atlantic/Cape_Verde"
        "CW" -> return "America/Curacao"
        "CX" -> return "Indian/Christmas"
        "CY" -> return "Asia/Nicosia"
        "CZ" -> return "Europe/Prague"
        "DE" -> return "Europe/Berlin"
        "DJ" -> return "Africa/Djibouti"
        "DK" -> return "Europe/Copenhagen"
        "DM" -> return "America/Dominica"
        "DO" -> return "America/Santo_Domingo"
        "DZ" -> return "Africa/Algiers"
        "EC" -> return if (this.longitude < -85) "Pacific/Galapagos" else "America/Guayaquil"
        "EE" -> return "Europe/Tallinn"
        "EG" -> return "Africa/Cairo"
        "EH" -> return "Africa/El_Aaiun"
        "ER" -> return "Africa/Asmara"
        "ES" -> return if (this.longitude < -10) "Atlantic/Canary" else "Europe/Madrid"
        "ET" -> return "Africa/Addis_Ababa"
        "FI" -> return "Europe/Helsinki"
        "FJ" -> return "Pacific/Fiji"
        "FK" -> return "Atlantic/Stanley"
        "FM" -> return if (this.longitude > 154) "Pacific/Pohnpei" else "Pacific/Chuuk"
        "FO" -> return "Atlantic/Faroe"
        "FR" -> return "Europe/Paris"
        "GA" -> return "Africa/Libreville"
        "GB" -> return "Europe/London"
        "GD" -> return "America/Grenada"
        "GE" -> return "Asia/Tbilisi"
        "GF" -> return "America/Cayenne"
        "GG" -> return "Europe/Guernsey"
        "GH" -> return "Africa/Accra"
        "GI" -> return "Europe/Gibraltar"
        "GL" -> return if (this.longitude > -32 && this.latitude > 70 && this.latitude < 74) "America/Scoresbysund"
        else if (this.longitude > -23 && this.latitude > 75 && this.latitude < 78) return "America/Danmarkshavn"
        else if (this.longitude < -66) return "America/Thule"
        else "America/Nuuk"

        "GM" -> return "Africa/Banjul"
        "GN" -> return "Africa/Conakry"
        "GP" -> return "America/Guadeloupe"
        "GQ" -> return "Africa/Malabo"
        "GR" -> return "Europe/Athens"
        "GS" -> return "Atlantic/South_Georgia"
        "GT" -> return "America/Guatemala"
        "GU" -> return "Pacific/Guam"
        "GW" -> return "Africa/Bissau"
        "GY" -> return "America/Guyana"
        "HK" -> return "Asia/Hong_Kong"
        "HN" -> return "America/Tegucigalpa"
        "HR" -> return "Europe/Zagreb"
        "HT" -> return "America/Port-au-Prince"
        "HU" -> return "Europe/Budapest"
        "ID" -> when (nonLocalizedAddress.adminArea) {
            "Aceh", "Bengkulu", "Jambi", "Lampung", "North Sumatra", "Sumatera Utara", "Riau", "South Sumatra", "Sumatera Selatan", "West Sumatra", "Sumatera Barat", "Riau Islands", "Kepulauan Riau", "Bangka Belitung Islands", "Kepulauan Bangka Belitung", "Banten", "Jakarta", "Daerah Khusus Ibukota Jakarta", "West Java", "Jawa Barat", "Central Java", "Jawa Tengah", "Special Region of Yogyakarta", "Daerah Istimewa Yogyakarta", "East Java", "Jawa Timur" -> return "Asia/Jakarta"
            "West Kalimantan", "Kalimantan Barat", "Central Kalimantan", "Kalimantan Tengah" -> return "Asia/Pontianak"
            "South Kalimantan", "Kalimantan Selatan", "East Kalimantan", "Kalimantan Timur", "North Kalimantan", "Kalimantan Utara", "North Sulawesi", "Sulawesi Utara", "Gorontalo", "Central Sulawesi", "Sulawesi Tengah", "West Sulawesi", "Sulawesi Barat", "South Sulawesi", "Sulawesi Selatan", "South East Sulawesi", "Sulawesi Tenggara", "Bali", "West Nusa Tenggara", "Nusa Tenggara Barat", "East Nusa Tenggara", "Nusa Tenggara Timur" -> return "Asia/Makassar"
            "Maluku", "North Maluku", "Maluku Utara", "West Papua", "Papua Barat", "Papua" -> return "Asia/Jayapura"
        }

        "IE" -> return "Europe/Dublin"
        "IL" -> return "Asia/Jerusalem"
        "IM" -> return "Europe/Isle_of_Man"
        "IN" -> return "Asia/Kolkata"
        "IO" -> return "Indian/Chagos"
        "IQ" -> return "Asia/Baghdad"
        "IR" -> return "Asia/Tehran"
        "IS" -> return "Atlantic/Reykjavik"
        "IT" -> return "Europe/Rome"
        "JM" -> return "America/Jamaica"
        "JO" -> return "Asia/Amman"
        "JP" -> return "Asia/Tokyo"
        "KE" -> return "Africa/Nairobi"
        "KG" -> return "Asia/Bishkek"
        "KH" -> return "Asia/Phnom_Penh"
        "KI" -> return if (this.longitude < -178 || this.longitude > 0) "Pacific/Tarawa"
        else if (this.latitude > -2.5 || this.longitude > -158.5) "Pacific/Kiritimati"
        else "Pacific/Kanton"

        "KM" -> return "Indian/Comoro"
        "KN" -> return "America/St_Kitts"
        "KP" -> return "Asia/Pyongyang"
        "KR" -> return "Asia/Seoul"
        "KW" -> return "Asia/Kuwait"
        "KY" -> return "America/Cayman"
        "KZ" -> return when (nonLocalizedAddress.adminArea?.replace(" Province", "")
            ?.replace(" oblısı", "")?.replace("ı", "y")?.replace("ý", "y")) {
            "Kyzylorda", "Qyzylorda" -> "Asia/Qyzylorda"
            "Kostanay", "Qostanay" -> "Asia/Qostanay"
            "Aktobe", "Aqtobe" -> "Asia/Aqtobe"
            "Mangystau" -> "Asia/Aqtau"
            "Atyrau" -> "Asia/Atyrau"
            "West Kazakhstan" -> "Asia/Oral"
            else -> "Asia/Almaty"
        }

        "LA" -> return "Asia/Vientiane"
        "LB" -> return "Asia/Beirut"
        "LC" -> return "America/St_Lucia"
        "LI" -> return "Europe/Vaduz"
        "LK" -> return "Asia/Colombo"
        "LR" -> return "Africa/Monrovia"
        "LS" -> return "Africa/Maseru"
        "LT" -> return "Europe/Vilnius"
        "LU" -> return "Europe/Luxembourg"
        "LV" -> return "Europe/Riga"
        "LY" -> return "Africa/Tripoli"
        "MA" -> return "Africa/Casablanca"
        "MC" -> return "Europe/Monaco"
        "MD" -> return "Europe/Chisinau"
        "ME" -> return "Europe/Podgorica"
        "MF" -> return "America/Marigot"
        "MG" -> return "Indian/Antananarivo"
        "MH" -> return "Pacific/Majuro"
        "MK" -> return "Europe/Skopje"
        "ML" -> return "Africa/Bamako"
        "MM" -> return "Asia/Yangon"
        "MN" -> return when (nonLocalizedAddress.adminArea) {
            "Bayan-Ölgii", "Bayan-Olgiy", "Govi-Altai", "Khovd", "Hovd", "Uvs", "Zavkhan" -> return "Asia/Hovd"
            "Dornod", "Sükhbaatar", "Sukhbaatar" -> return "Asia/Choibalsan"
            else -> "Asia/Ulaanbaatar"
        }

        "MO" -> return "Asia/Macau"
        "MP" -> return "Pacific/Saipan"
        "MQ" -> return "America/Martinique"
        "MR" -> return "Africa/Nouakchott"
        "MS" -> return "America/Montserrat"
        "MT" -> return "Europe/Malta"
        "MU" -> return "Indian/Mauritius"
        "MV" -> return "Indian/Maldives"
        "MW" -> return "Africa/Blantyre"
        "MX" -> return when (nonLocalizedAddress.adminArea?.replace("á", "a")?.replace("é", "e")
            ?.replace("í", "i")?.replace("ó", "o")) {
            "Baja California" -> return "America/Tijuana"
            "Sonora" -> return "America/Hermosillo"
            "Baja California Sur", "Nayarit", "Sinaloa" -> return "America/Mazatlan"
            "Chihuahua" -> return "America/Chihuahua"
            "Quintana Roo" -> return "America/Cancun"
            else -> "America/Mexico_City"
        }

        "MY" -> return "Asia/Kuala_Lumpur"
        "MZ" -> return "Africa/Maputo"
        "NA" -> return "Africa/Windhoek"
        "NC" -> return "Pacific/Noumea"
        "NE" -> return "Africa/Niamey"
        "NF" -> return "Pacific/Norfolk"
        "NG" -> return "Africa/Lagos"
        "NI" -> return "America/Managua"
        "NL" -> return "Europe/Amsterdam"
        "NO" -> return "Europe/Oslo"
        "NP" -> return "Asia/Kathmandu"
        "NR" -> return "Pacific/Nauru"
        "NU" -> return "Pacific/Niue"
        "NZ" -> return if (this.longitude < 0) "Pacific/Chatham" else "Pacific/Auckland"
        "OM" -> return "Asia/Muscat"
        "PA" -> return "America/Panama"
        "PE" -> return "America/Lima"
        "PF" -> return if (this.longitude > -135.3 && this.latitude < -23) "Pacific/Gambier"
        else if (this.longitude > 142.5 && this.latitude > -11) "Pacific/Marquesas"
        else "Pacific/Tahiti"

        "PG" -> return if ((nonLocalizedAddress.adminArea
                ?: "").contains("Bougainville")
        ) "Pacific/Bougainville"
        else "Pacific/Port_Moresby"

        "PH" -> return "Asia/Manila"
        "PK" -> return "Asia/Karachi"
        "PL" -> return "Europe/Warsaw"
        "PM" -> return "America/Miquelon"
        "PN" -> return "Pacific/Pitcairn"
        "PR" -> return "America/Puerto_Rico"
        "PS" -> return "Asia/Hebron"
        "PT" -> return if (this.latitude < 35) "Atlantic/Madeira"
        else if (this.longitude < -15) "Atlantic/Azores"
        else "Europe/Lisbon"

        "PW" -> return "Pacific/Palau"
        "PY" -> return "America/Asuncion"
        "QA" -> return "Asia/Qatar"
        "RE" -> return "Indian/Reunion"
        "RO" -> return "Europe/Bucharest"
        "RS" -> return "Europe/Belgrade"
        "RU" -> {
            when (nonLocalizedAddress.adminArea?.replace("'", "")?.replace("á", "a")
                ?.replace("ó", "o")?.replace("ú", "u")?.replace("Respublika", "")
                ?.replace("Republic of ", "")?.replace("Republic ", "")?.replace("Oblast", "")
                ?.replace("oblast", "")?.replace(" Autonomous Okrug", "")
                ?.replace(" avtonomnyy okrug", "")?.replace("Krai", "")?.replace("kray", "")
                ?.replace("skaya", "")?.replace("skaja", "")?.trim()) {
                "Kaliningrad" -> return "Europe/Kaliningrad"
                "Kirov" -> return "Europe/Kirov"
                "Saratov", "Volgograd", "Astrakhan" -> return "Europe/Volgograd"
                "Samar", "Samara", "Udmurt" -> return "Europe/Samara"
                "Ulyanovsk" -> return "Europe/Ulyanovsk"
                "Bashkortostan", "Chelyabin", "Chelyabinsk", "Khanty-Mansi", "Khanty-Mansiyskiy", "Kurgan", "Orenburg", "Perm", "Permskiy", "Sverdlov", "Sverdlovsk", "Tyumen", "Yamalo-Nenets", "Yamalo-Nenetskiy" -> return "Asia/Yekaterinburg"
                "Altai", "Altay", "Altayskiy", "Om", "Omsk" -> return "Asia/Omsk"
                "Novosibir", "Novosibirsk", "Tom", "Tomsk" -> return "Asia/Novosibirsk"
                "Kemerov", "Kemerovo" -> return "Asia/Novokuznetsk"
                "Khakasiya", "Khakassia", "Krasnoyarsk", "Krasnoyarskiy", "Tuva" -> return "Asia/Krasnoyarsk"
                "Irkut", "Irkutsk", "Buryatia", "Buryatiya" -> return "Asia/Irkutsk"
                "Amur", "Zabaykalsky" -> return "Asia/Yakutsk"
                "Jewish Autonomous", "Evrey avtonomnaya", "Khabarovsk", "Khabarovskiy", "Primorsky", "Primorskiy" -> return "Asia/Vladivostok"
                "Sakhalin" -> return "Asia/Sakhalin"
                "Magadan" -> return "Asia/Magadan"
                "Kamchatka" -> return "Asia/Kamchatka"
                "Chukotka" -> return "Asia/Anadyr"
                "Sakha" -> return if (this.longitude > 142 && this.latitude < 73 && (this.latitude > 65.2 || this.longitude > 146.2)) "Asia/Srednekolymsk"
                else if (this.longitude > 140 && this.latitude < 65.2) "Asia/Ust-Nera"
                else if ((this.longitude > 133.5 && this.latitude > 65.2) || (this.longitude > 131 && this.latitude > 65.5 && this.latitude < 68)) "Asia/Vladivostok"
                else "Asia/Yakutsk"
            }
            if (this.longitude < 68) {
                return "Europe/Moscow"
            }
        }

        "RW" -> return "Africa/Kigali"
        "SA" -> return "Asia/Riyadh"
        "SB" -> return "Pacific/Guadalcanal"
        "SC" -> return "Indian/Mahe"
        "SD" -> return "Africa/Khartoum"
        "SE" -> return "Europe/Stockholm"
        "SG" -> return "Asia/Singapore"
        "SH" -> return "Atlantic/St_Helena"
        "SI" -> return "Europe/Ljubljana"
        "SJ" -> return "Arctic/Longyearbyen"
        "SK" -> return "Europe/Bratislava"
        "SL" -> return "Africa/Freetown"
        "SM" -> return "Europe/San_Marino"
        "SN" -> return "Africa/Dakar"
        "SO" -> return "Africa/Mogadishu"
        "SR" -> return "America/Paramaribo"
        "SS" -> return "Africa/Juba"
        "ST" -> return "Africa/Sao_Tome"
        "SV" -> return "America/El_Salvador"
        "SX" -> return "America/Lower_Princes"
        "SY" -> return "Asia/Damascus"
        "SZ" -> return "Africa/Mbabane"
        "TC" -> return "America/Grand_Turk"
        "TD" -> return "Africa/Ndjamena"
        "TF" -> return "Indian/Kerguelen"
        "TG" -> return "Africa/Lome"
        "TH" -> return "Asia/Bangkok"
        "TJ" -> return "Asia/Dushanbe"
        "TK" -> return "Pacific/Fakaofo"
        "TL" -> return "Asia/Dili"
        "TM" -> return "Asia/Ashgabat"
        "TN" -> return "Africa/Tunis"
        "TO" -> return "Pacific/Tongatapu"
        "TR" -> return "Europe/Istanbul"
        "TT" -> return "America/Port_of_Spain"
        "TV" -> return "Pacific/Funafuti"
        "TW" -> return "Asia/Taipei"
        "TZ" -> return "Africa/Dar_es_Salaam"
        "UA" -> return "Europe/Kiev"
        "UG" -> return "Africa/Kampala"
        "UM" -> return if (this.latitude > 20) "Pacific/Midway"
        else if (this.longitude > 0) "Pacific/Wake"
        else if (this.latitude > 10) "Pacific/Johnston"
        else if (this.longitude < -170) "Etc/GMT+12"    //Howland and Baker Islands
        else "Pacific/Midway"

        "US" -> {
            when (nonLocalizedAddress.adminArea) {
                "Connecticut", "Delaware", "District of Columbia", "Georgia", "Maine", "Maryland", "Massachusetts", "New Hampshire", "New Jersey", "New York", "North Carolina", "Ohio", "Pennsylvania", "Rhode Island", "South Carolina", "Vermont", "Virginia", "West Virginia" -> return "America/New_York"
                "Alabama", "Arkansas", "Illinois", "Iowa", "Louisiana", "Minnesota", "Mississippi", "Missouri", "Oklahoma", "Wisconsin" -> return "America/Chicago"
                "Colorado", "Montana", "New Mexico", "Utah", "Wyoming" -> return "America/Denver"
                "California", "Nevada", "Washington" -> return "America/Los_Angeles"
                "Alaska" -> return if (this.longitude < -169.5 || this.longitude > 0) "America/Adak" else "America/Anchorage"
                "Arizona" -> return if (((this.latitude > 35.22 && this.longitude > -111) || (this.latitude > 35.77 && this.latitude < 36.87 && this.longitude > -111.75)) && !(this.latitude > 35.6 && this.latitude < 36 && this.longitude > -111 && this.longitude < -110.15)) "America/Shiprock" else "America/Phoenix"
                "Florida" -> return if ((-3.99 * this.longitude - 2.02 * this.latitude > 278.862 || -5.81 * this.longitude + 2.8 * this.latitude > 579.039) && (this.latitude > 30 || this.longitude < -84.95)) "America/Chicago" else "America/New_York"
                "Hawaii" -> return "Pacific/Honolulu"
                "Idaho" -> return if (this.latitude > 45.5 && this.longitude < -114) "America/Los_Angeles" else "America/Boise"
                "Indiana" -> return when (nonLocalizedAddress.subAdminArea) {
                    "Jasper County", "Lake County", "LaPorte County", "Newton County", "Porter County", "Gibson County", "Posey County", "Spencer County", "Vanderburgh County", "Warrick County" -> "America/Chicago"
                    "Starke County" -> "America/Indiana/Knox"
                    "Perry County" -> "America/Indiana/Tell_City"
                    "Pulaski County" -> "America/Indiana/Winamac"
                    "Pike County" -> "America/Indiana/Petersburg"
                    "Daviess County", "Dubois County", "Knox County", "Martin County" -> "America/Indiana/Vincennes"
                    "Crawford County" -> "America/Indiana/Marengo"
                    "Clark County", "Floyd County", "Harrison County" -> "America/Kentucky/Louisville"
                    "Switzerland County" -> "America/Indiana/Vevay"
                    else -> "America/Indiana/Indianapolis"
                }

                "Kansas" -> return when (nonLocalizedAddress.subAdminArea) {
                    "Sherman County", "Wallace County", "Greeley County", "Hamilton County" -> "America/Denver"
                    else -> "America/Chicago"
                }

                "Kentucky" -> return when (nonLocalizedAddress.subAdminArea) {
                    "Adair County", "Allen County", "Ballard County", "Barren County", "Breckinridge County", "Butler County", "Caldwell County", "Calloway County", "Carlisle County", "Christian County", "Clinton County", "Crittenden County", "Cumberland County", "Daviess County", "Edmonson County", "Fulton County", "Graves County", "Grayson County", "Green County", "Hancock County", "Hart County", "Henderson County", "Hickman County", "Hopkins County", "Livingston County", "Logan County", "Lyon County", "McCracken County", "McLean County", "Marshall County", "Meade County", "Metcalfe County", "Monroe County", "Muhlenberg County", "Ohio County", "Russell County", "Simpson County", "Todd County", "Trigg County", "Union County", "Warren County", "Webster County" -> "America/Chicago"
                    else -> "America/New_York"
                }

                "Michigan" -> return when (nonLocalizedAddress.subAdminArea) {
                    "Gogebic County", "Iron County", "Dickinson County", "Menominee County" -> "America/Menominee"
                    else -> "America/Detroit"
                }

                "Nebraska" -> return when (nonLocalizedAddress.subAdminArea) {
                    "Arthur County", "Chase County", "Dundy County", "Grant County", "Hooker County", "Keith County", "Perkins County", "Sioux County", "Scotts Bluff County", "Banner County", "Kimball County", "Dawes County", "Box Butte County", "Morrill County", "Cheyenne County", "Sheridan County", "Garden County", "Deuel County" -> "America/Denver"
                    "Cherry County" -> return if (this.longitude < -100.65) "America/Denver" else "America/Chicago"
                    else -> "America/Chicago"
                }

                "North Dakota" -> return when (nonLocalizedAddress.subAdminArea) {
                    "Bowman County", "Adams County", "Slope County", "Hettinger County", "Grant County", "Stark County", "Billings County", "Golden Valley County" -> "America/Denver"
                    "Dunn County", "McKenzie County" -> return if (this.longitude < -102 && this.latitude < 47.45) "America/Denver" else "America/Chicago"
                    else -> "America/Chicago"
                }

                "Oregon" -> return if ("Malheur County" == nonLocalizedAddress.subAdminArea && this.latitude > 42.4) "America/Boise" else "America/Los_Angeles"
                "South Dakota" -> return when (nonLocalizedAddress.subAdminArea) {
                    "Harding County", "Perkins County", "Corson County", "Dewey County", "Ziebach County", "Haakon County", "Jackson County", "Bennett County", "Meade County", "Lawrence County", "Pennington County", "Custer County", "Fall River County", "Oglala Lakota County", "Butte County" -> "America/Denver"
                    else -> "America/Chicago"
                }

                "Tennessee" -> return when (nonLocalizedAddress.subAdminArea) {
                    "Scott County", "Campbell County", "Claiborne County", "Hancock County", "Hawkins County", "Sullivan County", "Johnson County", "Morgan County", "Anderson County", "Union County", "Grainger County", "Hamblen County", "Greene County", "Washington County", "Unicoi County", "Carter County", "Roane County", "Loudon County", "Knox County", "Blount County", "Jefferson County", "Sevier County", "Cocke County", "Rhea County", "Meigs County", "McMinn County", "Monroe County", "Hamilton County", "Bradley County", "Polk County" -> "America/New_York"
                    else -> "America/Chicago"
                }

                "Texas" -> return when (nonLocalizedAddress.subAdminArea) {
                    "El Paso County", "Hudspeth County" -> "America/Denver"
                    else -> "America/Chicago"
                }
            }
        }

        "UY" -> return "America/Montevideo"
        "UZ" -> return "Asia/Tashkent"
        "VA" -> return "Europe/Vatican"
        "VC" -> return "America/St_Vincent"
        "VE" -> return "America/Caracas"
        "VG" -> return "America/Tortola"
        "VI" -> return "America/St_Thomas"
        "VN" -> return "Asia/Ho_Chi_Minh"
        "VU" -> return "Pacific/Efate"
        "WF" -> return "Pacific/Wallis"
        "WS" -> return "Pacific/Apia"
        "YE" -> return "Asia/Aden"
        "YT" -> return "Indian/Mayotte"
        "ZA" -> return "Africa/Johannesburg"
        "ZM" -> return "Africa/Lusaka"
        "ZW" -> return "Africa/Harare"
    }
    //If we can't find the country, take the timezone it would be in based on the longitude
    val utcOffset = (this.longitude / 15.0).roundToInt()
    return if (utcOffset < 0) "Etc/GMT+" + -utcOffset    //There's a minus sign here because the sign of Etc/UTC... timezones is inverted
    else "Etc/GMT" + -utcOffset
}