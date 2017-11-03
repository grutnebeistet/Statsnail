package com.statsnail.roberts.statsnail.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class TidesDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "tides.db";

    public static final int DATABASE_VERSION = 4;

    public TidesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_MOVIES_TABLE =
                "CREATE TABLE " + TidesContract.TidesEntry.TABLE_NAME + " (" +
                        TidesContract.TidesEntry.COLUMN_TIDES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        TidesContract.TidesEntry.COLUMN_TIDES_DATE + " TEXT, " +
                        TidesContract.TidesEntry.COLUMN_LEVEL_FLAG + " TEXT, " +
                        TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " TEXT, " +
                        TidesContract.TidesEntry.COLUMN_WATER_LEVEL + " TEXT, " +
                        TidesContract.TidesEntry.COLUMN_TIDE_ERROR_MSG + " TEXT, " +
                        TidesContract.TidesEntry.COLUMN_WIND_DIRECTION + " TEXT, " +
                        TidesContract.TidesEntry.COLUMN_WIND_SPEED + " TEXT " +
                        ");";

        sqLiteDatabase.execSQL(SQL_CREATE_MOVIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TidesContract.TidesEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
