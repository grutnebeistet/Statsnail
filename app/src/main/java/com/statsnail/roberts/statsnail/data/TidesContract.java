package com.statsnail.roberts.statsnail.data;

import android.net.Uri;
import android.provider.BaseColumns;


public class TidesContract {

    public static final String CONTENT_AUTHORITY = "com.statsnail.roberts.statsnail.data.TidesDataProvider";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_TIDES = "tides";
    public static final String PATH_WINDS = "winds";

    public static final class TidesEntry implements BaseColumns {
        public static Uri CONTENT_URI_TIDES = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_TIDES)
                .build();
        public static Uri CONTENT_URI_WINDS = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_WINDS)
                .build();
        public static final String TABLE_TIDES = "tides_table";
        public static final String TABLE_WINDS = "winds_table";

        /**
         * Extend the favorites ContentProvider to store the movie poster, synopsis, user rating,
         * and release date, and display them even when offline.
         */
        public static final String COLUMN_TIDES_ID = _ID;
        public static final String COLUMN_TIDES_DATE = "tide_date";
        public static final String COLUMN_WATER_LEVEL = "water_level";
        public static final String COLUMN_TIME_OF_LEVEL = "level_time";
        public static final String COLUMN_LEVEL_FLAG = "level_flag";
        public static final String COLUMN_TIDE_ERROR_MSG = "error";

        public static final String COLUMN_WINDS_ID = _ID;
        public static final String COLUMN_WINDS_DATE = "wind_date";
        public static final String COLUMN_TIME_OF_WIND = "wind_time";
        public static final String COLUMN_WIND_DIRECTION = "wind_dir";
        public static final String COLUMN_WIND_SPEED = "wind_speed";
        public static final String COLUMN_WIND_DIR_DEG = "wind_dir_deg";


    }

    public static final class WindsEntry implements BaseColumns {
        public static Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_TIDES)
                .build();

        public static final String TABLE_NAME = "winds_table";

        public static final String COLUMN_USER_LOCATION = "location";
        public static final String COLUMN_LATITUDE = "lat";
        public static final String COLUMN_LONGITUDE = "lon";
        public static final String COLUMN_WATER_LEVEL = "water_level";
        public static final String COLUMN_TIME_OF_LEVEL = "time_level";
        public static final String COLUMN_LEVEL_FLAG = "flag_level";

    }
}
