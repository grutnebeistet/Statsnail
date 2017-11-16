package com.statsnail.roberts.statsnail.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import com.statsnail.roberts.statsnail.data.TidesContract;
import com.statsnail.roberts.statsnail.utils.NetworkUtils;

import timber.log.Timber;

/**
 * Created by Adrian on 30/10/2017.
 */

public class StatsnailSyncTask {
    synchronized public static void syncData(Context context, boolean homeLocation) {

        try {
            Timber.d("SyncData");

            String windsRequestUrl = NetworkUtils.buildWindsRequestUrl(context, homeLocation);
            ContentValues[] windsData = NetworkUtils.loadWindsXml(windsRequestUrl);

            String tidesRequestUrl = NetworkUtils.buildTidesRequestUrl(context, homeLocation);
            ContentValues[] tidesData = NetworkUtils.loadNearbyXml(context, tidesRequestUrl);

            ContentResolver resolver = context.getContentResolver();
            if (null != tidesData && tidesData.length != 0) {
                resolver.delete(
                        TidesContract.TidesEntry.CONTENT_URI_TIDES,
                        null, null);

                resolver.bulkInsert(TidesContract.TidesEntry.CONTENT_URI_TIDES, tidesData);
            }
            if (null != windsData && windsData.length != 0) {
                resolver.delete(
                        TidesContract.TidesEntry.CONTENT_URI_WINDS, null, null
                );
                resolver.bulkInsert(TidesContract.TidesEntry.CONTENT_URI_WINDS, windsData);
            }

        } catch (Exception e) {
            Timber.d("failed to sync data");
            e.printStackTrace();
        }

    }
}
