package com.statsnail.roberts.statsnail.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.statsnail.roberts.statsnail.models.TidesData;

import timber.log.Timber;


public class TidesDataProvider extends ContentProvider {
    private static final String LOG_TAG = TidesDataProvider.class.getSimpleName();

    TidesDbHelper mDbHelper;
    /**
     * URI matcher codes for the content URI:
     * TIDES for general table query
     * TIDES_ID for query on a specific movie
     */
    private static final int TIDES = 100;
    private static final int TIDES_ID = 101;
    private static final int WINDS = 200;

    private static final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {

        mUriMatcher.addURI(TidesContract.CONTENT_AUTHORITY, TidesContract.PATH_TIDES, TIDES);

        mUriMatcher.addURI(TidesContract.CONTENT_AUTHORITY, TidesContract.PATH_WINDS, WINDS);

        mUriMatcher.addURI(TidesContract.CONTENT_AUTHORITY, TidesContract.PATH_TIDES + "/#", TIDES_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new TidesDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selArgs, String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        int uriMatch = mUriMatcher.match(uri);

        Cursor returnCursor;

        switch (uriMatch) {
            case TIDES:
                returnCursor = db.query(TidesContract.TidesEntry.TABLE_TIDES, projection, selection, selArgs, null, null, sortOrder);
                break;
            case WINDS:
                returnCursor = db.query(TidesContract.TidesEntry.TABLE_WINDS, projection, selection, selArgs, null, null, sortOrder);
                break;
            case TIDES_ID:
                selection = TidesContract.TidesEntry.COLUMN_TIDES_ID + "=?";
                selArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                returnCursor = db.query(TidesContract.TidesEntry.TABLE_TIDES, projection, selection, selArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query given uri: " + uri);
        }
        returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return returnCursor;
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long newRowId;
        int uriMatch = mUriMatcher.match(uri);
        switch (uriMatch) {
            case TIDES:
                newRowId = db.insert(TidesContract.TidesEntry.TABLE_TIDES, null, contentValues);
                break;
            case WINDS:
                newRowId = db.insert(TidesContract.TidesEntry.TABLE_WINDS, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Cannot insert for given uri: " + uri);
        }
        if (newRowId == -1) {
            Log.e(LOG_TAG, "insertion failed for " + uri);
            return null;
        }
        // Return the Uri for the newly added Movie
        return ContentUris.withAppendedId(uri, newRowId);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[]
            selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int deletedRows;
        switch (mUriMatcher.match(uri)) {
            case TIDES:
                deletedRows = db.delete(TidesContract.TidesEntry.TABLE_TIDES, selection, selectionArgs);
                break;
            case WINDS:
                deletedRows = db.delete(TidesContract.TidesEntry.TABLE_WINDS, selection, selectionArgs);
                break;
            case TIDES_ID:
                selection = TidesContract.TidesEntry.COLUMN_TIDES_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                deletedRows = db.delete(TidesContract.TidesEntry.TABLE_TIDES, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Failed to delete: " + uri);
        }
        if (deletedRows != 0) getContext().getContentResolver().notifyChange(uri, null);
        return deletedRows;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[]
            selectionArgs) {

        // return early if there's no values to update
        if (contentValues.size() == 0) return 0;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsUpdated;

        switch (mUriMatcher.match(uri)) {
            case TIDES:
                rowsUpdated = db.update(TidesContract.TidesEntry.TABLE_TIDES, contentValues, selection, selectionArgs);
                break;
            case WINDS:
                rowsUpdated = db.update(TidesContract.TidesEntry.TABLE_WINDS, contentValues, selection, selectionArgs);
                break;
            case TIDES_ID:
                selection = TidesContract.TidesEntry.COLUMN_TIDES_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsUpdated = db.update(TidesContract.TidesEntry.TABLE_TIDES, contentValues, selection, selectionArgs);
                // Log.i(LOG_TAG, "updated movie");
                break;
            default:
                throw new IllegalArgumentException("Cannot update for given uri " + uri);
        }
        //  if (rowsUpdated != 0) getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String table;
        switch (mUriMatcher.match(uri)) {

            case TIDES:
                table = TidesContract.TidesEntry.TABLE_TIDES;
                Timber.d("bulking TIDES");
                break;
            case WINDS:
                table = TidesContract.TidesEntry.TABLE_WINDS;
                Timber.d("bulking WINDS");
                break;
            default:
                return super.bulkInsert(uri, values);
        }
        db.beginTransaction();
        int rowsInserted = 0;
        try {
            for (ContentValues value : values) {
                long _id = -1;
                if (value != null)
                    _id = db.insert(table, null, value);
                if (_id != -1) {
                    rowsInserted++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (rowsInserted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        Log.i(LOG_TAG, "inserted: " + rowsInserted + " rows");
        return rowsInserted;

    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = mUriMatcher.match(uri);
        return "TODO";
    }
}

