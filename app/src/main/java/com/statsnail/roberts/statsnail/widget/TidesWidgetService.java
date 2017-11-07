package com.statsnail.roberts.statsnail.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.data.TidesContract;
import com.statsnail.roberts.statsnail.utils.Utils;

/**
 * Created by Adrian on 30/10/2017.
 */

public class TidesWidgetService extends RemoteViewsService {
    public TidesWidgetService() {
        super();
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TideseRemoteViewsFactory(this.getApplicationContext());
    }

    class TideseRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private Context mContext;
        Cursor tideData;

        TideseRemoteViewsFactory(Context context) {
            mContext = context;
        }

        @Override
        public void onCreate() {

        }

        @Override
        public int getCount() {
            return tideData == null ? 0 : tideData.getCount();
        }

        @Override
        public void onDataSetChanged() {
            final long identityToken = Binder.clearCallingIdentity();
            String[] projection = {
                    TidesContract.TidesEntry.COLUMN_TIDES_DATE,
                    TidesContract.TidesEntry.COLUMN_WATER_LEVEL,
                    TidesContract.TidesEntry.COLUMN_LEVEL_FLAG,
                    TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL,
                    TidesContract.TidesEntry.COLUMN_TIDE_ERROR_MSG
            };
            String sortOrder = TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " ASC";
            String selection = TidesContract.TidesEntry.COLUMN_TIDES_DATE + "=?";
            String[] selectionArgs = new String[]{Utils.getDate(System.currentTimeMillis())};

            tideData = mContext.getContentResolver().query(TidesContract.TidesEntry.CONTENT_URI,
                    projection, selection, selectionArgs, sortOrder
            );
            if (tideData == null || !tideData.moveToFirst()) {
                return;
            }
            Binder.restoreCallingIdentity(identityToken);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public RemoteViews getViewAt(int i) {
            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_tide_item);
            if (i == AdapterView.INVALID_POSITION ||
                    getCount() == 0 || !tideData.moveToFirst()) {
                return remoteViews;
            }
            tideData.moveToPosition(i);
            if (tideData.getCount() == 1) {
                remoteViews.setViewVisibility(R.id.tide_item_error, View.VISIBLE);
                remoteViews.setTextViewText(R.id.tide_item_error, tideData.getString(tideData.getColumnIndex(TidesContract.TidesEntry.COLUMN_TIDE_ERROR_MSG)));
                return remoteViews;
            }
            int flagIndex = tideData.getColumnIndex(TidesContract.TidesEntry.COLUMN_LEVEL_FLAG);
            int timeIndex = tideData.getColumnIndex(TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL);
            int levelIndex = tideData.getColumnIndex(TidesContract.TidesEntry.COLUMN_WATER_LEVEL);

            int imgRes = tideData.getString(flagIndex).equals("high") ? R.drawable.high_tide : R.drawable.low_tide;
            String time = tideData.getString(timeIndex);
            String level = tideData.getString(levelIndex);

            remoteViews.setTextViewText(R.id.tide_item_time, time);
            remoteViews.setTextViewText(R.id.tide_item_value, getString(R.string.level_format, level));
            remoteViews.setImageViewResource(R.id.tide_item_img, imgRes);

            return remoteViews;

        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onDestroy() {
            if (tideData != null)
                tideData.close();
        }

    }
}

