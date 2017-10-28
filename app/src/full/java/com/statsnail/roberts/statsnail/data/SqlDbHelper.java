package com.statsnail.roberts.statsnail.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_DATE;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_GRADED_BY;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_ID;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_USER;
import static com.statsnail.roberts.statsnail.data.LogContract.TABLE_LOGS;

/**
 * Created by Adrian on 18/08/2017.
 */

public class SqlDbHelper extends SQLiteOpenHelper {
    private final static String DB_NAME = "logs.db";
    private static final int DB_VERSION = 27;

    public SqlDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_LOGS + " (" +
                        COLUMN_HARVEST_ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_HARVEST_DATE + " STRING NOT NULL, " +
                        COLUMN_HARVEST_USER + " STRING NOT NULL, " +
                        COLUMN_HARVEST_GRADED_BY + " TEXT" +
                        ");";
        db.execSQL(SQL_CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);

        onCreate(db);
    }


}
