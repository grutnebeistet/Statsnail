package com.statsnail.roberts.statsnail.sync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by Adrian on 29/10/2017.
 */

public class SyncUtils {
    private static final int SYNC_INTERVAL_HOURS = 12;
    private static final int SYNC_INTERVAL_SECONDS = (int) TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS);
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS / 3;

    private static final String TIDES_SYNC_TAG = "tides-sync";

    private static boolean sInitialized;

    static void scheduleFirebaseJobDispatcher(@NonNull final Context context) {
        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher jobDispatcher = new FirebaseJobDispatcher(driver);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String lowTideTime = preferences.getString("nextLowTideTime", Utils.getTime());
        int timeToDispatch;


        Job syncTidesJob = jobDispatcher.newJobBuilder()
                .setService(FirebaseJobService.class)
                .setTag(TIDES_SYNC_TAG)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(
                        SYNC_FLEXTIME_SECONDS,
                        SYNC_FLEXTIME_SECONDS + SYNC_FLEXTIME_SECONDS))
                .setReplaceCurrent(true)
                .build();

        jobDispatcher.schedule(syncTidesJob);

    }

    synchronized public static void initialize(@NonNull final Context context) {

        /*
         * Only perform initialization once per app lifetime. If initialization has already been
         * performed, we have nothing to do in this method.
         */
        if (sInitialized) return;

        sInitialized = true;

        scheduleFirebaseJobDispatcher(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getString("nextLowTideLevel", null) == null) {
            startImmediateSync(context);
        }
    }

    public static void startImmediateSync(@NonNull final Context context) {
        Timber.d("startImmediateSync");
        Intent intentToSyncImmediately = new Intent(context, StatsnailSyncIntentService.class);
        context.startService(intentToSyncImmediately);
    }
}
