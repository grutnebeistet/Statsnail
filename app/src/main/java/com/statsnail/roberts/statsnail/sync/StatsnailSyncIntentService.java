package com.statsnail.roberts.statsnail.sync;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import timber.log.Timber;

/**
 * Created by Adrian on 30/10/2017.
 */

public class StatsnailSyncIntentService extends IntentService {
    public StatsnailSyncIntentService(){super("StatsnailSyncIntentService");}
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Timber.d("onHandleIntent, call syncData");
        StatsnailSyncTask.syncData(this);
    }
}
