package com.statsnail.roberts.statsnail.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.models.TidesData;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    public static boolean timeIsAfterNowOrMidnight(String time) {
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

        Timber.d("lowTideHours " + lowTideHours + " > " + "currentHours " + currentHours + " date: " + currentDay +
                "year: " + year + " month " + month + " day " + day + " currMontn " + currentMonth + " curryear: " + currentYear);

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
        String builder = country + slash + state + slash + subAdminArea +
                slash + ka + kairp + slash + slash + loc + slash + fea + slash + address;

        return builder;
    }

    public static String getPlaceName(Context context, boolean homeLocation) throws IOException {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultLat = context.getResources().getString(R.string.default_latitude);
        String defaultLon = context.getResources().getString(R.string.default_longitude);

        String latitude = homeLocation ? preference.getString(MainActivityFull.HOME_LAT, defaultLat) :
                preference.getString(MainActivityFull.EXTRA_LATITUDE, defaultLat);
        String longitude = homeLocation ? preference.getString(MainActivityFull.HOME_LON, defaultLon) :
                preference.getString(MainActivityFull.EXTRA_LONGITUDE, defaultLon);

        Timber.d("LAT i getPlaceName: " + latitude);
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(Double.valueOf(latitude), Double.valueOf(longitude), 1);

        if (addresses.size() != 0) {
            String subAdmin = addresses.get(0).getSubAdminArea();
            String adminArea = addresses.get(0).getAdminArea();

            return subAdmin != null && adminArea != null ? subAdmin + ", " + adminArea :
                    subAdmin == null && adminArea != null ? adminArea :
                            subAdmin != null && adminArea == null ? subAdmin : "Location unavailable";

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
            TidesData.Waterlevel l = waterlevels.get(i);
            if (l.flag.equals("low") && timeIsAfterNowOrMidnight(l.dateTime)) {// Utils.timeIsAfterNow(Utils.getFormattedTime(l.dateTime))) {
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

            Intent myIntent = new Intent(context, NotifyBroadcast.class);
            myIntent.putExtra("nextLowTideTime", (calendarLowTide.getTimeInMillis()));
            myIntent.putExtra("nextLowTideLevel", nextLow.waterValue);

            if (nextHighAfterLow != null) {
                myIntent.putExtra("nextHighTideTime", (nextHighAfterLow.dateTime));
                myIntent.putExtra("nextHighTideLevel", nextHighAfterLow.waterValue);
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent, 0);
            Timber.d("TIME: " + Utils.getTime(notificationTime));
            // Prepare notification only if it hasn't already been shown for this low tide
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            long previousNotificationTime = preferences.getLong(PREVIOUS_NOTIFICATION_TIME, 0);
            if (!Utils.getTime(previousNotificationTime).equals(Utils.getTime(notificationTime))) {
                if (Build.VERSION.SDK_INT >= 23)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                else if (Build.VERSION.SDK_INT >= 19)
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                else alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);

                Timber.d("alarm set for " + Utils.getTime(notificationTime));
                preferences.edit().putLong(PREVIOUS_NOTIFICATION_TIME, notificationTime).apply();
            }
        }

    }
}
