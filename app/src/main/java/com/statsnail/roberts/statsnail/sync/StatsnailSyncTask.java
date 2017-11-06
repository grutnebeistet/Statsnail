package com.statsnail.roberts.statsnail.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import com.statsnail.roberts.statsnail.data.TidesContract;
import com.statsnail.roberts.statsnail.data.TidesDbHelper;
import com.statsnail.roberts.statsnail.models.TidesData;
import com.statsnail.roberts.statsnail.utils.NetworkUtils;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.net.URL;

import timber.log.Timber;

/**
 * Created by Adrian on 30/10/2017.
 */

public class StatsnailSyncTask {
    synchronized public static void syncData(Context context) {

        try {
            Timber.d("SyncData");
            //   NetworkUtils.loadNearbyXml(Utils.buildTidesRequestUrl(mLocation));
            String tidesRequestUrl = NetworkUtils.buildTidesRequestUrl(context);
            ContentValues[] tidesData = NetworkUtils.loadNearbyXml(context, tidesRequestUrl);

            String windsRequestUrl = NetworkUtils.buildWindsRequestUrl(context);
           // ContentValues[] windsData = NetworkUtils.loadWindsXml(windsRequestUrl);
            Timber.d("winds url : " + windsRequestUrl);
            for (ContentValues tides : tidesData) {
                // tides.put("TODO");
            }

            if (tidesData != null && tidesData.length != 0) {
                TidesDbHelper dbHelper = new TidesDbHelper(context);
                //      dbHelper.getReadableDatabase().delete(TidesContract.TidesEntry.TABLE_NAME,null,null);
                Timber.d("TidesData in SYnc: " + tidesData[0].get(TidesContract.TidesEntry.COLUMN_TIDE_ERROR_MSG) + " (errormsg)");
                ContentResolver resolver = context.getContentResolver();
                resolver.delete(
                        TidesContract.TidesEntry.CONTENT_URI,
                        null, null);

                resolver.bulkInsert(TidesContract.TidesEntry.CONTENT_URI, tidesData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
