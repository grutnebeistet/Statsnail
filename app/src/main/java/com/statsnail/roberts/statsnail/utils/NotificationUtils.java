package com.statsnail.roberts.statsnail.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
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
    private static final String NOTIFICATION_CHANNEL_ID = "my_notification_channel";

    public static void notifyOfLowTideTest(Context context, String k) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String lowTideTime = preferences.getString("nextLowTideTime", Utils.getTime());
        String lowTideValue = preferences.getString("nextLowTideLevel", "0");

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel =
                new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

        notificationChannel.setDescription("Channel description");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setVibrate(new long[]{0, 100, 100, 100, 100, 100})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setSmallIcon(R.drawable.logo)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.logo))
                .setContentTitle("Tide alert! " + k)
                .setContentText("Hehehe")
                .setAutoCancel(true);
        notificationManager.notify(TIDES_NOTIFICATION_ID, notificationBuilder.build());
    }
}
