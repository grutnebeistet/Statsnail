package com.statsnail.roberts.statsnail.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class MovieDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "tides.db";

    public static final int DATABASE_VERSION = 1;

    public MovieDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_MOVIES_TABLE =
                "CREATE TABLE " + TidesContract.TidesEntry.TABLE_NAME + " (" +
                /*        MovieEntry.COLUMN_MOVIE_ID + " INTEGER PRIMARY KEY, " +
                        MovieEntry.COLUMN_MOVIE_ORIGINAL_TITLE + " STRING NOT NULL, " +
                        MovieEntry.COLUMN_MOVIE_ALTERNATIVE_TITLE + " STRING, " +
                        MovieEntry.COLUMN_MOVIE_RELEASE_DATE + " STRING NOT NULL, " +
                        MovieEntry.COLUMN_MOVIE_SYNOPSIS + " STRING NOT NULL, " +
                        MovieEntry.COLUMN_MOVIE_POSTER + " STRING NOT NULL, " +
                        MovieEntry.COLUMN_MOVIE_THUMBNAIL + " STRING NOT NULL, " +
                        MovieEntry.COLUMN_MOVIE_RATING + " DOUBLE NOT NULL, " +
                        MovieEntry.COLUMN_MOVIE_RUNTIME + " INTEGER, " +
                        MovieEntry.COLUMN_MOVIE_BY_POPULARITY + " INTEGER DEFAULT 0, " +
                        MovieEntry.COLUMN_MOVIE_BY_RATING + " INTEGER DEFAULT 0, " +
                        MovieEntry.COLUMN_MOVIE_BY_FAVOURITE + " INTEGER DEFAULT 0, " +
                        MovieEntry.COLUMN_STORED_MOVIE_THUMBNAIL + " BLOB, " +
                        MovieEntry.COLUMN_STORED_MOVIE_POSTER + " BLOB" +*/
                        ");";

        sqLiteDatabase.execSQL(SQL_CREATE_MOVIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TidesContract.TidesEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
