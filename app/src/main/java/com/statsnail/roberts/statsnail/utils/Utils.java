package com.statsnail.roberts.statsnail.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by Adrian on 24/10/2017.
 */

public final class Utils {

    // returns millisec from string date
    public static long getDateInMillisec(String dateString) throws ParseException {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = dateFormat.parse(dateString);
        return date.getTime();
    }


    // returns a date string of the day after param
    public static String getDatePlusOne(String oldDate) throws ParseException {
//        Timber.d("Date in: " + oldDate + ", plus " + TimeUnit.DAYS.toMillis(1) + ", return: " +
//                getDate(getDateInMillisec(oldDate) + TimeUnit.DAYS.toMillis(1)));
        return getDate(getDateInMillisec(oldDate) + TimeUnit.DAYS.toMillis(1));
    }

    // returns a date string of the day before param
    public static String getDateMinusOne(String oldDate) throws ParseException {
        return getDate(getDateInMillisec(oldDate) - TimeUnit.DAYS.toMillis(1));
    }

    // Returns a date string in yyyy-MM-dd from millisecs
    public static String getDate(long millis) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date(millis);
        return dateFormat.format(date);

    }

    // Returns a time string in hh:mm from millisecs
    public static String getTime(long millis) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date = new Date(millis);

        return dateFormat.format(date);

    }

    // Returns a date string in EEE,MMM dd from millisec
    public static String getPrettyDate(long millis) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        Date date = new Date(millis);

        return dateFormat.format(date);
    }

    // Returns true if the whole hour of time given (next low tide time) is after current time
    public static boolean timeIsAfterNowInclMidnight(String time) {
        //2017-11-12T20:24:00+01:00
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        int year = Integer.valueOf(time.substring(0, 4));
        int month = Integer.valueOf(time.substring(5, 7));
        int day = Integer.valueOf(time.substring(8, 10));

        if (year < currentYear) return false;
        if (month < currentMonth) return false;
        if (day < currentDay) return false;

        int lowTideHours = Integer.valueOf(time.substring(11, 13));
        int currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        boolean notifyOnNext = lowTideHours >= currentHours || day > currentDay || month > currentMonth || year > currentYear;
        Timber.d("notify on next: " + notifyOnNext);
        return notifyOnNext;
    }

    // Returns true if the whole hour of time given (next low tide time) is after current time
    public static boolean timeIsAfterNow(String time) {
        int lowTideHours = Integer.valueOf(time.substring(0, 2));
        int currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minutes = Calendar.getInstance().get(Calendar.MINUTE);
        boolean notifyOnNext = lowTideHours >= currentHours;

        return notifyOnNext;
    }

    // returns true if tomorrow is last day of forecast
    public static boolean isTomorrowLast(String dateString) throws ParseException {
        long now = System.currentTimeMillis();
        long last = now + TimeUnit.DAYS.toMillis(10);
        long testDate = getDateInMillisec(dateString);

        return (testDate + TimeUnit.DAYS.toMillis(1) >= last);
    }

    // Returns a String of remaining time in hours and/or minutes given a time in millisec
    public static String getRemainingTime(long rawTime) {
        long millisLeft = rawTime - System.currentTimeMillis();
        long hoursLeft = TimeUnit.MILLISECONDS.toHours(millisLeft);
        long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(millisLeft - TimeUnit.HOURS.toMillis(hoursLeft));
        return (hoursLeft > 0 ? hoursLeft + "h " + minutesLeft + "m " : minutesLeft + " minutes");
        //(hoursLeft > 1 ? hoursLeft +  "hours" + )
    }

    // Returns a formatted time string from tides APIs (hh:mm)
    public static String getFormattedTime(String rawTime) {
        return rawTime.substring(11, 16);
    }

    // Returns a formatted date string from tides APIs (yyy-mm-dd)
    public static String getFormattedDate(String rawDate) {
        return rawDate.substring(0, 10);
    }


    // Returns current time in format hh:mm
    public static String getTime() {
        int currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minutes = Calendar.getInstance().get(Calendar.MINUTE);
        return currentHours + ":" + minutes;
    }

    public static String getAccuratePlaceName(Context context, LatLng latLng) throws IOException, IndexOutOfBoundsException {

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        String address = addresses.get(0).getAddressLine(0);
        String subAdminArea = addresses.get(0).getSubAdminArea();

        StringBuilder sb = new StringBuilder();
        String place = "";
        if (address.substring(0, 7).equals("Unnamed") ||
                address.substring(0, 2).equals("Fv") ||
                address.substring(0, 2).equals("E1") ||
                address.substring(0, 2).equals("E6") ||
                address.substring(0, 2).equals("Rv"))
            return getPlaceName(addresses);
        for (char c : address.toCharArray()) {
            if (Character.isLetter(c) || Character.isSpaceChar(c))
                sb.append(c);
            else break;
        }
        int possibleSpaceIndex = (sb.length() - 1);
        if (Character.isSpaceChar(sb.charAt(possibleSpaceIndex)))
            sb.deleteCharAt(possibleSpaceIndex);

        place = sb.append(", ").append(subAdminArea).toString();
        return place;
    }

    public static String getAccuratePlaceName(Context context, boolean homeLocation) throws IOException, IndexOutOfBoundsException {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultLat = context.getResources().getString(R.string.default_latitude);
        String defaultLon = context.getResources().getString(R.string.default_longitude);

        String latitude = homeLocation ? preference.getString(MainActivityFull.HOME_LAT, defaultLat) :
                preference.getString(MainActivityFull.EXTRA_LATITUDE, defaultLat);
        String longitude = homeLocation ? preference.getString(MainActivityFull.HOME_LON, defaultLon) :
                preference.getString(MainActivityFull.EXTRA_LONGITUDE, defaultLon);
        LatLng latLng = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
        return getAccuratePlaceName(context, latLng);
    }

    private static String getPlaceName(List<Address> addresses) {
        if (addresses.size() != 0) {
            String subAdmin = addresses.get(0).getSubAdminArea();
            String adminArea = addresses.get(0).getAdminArea();

            return subAdmin != null && adminArea != null ? subAdmin + ", " + adminArea :
                    subAdmin == null && adminArea != null ? adminArea :
                            subAdmin != null && adminArea == null ? subAdmin : "Location unavailable";

        }
        return "Location unavailable";
    }

    public static String getPlaceName(Context context, LatLng latLng) throws IOException {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(latLng.latitude,
                latLng.longitude, 1);

        if (addresses.size() != 0) {
            String subAdmin = addresses.get(0).getSubAdminArea();
            String adminArea = addresses.get(0).getAdminArea();

            return subAdmin != null && adminArea != null ? subAdmin + ", " + adminArea :
                    subAdmin == null && adminArea != null ? adminArea :
                            subAdmin != null && adminArea == null ? subAdmin : "Location unavailable";

        }
        return "Location unavailable";
    }

    public static String getPlaceName(Context context, boolean homeLocation) throws IOException {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultLat = context.getResources().getString(R.string.default_latitude);
        String defaultLon = context.getResources().getString(R.string.default_longitude);

        String latitude = homeLocation ? preference.getString(MainActivityFull.HOME_LAT, defaultLat) :
                preference.getString(MainActivityFull.EXTRA_LATITUDE, defaultLat);
        String longitude = homeLocation ? preference.getString(MainActivityFull.HOME_LON, defaultLon) :
                preference.getString(MainActivityFull.EXTRA_LONGITUDE, defaultLon);
        LatLng latLng = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
        return getPlaceName(context, latLng);
    }

    public static boolean isGPSEnabled(Context mContext) {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Checks internet connection status
     *
     * @param context
     * @return true if the user has a internet connection, false otherwise
     */
    public static boolean workingConnection(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }


}
