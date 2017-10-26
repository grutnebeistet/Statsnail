package com.statsnail.roberts.statsnail.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateFormat;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by Adrian on 24/10/2017.
 */

public class Utils {
    public static String getUrlFromLocation(Location location) {
        String fromDate = getDate();
        int c = Integer.valueOf(fromDate.substring(fromDate.length() - 1)) + 1;

        String tillDate = fromDate.substring(0, fromDate.length() - 1) + c;
        String base = "http://api.sehavniva.no/tideapi.php?lat=" + 63.4581662 +
                //  location.getLatitude() +
                "&lon=" + 10.2795140 +
                //location.getLongitude() +
                "&fromtime=" +
                getDate() + "T00%3A00" +
                "&totime=" +
                tillDate +
                "T00%3A00" +
                "&datatype=tab&refcode=cd&place=&file=&lang=nn&interval=60&dst=1&tzone=1&tide_request=locationdata";
        Timber.d("URL created: " + base);
        return base;
    }

    public static String getDate() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());

        return dateFormat.format(date);

    }

    public static String getFormattedTime(String rawTime) {
        return rawTime.substring(11, 16);
    }

    public static String getPlaceName(Context context, Location location) throws IOException {
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

    public boolean isGPSEnabled(Context mContext) {
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
