package io.github.gustavlindberg99.weather;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.WordUtils;

public abstract class AddressFunctions{
    public static @NonNull String cityNameFromAddress(Context context, Address address){
        if(isNotEmpty(address.getLocality())){
            return fixCapitalization(address.getLocality());
        }
        if(address.getAddressLine(0) != null){
            Matcher matcher = Pattern.compile(",[0-9\\s]*([^0-9,]+)[0-9\\s]*,\\s*" + Pattern.quote(String.valueOf(address.getCountryName())) + "$").matcher(address.getAddressLine(0));
            if(matcher.find()){
                final String toReturn = fixCapitalization(Objects.requireNonNull(matcher.group(1)));
                if(isNotEmpty(toReturn)){
                    return toReturn;
                }
            }
        }
        if(isNotEmpty(address.getSubLocality())){
            return fixCapitalization(address.getSubLocality());
        }
        if(isNotEmpty(address.getSubAdminArea())){
            return fixCapitalization(address.getSubAdminArea());
        }
        if(isNotEmpty(address.getAdminArea())){
            return fixCapitalization(address.getAdminArea());
        }
        if(isNotEmpty(address.getCountryName())){
            return fixCapitalization(address.getCountryName());
        }
        return context.getString(R.string.unknownLocation);
    }

    private static String fixCapitalization(String s){
        if(s.equals(s.toUpperCase()) || s.equals(s.toLowerCase())){
            return WordUtils.capitalizeFully(s.trim());
        }
        return s.trim();
    }

    private static boolean isNotEmpty(@Nullable String s){
        return s != null && !s.trim().isEmpty();
    }

    private static Address nonLocalizedAddress(Context context, Address localizedAddress) throws IOException{
        if(localizedAddress.getLocale().equals(Locale.US)){
            return localizedAddress;
        }
        final List<Address> results = new Geocoder(context, Locale.US).getFromLocation(localizedAddress.getLatitude(), localizedAddress.getLongitude(), 1);
        if(results.size() == 0){
            return localizedAddress;
        }
        return results.get(0);
    }

