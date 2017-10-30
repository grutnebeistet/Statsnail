package com.statsnail.roberts.statsnail.widget;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

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

        TideseRemoteViewsFactory(Context context) {
            mContext = context;
        }

        @Override
        public void onCreate() {

        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public void onDataSetChanged() {

        }

        @Override
        public int getViewTypeCount() {
            return 0;
        }

        @Override
        public RemoteViews getViewAt(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public void onDestroy() {

        }
    }
}
