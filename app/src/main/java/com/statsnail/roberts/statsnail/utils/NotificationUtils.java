package com.statsnail.roberts.statsnail.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivity;
import com.statsnail.roberts.statsnail.models.TidesData;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by Adrian on 29/10/2017.
 */

public class NotificationUtils {
    private final static String PREVIOUS_NOTIFICATION_TIME = "prev_not";
    private final static String PREVIOUS_NOTIFICATION_OFFSET = "prev_offset";

    public static void prepareNotification(Context context, List<TidesData.Waterlevel> waterlevels) {
        // get next low tide to notify about
        TidesData.Waterlevel nextLow = null;
        TidesData.Waterlevel nextHighAfterLow = null;
        for (int i = 0; i < waterlevels.size(); i++) {
            TidesData.Waterlevel l = waterlevels.get(i);
            if (l.flag.equals("low") && Utils.timeIsAfterNowInclMidnight(l.dateTime)) {// Utils.timeIsAfterNow(Utils.getFormattedTime(l.dateTime))) {
                //  nextLow = (nextLow == null || (l.dateTime.compareTo(nextLow.dateTime) < 0) ? l : nextLow);
                if (nextLow == null || (l.dateTime.compareTo(nextLow.dateTime) < 0)) {
                    nextLow = l;
                    if (waterlevels.size() > i+1) nextHighAfterLow = waterlevels.get(i + 1);
                }
            }
        }
        if (nextLow != null) {

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // In case lowtide is after midnight, date must be considered
            String lowTideDateString = Utils.getFormattedDate(nextLow.dateTime);
            String lowTideTimeString = Utils.getFormattedTime(nextLow.dateTime);

            Calendar calendarLowTide = Calendar.getInstance();

            calendarLowTide.set(Calendar.YEAR, Integer.valueOf(lowTideDateString.substring(0, 4)));
            calendarLowTide.set(Calendar.MONTH, Integer.valueOf(lowTideDateString.substring(5, 7)) - 1); // months are counted from 0
            calendarLowTide.set(Calendar.DATE, Integer.valueOf(lowTideDateString.substring(8)));
            calendarLowTide.set(Calendar.HOUR_OF_DAY, Integer.valueOf(lowTideTimeString.substring(0, 2)));
            calendarLowTide.set(Calendar.MINUTE, Integer.valueOf(lowTideTimeString.substring(3, 5)));
            Timber.d("Date from calendar thing: " + calendarLowTide.getTime() + "\nvia utils: " +
                    Utils.getDate(calendarLowTide.getTimeInMillis()) + " " + Utils.getTime(calendarLowTide.getTimeInMillis()));

            long currentTime = System.currentTimeMillis();
            long lowTideTime = calendarLowTide.getTimeInMillis();
            String hoursOffsetPrefs = PreferenceManager.getDefaultSharedPreferences(context).getString(
                    context.getString(R.string.notify_hours_key), context.getString(R.string.notify_hours_default));
            Timber.d("NOTIFY OFFSET PREF: " + hoursOffsetPrefs);
            long offset = TimeUnit.HOURS.toMillis(Integer.parseInt(hoursOffsetPrefs));
            long offsetMargin = TimeUnit.MINUTES.toMillis(1);
            long notificationTime = lowTideTime - offset + offsetMargin;

            // set notification time to one minute from now if it's less than 3 hours till low tide
            //if ((notificationTime + offsetMargin) < notificationTime); //;(calendarLowTide.getTimeInMillis() - offset)))
            if (currentTime + offset > lowTideTime)
                notificationTime = currentTime;// + offsetMargin;

            Intent myIntent = new Intent(context, AlarmReceiver.class);   //(AlarmReceiver.INTENT_FILTER);
            myIntent.putExtra("nextLowTideTime", lowTideTime);
            myIntent.putExtra("nextLowTideLevel", nextLow.waterValue);

            if (nextHighAfterLow != null) {
                myIntent.putExtra("nextHighTideTime", (nextHighAfterLow.dateTime));
                myIntent.putExtra("nextHighTideLevel", nextHighAfterLow.waterValue);
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            // PendingIntent.FLAG_CANCEL_CURRENT);  // FLAG to avoid creating a second service if there's already one running

            // Prepare notification only if it hasn't already been shown for this low tide
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            long previousNotificationTime = preferences.getLong(PREVIOUS_NOTIFICATION_TIME, 0);
            String previousNotificationOffset = preferences.getString(PREVIOUS_NOTIFICATION_OFFSET, "0");

            Timber.d("Previous time: " + previousNotificationTime + ", newtime: " + notificationTime);
            if (!(Utils.getTime(previousNotificationTime).equals(Utils.getTime(lowTideTime))
                    && hoursOffsetPrefs.equals(previousNotificationOffset))) {
                final int SDK_INT = Build.VERSION.SDK_INT;
                if (SDK_INT >= Build.VERSION_CODES.M)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                else if (Build.VERSION_CODES.KITKAT <= SDK_INT && SDK_INT < Build.VERSION_CODES.M)
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                else alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);

                Timber.d("New alarm set for " + Utils.getTime(notificationTime));
                preferences.edit().putLong(PREVIOUS_NOTIFICATION_TIME, lowTideTime).
                        putString(PREVIOUS_NOTIFICATION_OFFSET, hoursOffsetPrefs).apply();
            }
        }
    }
}