    public static String timezoneFromAddress(Context context, Address address) throws IOException{    //There are libraries that can do this, but they only check the coordinates which means they have to use a huge database which is slow and can easily run out of memory. This function is more efficient because since we already have the Address object we can find the timezone by looking at the country/region.
        //Source: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
        switch(String.valueOf(address.getCountryCode())){    //Avoid exceptions if the country code is null, see https://stackoverflow.com/a/29873100/4284627. "null" isn't a valid country code so it doesn't matter that it can't distinguish between null and "null".
        case "AD":
            return "Europe/Andorra";
        case "AE":
            return "Asia/Dubai";
        case "AF":
            return "Asia/Kabul";
        case "AG":
            return "America/Antigua";
        case "AI":
            return "America/Anguilla";
        case "AL":
            return "Europe/Tirane";
        case "AM":
            return "Asia/Yerevan";
        case "AO":
            return "Africa/Luanda";
        case "AQ":
            if(address.getLatitude() < -88.75){
                return "Antarctica/South_Pole";
            }
            else if(address.getLongitude() > -90 && address.getLongitude() < -20){
                return "Antarctica/Palmer";
            }
            else if(address.getLongitude() > 160 || address.getLongitude() < -150){
                return "Antarctica/McMurdo";
            }
            else if(address.getLatitude() < -80 || address.getLongitude() < 15){
                return "Antarctica/Troll";
            }
            else if(address.getLongitude() < 45){
                return "Antarctica/Syowa";
            }
            else if(address.getLongitude() < 70){
                return "Antarctica/Mawson";
            }
            else if(address.getLongitude() < 100){
                return "Antarctica/Davis";
            }
            else if(address.getLongitude() < 110 && address.getLatitude() < -78){
                return "Antarctica/Troll";
            }
            else if(address.getLongitude() < 135){
                return "Antarctica/Casey";
            }
            else{
                return "Antarctica/DumontDUrville";
            }
        case "AR":
            switch(String.valueOf(nonLocalizedAddress(context, address).getAdminArea()).replace("á", "a").replace("é", "e").replace("Provincia de ", "").replace(" Province", "")){
            case "Buenos Aires":
                return "America/Argentina/Buenos_Aires";
            case "Catamarca":
            case "Chubut":
                return "America/Argentina/Catamarca";
            case "Jujuy":
                return "America/Argentina/Jujuy";
            case "La Rioja":
                return "America/Argentina/La_Rioja";
            case "Mendoza":
                return "America/Argentina/Mendoza";
            case "Santa Cruz":
                return "America/Argentina/Rio_Gallegos";
            case "Salta":
                return "America/Argentina/Salta";
            case "San Juan":
                return "America/Argentina/San_Juan";
            case "San Luis":
                return "America/Argentina/San_Luis";
            case "Tucuman":
                return "America/Argentina/Tucuman";
            case "Tierra del Fuego":
                return "America/Argentina/Ushuaia";
            default:
                return "America/Argentina/Cordoba";
            }
        case "AS":
            return "Pacific/Pago_Pago";
        case "AT":
            return "Europe/Vienna";
        case "AU":
            if(address.getLatitude() < -50){
                return "Antarctica/Macquarie";
            }
            {
                final Address a = nonLocalizedAddress(context, address);
                switch(String.valueOf(a.getAdminArea())){
                case "South Australia":
                    return "Australia/Adelaide";
                case "Queensland":
                    return "Australia/Brisbane";
                case "Northern Territory":
                    return "Australia/Darwin";
                case "Tasmania":
                    return "Australia/Hobart";
                case "Victoria":
                    return "Australia/Melbourne";
                case "Western Australia":
                    return "Australia/Perth";
                case "New South Wales":
                case "Australian Capital Territory":
                case "Jervis Bay Territory":
                    if(address.getLongitude() < 141.9 && address.getLatitude() > -32.2 && address.getLatitude() < -31.6){
                        return "Australia/Broken_Hill";
                    }
                    else if(a.getLocality().equals("Lord Howe Island")){
                        return "Australia/Lord_Howe";
                    }
                    return "Australia/Sydney";
                }
            }
            break;
        case "AW":
            return "America/Aruba";
        case "AX":
            return "Europe/Mariehamn";
        case "AZ":
            return "Asia/Baku";
        case "BA":
            return "Europe/Sarajevo";
        case "BB":
            return "America/Barbados";
        case "BD":
            return "Asia/Dhaka";
        case "BE":
            return "Europe/Brussels";
        case "BF":
            return "Africa/Ouagadougou";
        case "BG":
            return "Europe/Sofia";
        case "BH":
            return "Asia/Bahrain";
        case "BI":
            return "Africa/Bujumbura";
        case "BJ":
            return "Africa/Porto-Novo";
        case "BL":
            return "America/St_Barthelemy";
        case "BM":
            return "Atlantic/Bermuda";
        case "BN":
            return "Asia/Brunei";
        case "BO":
            return "America/La_Paz";
        case "BQ":
            return "America/Kralendijk";
        case "BR":
            if(address.getLongitude() > -34){
                return "America/Noronha";
            }
            switch(String.valueOf(nonLocalizedAddress(context, address).getAdminArea()).replace("á", "a").replace("ã", "a").replace("í", "i").replace("ô", "o").replace("State of ", "")){
            case "Roraima":
                return "America/Boa_Vista";
            case "Amapa":
            case "Para":
                return "America/Belem";
            case "Amazonas":
                if(-5.33 * address.getLongitude() - 2.41 * address.getLatitude() >= 382.97){
                    return "America/Eirunepe";
                }
                else{
                    return "America/Manaus";
                }
            case "Maranhao":
            case "Piaui":
            case "Ceara":
            case "Rio Grande do Norte":
            case "Paraiba":
                return "America/Fortaleza";
            case "Tocantins":
                return "America/Araguaina";
            case "Acre":
                return "America/Rio_Branco";
            case "Rondonia":
                return "America/Porto_Velho";
            case "Mato Grosso":
                return "America/Cuiaba";
            case "Pernambuco":
                return "America/Recife";
            case "Bahia":
                return "America/Bahia";
            case "Alagoas":
            case "Sergipe":
                return "America/Maceio";
            case "Mato Grosso do Sul":
                return "America/Campo_Grande";
            default:
                return "America/Sao_Paulo";
            }
        case "BS":
            return "America/Nassau";
        case "BT":
            return "Asia/Thimphu";
        case "BW":
            return "Africa/Gaborone";
        case "BY":
            return "Europe/Minsk";
        case "BZ":
            return "America/Belize";
        case "CA":{
            final Address a = nonLocalizedAddress(context, address);
            switch(String.valueOf(a.getAdminArea()).replace("é", "e")){
            case "Prince Edward Island":
            case "Nova Scotia":
            case "New Brunswick":
                return "America/Halifax";
            case "Manitoba":
                return "America/Winnipeg";
            case "Alberta":
                return "America/Edmonton";
            case "Yukon":
                return "America/Whitehorse";
            case "Northwest Territories":
                return "America/Yellowknife";
            case "Saskatchewan":
                if(address.getLatitude() < 53.55 && address.getLatitude() > 53 && address.getLongitude() < -109.55){
                    return "America/Edmonton";
                }
                return "America/Regina";
            case "Nunavut":
                if("Coral Harbour".equals(a.getLocality())){
                    return "America/Coral_Harbour";
                }
                if(address.getLongitude() > -85 || "Arctic Bay".equals(a.getLocality())){
                    return "America/Iqaluit";
                }
                else if(address.getLongitude() < -102 || (address.getLatitude() > 67 && address.getLatitude() < 73 && address.getLongitude() < -89)){
                    return "America/Cambridge_Bay";
                }
                return "America/Rankin_Inlet";
            case "Newfoundland and Labrador":
                if(address.getLongitude() > -57.1 && address.getLatitude() < 53.5){
                    return "America/St_Johns";
                }
                return "America/Goose_Bay";
            case "Quebec":
                if(address.getLongitude() > -51.6){
                    return "America/Blanc-Sablon";
                }
                return "America/Montreal";
            case "Ontario":
                if(address.getLongitude() > -90){
                    if(address.getLatitude() > 52.9 && address.getLatitude() < 54 && address.getLongitude() < -88){
                        return "America/Winnipeg";
                    }
                    else{
                        return "America/Toronto";
                    }
                }
                else{
                    if(address.getLongitude() > -91 && address.getLatitude() > 51 && address.getLatitude() < 51.7){
                        return "America/Atikokan";
                    }
                    else if(address.getLongitude() > -91 && address.getLatitude() < 49.2){
                        return "America/Toronto";
                    }
                    else if(address.getLongitude() > -92 && address.getLatitude() < 49){
                        return "America/Atikokan";
                    }
                    else{
                        return "America/Winnipeg";
                    }
                }
            case "British Columbia":
                if(address.getLatitude() < 50 && address.getLongitude() > -116.88 && (address.getLongitude() < -116.4 || (address.getLatitude() < 49.3 && address.getLongitude() < -116))){
                    return "America/Creston";
                }
                else if(address.getLongitude() > -116.88 || (address.getLatitude() > 51 && address.getLongitude() > -117.7)){
                    return "America/Edmonton";
                }
                else if(-6.24 * address.getLongitude() - 8.63 * address.getLatitude() < 284.65){
                    return "America/Fort_Nelson";
                }
                return "America/Vancouver";
            }
            break;
        }
        case "CC":
            return "Indian/Cocos";
        case "CD":
            return "Africa/Kinshasa";
        case "CF":
            return "Africa/Bangui";
        case "CG":
            return "Africa/Brazzaville";
        case "CH":
            return "Europe/Zurich";
        case "CI":
            return "Africa/Abidjan";
        case "CK":
            return "Pacific/Rarotonga";
        case "CL":
            if(address.getLongitude() < -90){
                return "Pacific/Easter";
            }
            if(String.valueOf(nonLocalizedAddress(context, address).getAdminArea()).contains("Magallanes")){
                return "America/Punta_Arenas";
            }
            return "America/Santiago";
        case "CM":
            return "Africa/Douala";
        case "CN":
            return "Asia/Shanghai";
        case "CO":
            return "America/Bogota";
        case "CR":
            return "America/Costa_Rica";
        case "CU":
            return "America/Havana";
        case "CV":
            return "Atlantic/Cape_Verde";
        case "CW":
            return "America/Curacao";
        case "CX":
            return "Indian/Christmas";
        case "CY":
            return "Asia/Nicosia";
        case "CZ":
            return "Europe/Prague";
        case "DE":
            return "Europe/Berlin";
        case "DJ":
            return "Africa/Djibouti";
        case "DK":
            return "Europe/Copenhagen";
        case "DM":
            return "America/Dominica";
        case "DO":
            return "America/Santo_Domingo";
        case "DZ":
            return "Africa/Algiers";
        case "EC":
            if(address.getLongitude() < -85){
                return "Pacific/Galapagos";
            }
            return "America/Guayaquil";
        case "EE":
            return "Europe/Tallinn";
        case "EG":
            return "Africa/Cairo";
        case "EH":
            return "Africa/El_Aaiun";
        case "ER":
            return "Africa/Asmara";
        case "ES":
            if(address.getLongitude() < -10){
                return "Atlantic/Canary";
            }
            return "Europe/Madrid";
        case "ET":
            return "Africa/Addis_Ababa";
        case "FI":
            return "Europe/Helsinki";
        case "FJ":
            return "Pacific/Fiji";
        case "FK":
            return "Atlantic/Stanley";
        case "FM":
            if(address.getLongitude() > 154){
                return "Pacific/Pohnpei";
            }
            return "Pacific/Chuuk";
        case "FO":
            return "Atlantic/Faroe";
        case "FR":
            return "Europe/Paris";
        case "GA":
            return "Africa/Libreville";
        case "GB":
            return "Europe/London";
        case "GD":
            return "America/Grenada";
        case "GE":
            return "Asia/Tbilisi";
        case "GF":
            return "America/Cayenne";
        case "GG":
            return "Europe/Guernsey";
        case "GH":
            return "Africa/Accra";
        case "GI":
            return "Europe/Gibraltar";
        case "GL":
            if(address.getLongitude() > -32 && address.getLatitude() > 70 && address.getLatitude() < 74){
                return "America/Scoresbysund";
            }
            else if(address.getLongitude() > -23 && address.getLatitude() > 75 && address.getLatitude() < 78){
                return "America/Danmarkshavn";
            }
            else if(address.getLongitude() < -66){
                return "America/Thule";
            }
            return "America/Nuuk";
        case "GM":
            return "Africa/Banjul";
        case "GN":
            return "Africa/Conakry";
        case "GP":
            return "America/Guadeloupe";
        case "GQ":
            return "Africa/Malabo";
        case "GR":
            return "Europe/Athens";
        case "GS":
            return "Atlantic/South_Georgia";
        case "GT":
            return "America/Guatemala";
        case "GU":
            return "Pacific/Guam";
        case "GW":
            return "Africa/Bissau";
        case "GY":
            return "America/Guyana";
        case "HK":
            return "Asia/Hong_Kong";
        case "HN":
            return "America/Tegucigalpa";
        case "HR":
            return "Europe/Zagreb";
        case "HT":
            return "America/Port-au-Prince";
        case "HU":
            return "Europe/Budapest";
        case "ID":
            switch(String.valueOf(nonLocalizedAddress(context, address).getAdminArea())){
            case "Aceh":
            case "Bengkulu":
            case "Jambi":
            case "Lampung":
            case "North Sumatra":
            case "Sumatera Utara":
            case "Riau":
            case "South Sumatra":
            case "Sumatera Selatan":
            case "West Sumatra":
            case "Sumatera Barat":
            case "Riau Islands":
            case "Kepulauan Riau":
            case "Bangka Belitung Islands":
            case "Kepulauan Bangka Belitung":
            case "Banten":
            case "Jakarta":
            case "Daerah Khusus Ibukota Jakarta":
            case "West Java":
            case "Jawa Barat":
            case "Central Java":
            case "Jawa Tengah":
            case "Special Region of Yogyakarta":
            case "Daerah Istimewa Yogyakarta":
            case "East Java":
            case "Jawa Timur":
                return "Asia/Jakarta";
            case "West Kalimantan":
            case "Kalimantan Barat":
            case "Central Kalimantan":
            case "Kalimantan Tengah":
                return "Asia/Pontianak";
            case "South Kalimantan":
            case "Kalimantan Selatan":
            case "East Kalimantan":
            case "Kalimantan Timur":
            case "North Kalimantan":
            case "Kalimantan Utara":
            case "North Sulawesi":
            case "Sulawesi Utara":
            case "Gorontalo":
            case "Central Sulawesi":
            case "Sulawesi Tengah":
            case "West Sulawesi":
            case "Sulawesi Barat":
            case "South Sulawesi":
            case "Sulawesi Selatan":
            case "South East Sulawesi":
            case "Sulawesi Tenggara":
            case "Bali":
            case "West Nusa Tenggara":
            case "Nusa Tenggara Barat":
            case "East Nusa Tenggara":
            case "Nusa Tenggara Timur":
                return "Asia/Makassar";
            case "Maluku":
            case "North Maluku":
            case "Maluku Utara":
            case "West Papua":
            case "Papua Barat":
            case "Papua":
                return "Asia/Jayapura";
            }
            break;
        case "IE":
            return "Europe/Dublin";
        case "IL":
            return "Asia/Jerusalem";
        case "IM":
            return "Europe/Isle_of_Man";
        case "IN":
            return "Asia/Kolkata";
        case "IO":
            return "Indian/Chagos";
        case "IQ":
            return "Asia/Baghdad";
        case "IR":
            return "Asia/Tehran";
        case "IS":
            return "Atlantic/Reykjavik";
        case "IT":
            return "Europe/Rome";
        case "JM":
            return "America/Jamaica";
        case "JO":
            return "Asia/Amman";
        case "JP":
            return "Asia/Tokyo";
        case "KE":
            return "Africa/Nairobi";
        case "KG":
            return "Asia/Bishkek";
        case "KH":
            return "Asia/Phnom_Penh";
        case "KI":
            if(address.getLongitude() < -178 || address.getLongitude() > 0){
                return "Pacific/Tarawa";
            }
            else if(address.getLatitude() > -2.5 || address.getLongitude() > -158.5){
                return "Pacific/Kiritimati";
            }
            return "Pacific/Kanton";
        case "KM":
            return "Indian/Comoro";
        case "KN":
            return "America/St_Kitts";
        case "KP":
            return "Asia/Pyongyang";
        case "KR":
            return "Asia/Seoul";
        case "KW":
            return "Asia/Kuwait";
        case "KY":
            return "America/Cayman";
        case "KZ":
            switch(String.valueOf(nonLocalizedAddress(context, address).getAdminArea()).replace(" Province", "").replace(" oblısı", "").replace("ı", "y").replace("ý", "y")){
            case "Kyzylorda":
            case "Qyzylorda":
                return "Asia/Qyzylorda";
            case "Kostanay":
            case "Qostanay":
                return "Asia/Qostanay";
            case "Aktobe":
            case "Aqtobe":
                return "Asia/Aqtobe";
            case "Mangystau":
                return "Asia/Aqtau";
            case "Atyrau":
                return "Asia/Atyrau";
            case "West Kazakhstan":
                return "Asia/Oral";
            }
            return "Asia/Almaty";
        case "LA":
            return "Asia/Vientiane";
        case "LB":
            return "Asia/Beirut";
        case "LC":
            return "America/St_Lucia";
        case "LI":
            return "Europe/Vaduz";
        case "LK":
            return "Asia/Colombo";
        case "LR":
            return "Africa/Monrovia";
        case "LS":
            return "Africa/Maseru";
        case "LT":
            return "Europe/Vilnius";
        case "LU":
            return "Europe/Luxembourg";
        case "LV":
            return "Europe/Riga";
        case "LY":
            return "Africa/Tripoli";
        case "MA":
            return "Africa/Casablanca";
        case "MC":
            return "Europe/Monaco";
        case "MD":
            return "Europe/Chisinau";
        case "ME":
            return "Europe/Podgorica";
        case "MF":
            return "America/Marigot";
        case "MG":
            return "Indian/Antananarivo";
        case "MH":
            return "Pacific/Majuro";
        case "MK":
            return "Europe/Skopje";
        case "ML":
            return "Africa/Bamako";
        case "MM":
            return "Asia/Yangon";
        case "MN":
            switch(String.valueOf(nonLocalizedAddress(context, address).getAdminArea())){
            case "Bayan-Ölgii":
            case "Bayan-Olgiy":
            case "Govi-Altai":
            case "Khovd":
            case "Hovd":
            case "Uvs":
            case "Zavkhan":
                return "Asia/Hovd";
            case "Dornod":
            case "Sükhbaatar":
            case "Sukhbaatar":
                return "Asia/Choibalsan";
            }
            return "Asia/Ulaanbaatar";
        case "MO":
            return "Asia/Macau";
        case "MP":
            return "Pacific/Saipan";
        case "MQ":
            return "America/Martinique";
        case "MR":
            return "Africa/Nouakchott";
        case "MS":
            return "America/Montserrat";
        case "MT":
            return "Europe/Malta";
        case "MU":
            return "Indian/Mauritius";
        case "MV":
            return "Indian/Maldives";
        case "MW":
            return "Africa/Blantyre";
        case "MX":
            switch(String.valueOf(nonLocalizedAddress(context, address).getAdminArea()).replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o")){
            case "Baja California":
                return "America/Tijuana";
            case "Sonora":
                return "America/Hermosillo";
            case "Baja California Sur":
            case "Nayarit":
            case "Sinaloa":
                return "America/Mazatlan";
            case "Chihuahua":
                return "America/Chihuahua";
            case "Quintana Roo":
                return "America/Cancun";
            }
            return "America/Mexico_City";
        case "MY":
            return "Asia/Kuala_Lumpur";
        case "MZ":
            return "Africa/Maputo";
        case "NA":
            return "Africa/Windhoek";
        case "NC":
            return "Pacific/Noumea";
        case "NE":
            return "Africa/Niamey";
        case "NF":
            return "Pacific/Norfolk";
        case "NG":
            return "Africa/Lagos";
        case "NI":
            return "America/Managua";
        case "NL":
            return "Europe/Amsterdam";
        case "NO":
            return "Europe/Oslo";
        case "NP":
            return "Asia/Kathmandu";
        case "NR":
            return "Pacific/Nauru";
        case "NU":
            return "Pacific/Niue";
        case "NZ":
            if(address.getLongitude() < 0){
                return "Pacific/Chatham";
            }
            return "Pacific/Auckland";
        case "OM":
            return "Asia/Muscat";
        case "PA":
            return "America/Panama";
        case "PE":
            return "America/Lima";
        case "PF":
            if(address.getLongitude() > -135.3 && address.getLatitude() < -23){
                return "Pacific/Gambier";
            }
            else if(address.getLongitude() > 142.5 && address.getLatitude() > -11){
                return "Pacific/Marquesas";
            }
            return "Pacific/Tahiti";
        case "PG":
            if(String.valueOf(nonLocalizedAddress(context, address).getAdminArea()).contains("Bougainville")){
                return "Pacific/Bougainville";
            }
            return "Pacific/Port_Moresby";
        case "PH":
            return "Asia/Manila";
        case "PK":
            return "Asia/Karachi";
        case "PL":
            return "Europe/Warsaw";
        case "PM":
            return "America/Miquelon";
        case "PN":
            return "Pacific/Pitcairn";
        case "PR":
            return "America/Puerto_Rico";
        case "PS":
            return "Asia/Hebron";
        case "PT":
            if(address.getLatitude() < 35){
                return "Atlantic/Madeira";
            }
            else if(address.getLongitude() < -15){
                return "Atlantic/Azores";
            }
            return "Europe/Lisbon";
        case "PW":
            return "Pacific/Palau";
        case "PY":
            return "America/Asuncion";
        case "QA":
            return "Asia/Qatar";
        case "RE":
            return "Indian/Reunion";
        case "RO":
            return "Europe/Bucharest";
        case "RS":
            return "Europe/Belgrade";
        case "RU":
            switch(String.valueOf(nonLocalizedAddress(context, address).getAdminArea()).replace("'", "").replace("á", "a").replace("ó", "o").replace("ú", "u").replace("Respublika", "").replace("Republic of ", "").replace("Republic ", "").replace("Oblast", "").replace("oblast", "").replace(" Autonomous Okrug", "").replace(" avtonomnyy okrug", "").replace("Krai", "").replace("kray", "").replace("skaya", "").replace("skaja", "").trim()){
            case "Kaliningrad":
                return "Europe/Kaliningrad";
            case "Kirov":
                return "Europe/Kirov";
            case "Saratov":
            case "Volgograd":
            case "Astrakhan":
                return "Europe/Volgograd";
            case "Samar":
            case "Samara":
            case "Udmurt":
                return "Europe/Samara";
            case "Ulyanovsk":
                return "Europe/Ulyanovsk";
            case "Bashkortostan":
            case "Chelyabin":
            case "Chelyabinsk":
            case "Khanty-Mansi":
            case "Khanty-Mansiyskiy":
            case "Kurgan":
            case "Orenburg":
            case "Perm":
            case "Permskiy":
            case "Sverdlov":
            case "Sverdlovsk":
            case "Tyumen":
            case "Yamalo-Nenets":
            case "Yamalo-Nenetskiy":
                return "Asia/Yekaterinburg";
            case "Altai":
            case "Altay":
            case "Altayskiy":
            case "Om":
            case "Omsk":
                return "Asia/Omsk";
            case "Novosibir":
            case "Novosibirsk":
            case "Tom":
            case "Tomsk":
                return "Asia/Novosibirsk";
            case "Kemerov":
            case "Kemerovo":
                return "Asia/Novokuznetsk";
            case "Khakasiya":
            case "Khakassia":
            case "Krasnoyarsk":
            case "Krasnoyarskiy":
            case "Tuva":
                return "Asia/Krasnoyarsk";
            case "Irkut":
            case "Irkutsk":
            case "Buryatia":
            case "Buryatiya":
                return "Asia/Irkutsk";
            case "Amur":
            case "Zabaykalsky":
                return "Asia/Yakutsk";
            case "Jewish Autonomous":
            case "Evrey avtonomnaya":
            case "Khabarovsk":
            case "Khabarovskiy":
            case "Primorsky":
            case "Primorskiy":
                return "Asia/Vladivostok";
            case "Sakhalin":
                return "Asia/Sakhalin";
            case "Magadan":
                return "Asia/Magadan";
            case "Kamchatka":
                return "Asia/Kamchatka";
            case "Chukotka":
                return "Asia/Anadyr";
            case "Sakha":
                if(address.getLongitude() > 142 && address.getLatitude() < 73 && (address.getLatitude() > 65.2 || address.getLongitude() > 146.2)){
                    return "Asia/Srednekolymsk";
                }
                else if(address.getLongitude() > 140 && address.getLatitude() < 65.2){
                    return "Asia/Ust-Nera";
                }
                else if((address.getLongitude() > 133.5 && address.getLatitude() > 65.2) || (address.getLongitude() > 131 && address.getLatitude() > 65.5 && address.getLatitude() < 68)){
                    return "Asia/Vladivostok";
                }
                return "Asia/Yakutsk";
            }
            if(address.getLongitude() < 68){
                return "Europe/Moscow";
            }
            break;
        case "RW":
            return "Africa/Kigali";
        case "SA":
            return "Asia/Riyadh";
        case "SB":
            return "Pacific/Guadalcanal";
        case "SC":
            return "Indian/Mahe";
        case "SD":
            return "Africa/Khartoum";
        case "SE":
            return "Europe/Stockholm";
        case "SG":
            return "Asia/Singapore";
        case "SH":
            return "Atlantic/St_Helena";
        case "SI":
            return "Europe/Ljubljana";
        case "SJ":
            return "Arctic/Longyearbyen";
        case "SK":
            return "Europe/Bratislava";
        case "SL":
            return "Africa/Freetown";
        case "SM":
            return "Europe/San_Marino";
        case "SN":
            return "Africa/Dakar";
        case "SO":
            return "Africa/Mogadishu";
        case "SR":
            return "America/Paramaribo";
        case "SS":
            return "Africa/Juba";
        case "ST":
            return "Africa/Sao_Tome";
        case "SV":
            return "America/El_Salvador";
        case "SX":
            return "America/Lower_Princes";
        case "SY":
            return "Asia/Damascus";
        case "SZ":
            return "Africa/Mbabane";
        case "TC":
            return "America/Grand_Turk";
        case "TD":
            return "Africa/Ndjamena";
        case "TF":
            return "Indian/Kerguelen";
        case "TG":
            return "Africa/Lome";
        case "TH":
            return "Asia/Bangkok";
        case "TJ":
            return "Asia/Dushanbe";
        case "TK":
            return "Pacific/Fakaofo";
        case "TL":
            return "Asia/Dili";
        case "TM":
            return "Asia/Ashgabat";
        case "TN":
            return "Africa/Tunis";
        case "TO":
            return "Pacific/Tongatapu";
        case "TR":
            return "Europe/Istanbul";
        case "TT":
            return "America/Port_of_Spain";
        case "TV":
            return "Pacific/Funafuti";
        case "TW":
            return "Asia/Taipei";
        case "TZ":
            return "Africa/Dar_es_Salaam";
        case "UA":
            return "Europe/Kiev";
        case "UG":
            return "Africa/Kampala";
        case "UM":
            if(address.getLatitude() > 20){
                return "Pacific/Midway";
            }
            else if(address.getLongitude() > 0){
                return "Pacific/Wake";
            }
            else if(address.getLatitude() > 10){
                return "Pacific/Johnston";
            }
            else if(address.getLongitude() < -170){
                return "Etc/GMT+12";    //Howland and Baker Islands
            }
            return "Pacific/Midway";
        case "US":{
            final Address a = nonLocalizedAddress(context, address);
            switch(a.getAdminArea()){
            case "Connecticut":
            case "Delaware":
            case "District of Columbia":
            case "Georgia":
            case "Maine":
            case "Maryland":
            case "Massachusetts":
            case "New Hampshire":
            case "New Jersey":
            case "New York":
            case "North Carolina":
            case "Ohio":
            case "Pennsylvania":
            case "Rhode Island":
            case "South Carolina":
            case "Vermont":
            case "Virginia":
            case "West Virginia":
                return "America/New_York";
            case "Alabama":
            case "Arkansas":
            case "Illinois":
            case "Iowa":
            case "Louisiana":
            case "Minnesota":
            case "Mississippi":
            case "Missouri":
            case "Oklahoma":
            case "Wisconsin":
                return "America/Chicago";
            case "Colorado":
            case "Montana":
            case "New Mexico":
            case "Utah":
            case "Wyoming":
                return "America/Denver";
            case "California":
            case "Nevada":
            case "Washington":
                return "America/Los_Angeles";
            case "Alaska":
                if(address.getLongitude() < -169.5 || address.getLongitude() > 0){
                    return "America/Adak";
                }
                return "America/Anchorage";
            case "Arizona":
                if(((address.getLatitude() > 35.22 && address.getLongitude() > -111) || (address.getLatitude() > 35.77 && address.getLatitude() < 36.87 && address.getLongitude() > -111.75)) && !(address.getLatitude() > 35.6 && address.getLatitude() < 36 && address.getLongitude() > -111 && address.getLongitude() < -110.15)){
                    return "America/Shiprock";
                }
                return "America/Phoenix";
            case "Florida":
                if((-3.99 * address.getLongitude() - 2.02 * address.getLatitude() > 278.862 || -5.81 * address.getLongitude() + 2.8 * address.getLatitude() > 579.039) && (address.getLatitude() > 30 || address.getLongitude() < -84.95)){
                    return "America/Chicago";
                }
                return "America/New_York";
            case "Hawaii":
                return "Pacific/Honolulu";
            case "Idaho":
                if(address.getLatitude() > 45.5 && address.getLongitude() < -114){
                    return "America/Los_Angeles";
                }
                return "America/Boise";
            case "Indiana":
                switch(a.getSubAdminArea()){
                case "Jasper County":
                case "Lake County":
                case "LaPorte County":
                case "Newton County":
                case "Porter County":
                case "Gibson County":
                case "Posey County":
                case "Spencer County":
                case "Vanderburgh County":
                case "Warrick County":
                    return "America/Chicago";
                case "Starke County":
                    return "America/Indiana/Knox";
                case "Perry County":
                    return "America/Indiana/Tell_City";
                case "Pulaski County":
                    return "America/Indiana/Winamac";
                case "Pike County":
                    return "America/Indiana/Petersburg";
                case "Daviess County":
                case "Dubois County":
                case "Knox County":
                case "Martin County":
                    return "America/Indiana/Vincennes";
                case "Crawford County":
                    return "America/Indiana/Marengo";
                case "Clark County":
                case "Floyd County":
                case "Harrison County":
                    return "America/Kentucky/Louisville";
                case "Switzerland County":
                    return "America/Indiana/Vevay";
                }
                return "America/Indiana/Indianapolis";
            case "Kansas":
                switch(a.getSubAdminArea()){
                case "Sherman County":
                case "Wallace County":
                case "Greeley County":
                case "Hamilton County":
                    return "America/Denver";
                }
                return "America/Chicago";
            case "Kentucky":
                switch(a.getSubAdminArea()){
                case "Adair County":
                case "Allen County":
                case "Ballard County":
                case "Barren County":
                case "Breckinridge County":
                case "Butler County":
                case "Caldwell County":
                case "Calloway County":
                case "Carlisle County":
                case "Christian County":
                case "Clinton County":
                case "Crittenden County":
                case "Cumberland County":
                case "Daviess County":
                case "Edmonson County":
                case "Fulton County":
                case "Graves County":
                case "Grayson County":
                case "Green County":
                case "Hancock County":
                case "Hart County":
                case "Henderson County":
                case "Hickman County":
                case "Hopkins County":
                case "Livingston County":
                case "Logan County":
                case "Lyon County":
                case "McCracken County":
                case "McLean County":
                case "Marshall County":
                case "Meade County":
                case "Metcalfe County":
                case "Monroe County":
                case "Muhlenberg County":
                case "Ohio County":
                case "Russell County":
                case "Simpson County":
                case "Todd County":
                case "Trigg County":
                case "Union County":
                case "Warren County":
                case "Webster County":
                    return "America/Chicago";
                }
                return "America/New_York";
            case "Michigan":
                switch(a.getSubAdminArea()){
                case "Gogebic County":
                case "Iron County":
                case "Dickinson County":
                case "Menominee County":
                    return "America/Menominee";
                }
                return "America/Detroit";
            case "Nebraska":
                switch(a.getSubAdminArea()){
                case "Arthur County":
                case "Chase County":
                case "Dundy County":
                case "Grant County":
                case "Hooker County":
                case "Keith County":
                case "Perkins County":
                case "Sioux County":
                case "Scotts Bluff County":
                case "Banner County":
                case "Kimball County":
                case "Dawes County":
                case "Box Butte County":
                case "Morrill County":
                case "Cheyenne County":
                case "Sheridan County":
                case "Garden County":
                case "Deuel County":
                    return "America/Denver";
                case "Cherry County":
                    if(address.getLongitude() < -100.65){
                        return "America/Denver";
                    }
                }
                return "America/Chicago";
            case "North Dakota":
                switch(a.getSubAdminArea()){
                case "Bowman County":
                case "Adams County":
                case "Slope County":
                case "Hettinger County":
                case "Grant County":
                case "Stark County":
                case "Billings County":
                case "Golden Valley County":
                    return "America/Denver";
                case "Dunn County":
                case "McKenzie County":
                    if(address.getLongitude() < -102 && address.getLatitude() < 47.45){
                        return "America/Denver";
                    }
                }
                return "America/Chicago";
            case "Oregon":
                if("Malheur County".equals(a.getSubAdminArea()) && address.getLatitude() > 42.4){
                    return "America/Boise";
                }
                return "America/Los_Angeles";
            case "South Dakota":
                switch(a.getSubAdminArea()){
                case "Harding County":
                case "Perkins County":
                case "Corson County":
                case "Dewey County":
                case "Ziebach County":
                case "Haakon County":
                case "Jackson County":
                case "Bennett County":
                case "Meade County":
                case "Lawrence County":
                case "Pennington County":
                case "Custer County":
                case "Fall River County":
                case "Oglala Lakota County":
                case "Butte County":
                    return "America/Denver";
                }
                return "America/Chicago";
            case "Tennessee":
                switch(a.getSubAdminArea()){
                case "Scott County":
                case "Campbell County":
                case "Claiborne County":
                case "Hancock County":
                case "Hawkins County":
                case "Sullivan County":
                case "Johnson County":
                case "Morgan County":
                case "Anderson County":
                case "Union County":
                case "Grainger County":
                case "Hamblen County":
                case "Greene County":
                case "Washington County":
                case "Unicoi County":
                case "Carter County":
                case "Roane County":
                case "Loudon County":
                case "Knox County":
                case "Blount County":
                case "Jefferson County":
                case "Sevier County":
                case "Cocke County":
                case "Rhea County":
                case "Meigs County":
                case "McMinn County":
                case "Monroe County":
                case "Hamilton County":
                case "Bradley County":
                case "Polk County":
                    return "America/New_York";
                }
                return "America/Chicago";
            case "Texas":
                switch(a.getSubAdminArea()){
                case "El Paso County":
                case "Hudspeth County":
                    return "America/Denver";
                }
                return "America/Chicago";
            }
            break;
        }
        case "UY":
            return "America/Montevideo";
        case "UZ":
            return "Asia/Tashkent";
        case "VA":
            return "Europe/Vatican";
        case "VC":
            return "America/St_Vincent";
        case "VE":
            return "America/Caracas";
        case "VG":
            return "America/Tortola";
        case "VI":
            return "America/St_Thomas";
        case "VN":
            return "Asia/Ho_Chi_Minh";
        case "VU":
            return "Pacific/Efate";
        case "WF":
            return "Pacific/Wallis";
        case "WS":
            return "Pacific/Apia";
        case "YE":
            return "Asia/Aden";
        case "YT":
            return "Indian/Mayotte";
        case "ZA":
            return "Africa/Johannesburg";
        case "ZM":
            return "Africa/Lusaka";
        case "ZW":
            return "Africa/Harare";
        }
        //If we can't find the country, take the timezone it would be in based on the longitude
        final long utcOffset = Math.round(address.getLongitude() / 15.0);
        if(utcOffset < 0){
            return "Etc/GMT+" + (-utcOffset);    //There's a minus sign here because the sign of Etc/UTC... timezones is inverted
        }
        else{
            return "Etc/GMT" + (-utcOffset);
        }
    }
}
