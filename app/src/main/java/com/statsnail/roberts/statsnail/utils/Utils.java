package com.statsnail.roberts.statsnail.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.TimeZoneNames;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.data.TidesContract;
import com.statsnail.roberts.statsnail.models.TidesData;
import com.statsnail.roberts.statsnail.sync.FirebaseJobService;
import com.statsnail.roberts.statsnail.sync.NotifyService;

import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Adrian on 24/10/2017.
 */

public final class Utils {

    private final static String PREVIOUS_NOTIFICATION_TIME = "prev_not";

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
    public static boolean timeIsAfterNow(String time) {
        int lowTideHours = Integer.valueOf(time.substring(0, 2));
        int currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minutes = Calendar.getInstance().get(Calendar.MINUTE);
        boolean notifyOnNext = lowTideHours >= currentHours;
        Timber.d("lowTideHours " + lowTideHours + " > " + "currentHours " + currentHours + " = " + notifyOnNext);
        return lowTideHours >= currentHours;
    }
    // returns true if tomorrow is last day of forecast
    public static boolean isTomorrowLast(String dateString) throws ParseException {
        long now = System.currentTimeMillis();
        long last = now + TimeUnit.DAYS.toMillis(6);
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

    public static String getPlaceDirName(Context context, Location location) throws IOException, IndexOutOfBoundsException {
        Timber.d("Location null? " + (location == null));
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String fea = addresses.get(0).getFeatureName();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String loc = addresses.get(0).getLocality();
        String ka = addresses.get(0).getSubLocality();
        String kairp = addresses.get(0).getPhone();
        String subAdminArea = addresses.get(0).getSubAdminArea();

        String slash = "/";
        StringBuilder builder = new StringBuilder();

        return builder.append(country).append(slash).append(state).append(slash).append(subAdminArea)
                .append(slash).append(ka).append(kairp).append(slash).append(slash).append(loc).append(slash).append(fea).append(slash).append(address).toString();
    }

    public static String getPlaceName(Context context) throws IOException {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        String latitude = preference.getString(
                MainActivityFull.EXTRA_LATITUDE, context.getResources().getString(R.string.default_latitude));
        Timber.d("LAT i getPlaceName: " + latitude);
        String longitude = preference.getString(
                MainActivityFull.EXTRA_LONGITUDE, context.getResources().getString(R.string.default_longitude));

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(Double.valueOf(latitude), Double.valueOf(longitude), 1);

        if (addresses.size() != 0) {
            String subAdmin = addresses.get(0).getSubAdminArea();
            String adminArea = addresses.get(0).getAdminArea();
            return subAdmin == null || subAdmin == "null" ?
                    adminArea : subAdmin + ", " + adminArea;
        }
        return "Location unavailable";
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

    public static void prepareNotification(Context context, List<TidesData.Waterlevel> waterlevels) {
        Timber.d("prepareNotfic");
        // get next low tide to notify about
        TidesData.Waterlevel nextLow = null;
        TidesData.Waterlevel nextHighAfterLow = null;
        for (int i = 0; i < waterlevels.size(); i++) {
            Timber.d("sjekker levels nr " + i);
            TidesData.Waterlevel l = waterlevels.get(i);
            if (l.flag.equals("low") && Utils.timeIsAfterNow(Utils.getFormattedTime(l.dateTime))) {
                nextLow = (nextLow == null || (l.dateTime.compareTo(nextLow.dateTime) < 0) ? l : nextLow);
                if (i + 1 < waterlevels.size())
                    nextHighAfterLow = waterlevels.get(i + 1);
            }
        }

        if (nextLow != null) {

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            String lowTideTime = Utils.getFormattedTime(nextLow.dateTime);

            Calendar calendarLowTide = Calendar.getInstance();
            calendarLowTide.set(Calendar.HOUR_OF_DAY, Integer.valueOf(lowTideTime.substring(0, 2)));
            calendarLowTide.set(Calendar.MINUTE, Integer.valueOf(lowTideTime.substring(3, 5)));
            long offset = TimeUnit.HOURS.toMillis(3);
            long offsetMargin = TimeUnit.MINUTES.toMillis(1);
            long notificationTime = calendarLowTide.getTimeInMillis() - offset;

            // set notification time to one minute from now if it's less than 3 hours till low tide
            if (((notificationTime + offsetMargin) < (calendarLowTide.getTimeInMillis() - offset)))
                notificationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);

            Intent myIntent = new Intent(context, NotifyService.class);
            myIntent.putExtra("nextLowTideTime", (calendarLowTide.getTimeInMillis()));
            myIntent.putExtra("nextLowTideLevel", nextLow.waterValue);
            if (nextHighAfterLow != null) {
                myIntent.putExtra("nextHighTideTime", (nextHighAfterLow.dateTime));
                myIntent.putExtra("nextHighTideLevel", nextHighAfterLow.waterValue);
            }

            PendingIntent pendingIntent = PendingIntent.getService(context.getApplicationContext(), 0, myIntent, 0);
            Timber.d("TIME: " + Utils.getTime(notificationTime));
            // Prepare notification only if it hasn't already been shown for this low tide
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            long previous = preferences.getLong(PREVIOUS_NOTIFICATION_TIME, 0);
            if (!Utils.getTime(previous).equals(Utils.getTime(notificationTime))) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                preferences.edit().putLong(PREVIOUS_NOTIFICATION_TIME, notificationTime).apply();
            }
        }

    }
}
