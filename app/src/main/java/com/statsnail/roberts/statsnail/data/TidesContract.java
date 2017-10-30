package com.statsnail.roberts.statsnail.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;




public class TidesContract {

    public static final String CONTENT_AUTHORITY = "com.statsnail.roberts.statsnail";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_TIDES = "tides";

    public static final class TidesEntry implements BaseColumns {
        public static Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_TIDES)
                .build();

        public static final String TABLE_NAME = "tides_table";

        /**
         * Extend the favorites ContentProvider to store the movie poster, synopsis, user rating,
         * and release date, and display them even when offline.
         */
        public static final String COLUMN_USER_LOCATION = "location";
        public static final String COLUMN_LATITUDE = "lat";
        public static final String COLUMN_LONGITUDE = "lon";
        public static final String COLUMN_WATER_LEVEL = "water_level";
        public static final String COLUMN_TIME_OF_LEVEL = "time_level";
        public static final String COLUMN_LEVEL_FLAG = "flag_level";

    }
}
