package com.statsnail.roberts.statsnail.utils;

import android.content.Context;
import android.icu.text.TimeZoneNames;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.statsnail.roberts.statsnail.sync.FirebaseJobService;

import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by Adrian on 24/10/2017.
 */

public final class Utils {

    public static void updateTidesData(Context contex) {

    }

    public static String getUrlFromLocation(Location location) {
    /*    int year = Calendar.getInstance().get(Calendar.YEAR);
        int month = Calendar.getInstance().get(Calendar.MONTH);
        int date = Calendar.getInstance().get(Calendar.DATE);
        Calendar calendar = new GregorianCalendar(year, month, date);*/
        String language = "en";
        String fromDate = getDate(System.currentTimeMillis());
        long offset = TimeUnit.HOURS.toMillis(24);
        String tillDate = getDate(System.currentTimeMillis() + offset);


        //// fromDate.substring(0, fromDate.length() - 1) + c; // TODO fikse for mnd skift etc
        //    tillDate = "2017-10-30";
        String base = "http://api.sehavniva.no/tideapi.php?lat=" + 63.4581662 +
                "&lon=" + 10.2795140 +
                "&fromtime=" +
                fromDate + "T00%3A00" +
                "&totime=" +
                tillDate +
                "T00%3A00" +
                "&datatype=tab&refcode=cd&place=&file=&lang=" + language + "&interval=60&dst=0&tzone=1&tide_request=locationdata";
        if (location != null)
            base = "http://api.sehavniva.no/tideapi.php?lat=" + //63.4581662 +
                    location.getLatitude() +
                    "&lon=" + //10.2795140 +
                    location.getLongitude() +
                    "&fromtime=" +
                    fromDate + "T00%3A00" +
                    "&totime=" +
                    tillDate +
                    "T00%3A00" +
                    "&datatype=tab&refcode=cd&place=&file=&lang=" + language + "&interval=60&dst=0&tzone=1&tide_request=locationdata";
        Timber.d("URL created: " + base);
        return base;
    }

    public static String getDate(long millis) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date(millis);

        return dateFormat.format(date);

    }
    public static String getTime(long millis) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("hh:mm", Locale.getDefault());
        Date date = new Date(millis);

        return dateFormat.format(date);

    }


    // Returns true if the whole hour of time given (next low tide time) is after current time
    public static boolean timeIsAfterNow(String time) {
        int lowTideHours = Integer.valueOf(time.substring(0, 2));
        int currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minutes = Calendar.getInstance().get(Calendar.MINUTE);
        boolean k = lowTideHours > currentHours;
        Timber.d("lowTideHours " + lowTideHours + " > " + "currentHours " + currentHours + " = " + k);
        return lowTideHours > currentHours;
    }

    public static String getFormattedTime(String rawTime) {
        return rawTime.substring(11, 16);
    }

    // Returns current time in format hh:mm
    public static String getTime() {
        int currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minutes = Calendar.getInstance().get(Calendar.MINUTE);
        return currentHours + ":" + minutes;
    }

    public static String getPlaceDirName(Context context, Location location) throws IOException {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String knownName = addresses.get(0).getFeatureName();


        String slash = "/";
        StringBuilder builder = new StringBuilder();

        return builder.append(country).append(slash).append(state).append(slash).append(city).toString();
    }

    public static String getPlaceName(Context context, Location location) throws IOException {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        String city = addresses.get(0).getLocality();
        Timber.d("Stedsnavn: " + addresses.get(0).getAdminArea() + ", " +
                addresses.get(0).getFeatureName() + ", " + addresses.get(0).getLocality() + ", " +
                addresses.get(0).getPremises() + ", " + addresses.get(0).getSubAdminArea());

        return addresses.get(0).getSubAdminArea() + ", " + addresses.get(0).getAdminArea();
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

        return (networkInfo != null && networkInfo.isConnectedOrConnecting());
    }

}
