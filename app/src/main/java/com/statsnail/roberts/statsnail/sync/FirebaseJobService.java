package com.statsnail.roberts.statsnail.sync;

import android.content.Context;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;
import com.statsnail.roberts.statsnail.utils.NotificationUtils;

import timber.log.Timber;

/**
 * Created by Adrian on 28/10/2017.
 */

public class FirebaseJobService extends com.firebase.jobdispatcher.JobService {
    private static AsyncTask<Void, Void, Void> mFetchTidesDataTask;


    @Override
    public boolean onStartJob(final JobParameters job) {
        mFetchTidesDataTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Timber.d("job, do in BG");
                Context context = getApplicationContext();
                StatsnailSyncTask.syncData(context, true);
               // NotificationUtils.notifyOfLowTideTest(context, " doInBG");
                jobFinished(job, true);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(job, true);
            }
        };
        mFetchTidesDataTask.execute();
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters job) {
        if (mFetchTidesDataTask != null) {
            mFetchTidesDataTask.cancel(true);
        }
        return true;
    }
}
