package com.statsnail.roberts.statsnail.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.data.LogContract;
import com.statsnail.roberts.statsnail.data.SqlDbHelper;

import java.io.IOException;

import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import timber.log.Timber;

import static android.content.ContentValues.TAG;
import static com.statsnail.roberts.statsnail.activities.HarvestActivity.REQUEST_AUTHORIZATION;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_DATE;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_GRADED_BY;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_ID;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_USER;
import static com.statsnail.roberts.statsnail.data.LogContract.CONTENT_URI_HARVEST_LOG;
import static com.statsnail.roberts.statsnail.data.LogContract.TABLE_LOGS;

/**
 * Created by Adrian on 27/10/2017.
 */

public class HarvestUtils {
    @AfterPermissionGranted(REQUEST_AUTHORIZATION)
    public static ValueRange readSheet(Activity context, com.google.api.services.sheets.v4.Sheets service) {
        ValueRange result = null;
        String spreadsheetId = context.getString(R.string.spreadsheet_id);
        String range = context.getString(R.string.spreadsheet_read_range);
        // Log.i(TAG, "name spread: " + spreadsheetId);
        try {
            result = service.spreadsheets().values().get(spreadsheetId, range).execute();
            updateDb(context, result);

        } catch (UserRecoverableAuthIOException userRecoverableException) {
            context.startActivityForResult(
                    userRecoverableException.getIntent(), REQUEST_AUTHORIZATION); // Requests permission again
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getLocationUrl(Location location) {
        if (location != null) {
            double longitude;
            double latitude;
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            String latLonBase = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;

            return "=HYPERLINK(\"" + latLonBase + "\", \"" + latitude + "," + longitude + "\")";
        } else return "Location not available";
    }


    public static void updateDbSingle(Context context, ValueRange newValue, int rowNum) {

        ContentValues values = new ContentValues();

        String name_grader = newValue.getValues().get(0).get(4).toString();
        values.put(COLUMN_HARVEST_GRADED_BY, name_grader);

        Uri logUri = ContentUris.withAppendedId(CONTENT_URI_HARVEST_LOG, rowNum);
        context.getContentResolver().update(logUri, values, null, null);
        context.getContentResolver().notifyChange(logUri, null);

    }

    public static void updateDb(Context context, ValueRange result) {
        int numRows = result.getValues() != null ? result.getValues().size() - 1 : 0;  // +1 because first row consists of labels

        Cursor cursor;
        SqlDbHelper dbHelper = new SqlDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteDatabase dbw = dbHelper.getWritableDatabase();

        cursor = db.query(LogContract.TABLE_LOGS, null, null, null, null, null, null);
        // Log.i(TAG, "Cursor Count : " + cursor.getCount());

        if (cursor.getCount() > 0) {
            //db.delete(LogContract.TABLE_LOGS, null, null); // TODO bedre enn dette -nå slettes hver gang
            dbw.delete(TABLE_LOGS, null, null);
            cursor.close();
        }


        List<List<Object>> logs = result.getValues(); // TODO flytte DB stuff til egen metode/ kun lagre nye innføringer?
        if (!result.isEmpty() && numRows >= 1) {
            for (int i = 1; i < logs.size(); i++) {
                List<Object> lb = logs.get(i);
                String harvestNo = lb.get(0).toString();
                String date = lb.get(1).toString();
                String name = lb.get(5).toString();
                String name_grader = lb.size() > 6 ? lb.get(10).toString() : null;


                ContentValues values = new ContentValues();
                values.put(COLUMN_HARVEST_ID, Integer.valueOf(harvestNo));
                values.put(COLUMN_HARVEST_DATE, date);
                values.put(COLUMN_HARVEST_USER, name);
                // a row with size > 6 means it has been graded - retrieve graders name
                if (lb.size() > 6) {
                    // Log.i(TAG, "Been graded " + harvestNo);
                    values.put(COLUMN_HARVEST_GRADED_BY, name_grader);
                } else {
                    // values.put(COLUMN_HARVEST_GRADED_BY, null);
                    // Log.i(TAG, "Not graded " + harvestNo);
                } // TODO bulkinsert

                context.getContentResolver().insert(CONTENT_URI_HARVEST_LOG, values);
                //   Log.i(TAG, "lb: " + lb + " lb.size: " +lb.size() + "\nHarvNo " + harvestNo + " dato: " + date + " name: " + name);
            }
        }
    }
}
