package com.statsnail.roberts.statsnail.utils;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivity;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.activities.SignInActivity;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.security.acl.LastOwnerException;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by Adrian on 29/10/2017.
 */

public class AlarmReceiver extends BroadcastReceiver {
    private static final int TIDES_NOTIFICATION_ID = 1349;
    private static final String NOTIFICATION_CHANNEL_ID = "my_notification_channel";

    @Override
    public void onReceive(Context context, @Nullable Intent intent) throws NullPointerException {

        long nextLowTideTime = intent.getLongExtra("nextLowTideTime", 0);
        String nextHighTideTime = intent.getStringExtra("nextHighTideTime");
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        String timeLeft = Utils.getRemainingTime(nextLowTideTime);
        String titleMessage;
        if ((timeLeft.charAt(0)) == '-') {
            titleMessage = "Tide was lowest " + timeLeft.substring(1) + " ago";
        } else
            titleMessage = timeLeft + " until tide bottom";

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setVibrate(new long[]{0, 100, 100, 100, 100, 100})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setSmallIcon(R.drawable.logo)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.logo))
                .setContentTitle("Tide alert! " + titleMessage)
                .setContentText("Lowest point at " + Utils.getTime((nextLowTideTime)) +
                        (nextHighTideTime != null ? ". Following peak at " + Utils.getFormattedTime(nextHighTideTime) : "."))
                .setAutoCancel(true);

        Intent activityIntent = new Intent(context, SignInActivity.class);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addNextIntentWithParentStack(activityIntent);
        PendingIntent resultPendingIntent = taskStackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(resultPendingIntent);

        Boolean notificationsEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_enable_notifications_key),
                context.getResources().getBoolean(R.bool.show_notifications_by_default));
        if (notificationsEnabled)
            try {
                notificationManager.notify(TIDES_NOTIFICATION_ID, notificationBuilder.build());
                // TODO if notification not read/still visible - update time left to next tide (update notify)
            } catch (NullPointerException e) {
                Timber.d("failed notifying: " + e.getMessage());
                e.printStackTrace();
            }
    }
}
