package com.statsnail.roberts.statsnail.sync;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.BuildConfig;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivity;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;

import timber.log.Timber;

/**
 * Created by Adrian on 29/10/2017.
 */

public class NotifyService extends IntentService {
    private static final int TIDES_NOTIFICATION_ID = 1349;
    private static final String NOTIFICATION_CHANNEL_ID = "my_notification_channel";

    public NotifyService() {
        super("NotifyService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) throws NullPointerException {
        Timber.d("onHandleIntent!");
        String nextLowTideTime = intent.getStringExtra("nextLowTideTime");
        String nextHighTideTime = intent.getStringExtra("nextHighTideTime");
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

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

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setVibrate(new long[]{0, 100, 100, 100, 100, 100})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setSmallIcon(R.drawable.ic_logo)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_logo))
                .setContentTitle("Get to work you lazy bastard")
                .setContentText("Next low tide: " + nextLowTideTime + " o'clock. Following high tide peak: " + nextHighTideTime) //todo if nexthigh null
                .setAutoCancel(true);

        Class c = (com.statsnail.roberts.statsnail.BuildConfig.APPLICATION_ID.equals("com.statsnail.roberts.statsnail.full") ?
                MainActivityFull.class : MainActivity.class);
        Intent activityIntent = new Intent(this, c);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addNextIntentWithParentStack(activityIntent);
        PendingIntent resultPendingIntent = taskStackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(resultPendingIntent);


        notificationManager.notify(TIDES_NOTIFICATION_ID, notificationBuilder.build());
    }
}
