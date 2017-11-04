package com.statsnail.roberts.statsnail.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Adrian on 18/08/2017.
 */

public class LogContract implements BaseColumns {
    public static final String CONTENT_AUTHORITY = "com.statsnail.roberts.statsnail.full";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_LOG = "harvest_log";

    public static final Uri CONTENT_URI_HARVEST_LOG = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOG).build();
    /**
     * The MIME type of the {@link #CONTENT_URI_HARVEST_LOG} for a list of movies.
     */
    public static final String CONTENT_LOG_LIST_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOG;

    /**
     * The MIME type of the {@link #CONTENT_URI_HARVEST_LOG} for a single movie.
     */
    public static final String CONTENT_LOG_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOG;



    public static final String TABLE_LOGS = "harvest_logs";

    public static final String COLUMN_HARVEST_ID = "harvest_id";
    public static final String COLUMN_HARVEST_USER = "harvest_user";
    public static final String COLUMN_HARVEST_DATE = "harvest_date";
    public static final String COLUMN_HARVEST_GRADED_BY = "harvest_graded_by";

    public static final int BEEN_GRADED = 1;
    public static final int NOT_GRADED = 0;




}
