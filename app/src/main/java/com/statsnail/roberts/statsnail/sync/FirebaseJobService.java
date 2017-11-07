package com.statsnail.roberts.statsnail.sync;

import android.content.Context;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;

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
                Context context = getApplicationContext();
                StatsnailSyncTask.syncData(context);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(job, false);
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
