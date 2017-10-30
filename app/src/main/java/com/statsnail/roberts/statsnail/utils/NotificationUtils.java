package com.statsnail.roberts.statsnail.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivity;

/**
 * Created by Adrian on 29/10/2017.
 */

public class NotificationUtils {
 private static final int TIDES_NOTIFICATION_ID = 1349;

    public static void notifyOfLowTide(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String lowTideTime = preferences.getString("nextLowTideTime", Utils.getTime());
        String lowTideValue = preferences.getString("nextLowTideLevel", "0");

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "0")
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary_700))
                // .setSmallIcon(dra) low tide icon
                //.setLargeIcon(largeIcon)
                .setContentTitle("Get to work you lazy bastard")
                .setContentText("Next tide at " + lowTideTime + " o'clock.")
                .setAutoCancel(true);

        Intent intent = new Intent(context, MainActivity.class);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addNextIntentWithParentStack(intent);
        PendingIntent resultPendingIntent = taskStackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(TIDES_NOTIFICATION_ID, notificationBuilder.build());
    }
}
