package com.statsnail.roberts.statsnail.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.statsnail.roberts.statsnail.BuildConfig;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivity;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;

/**
 * Created by Adrian on 30/10/2017.
 */

public class TidesWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list);

            // Open mainactivity when title clicked TODO

            Class main = BuildConfig.APPLICATION_ID.equals("com.statsnail.roberts.statsnail.full") ?
                    MainActivityFull.class : MainActivity.class;
            Intent detailsIntent = new Intent(context, main);

            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(detailsIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.winds_list_view, clickPendingIntentTemplate);

            Intent intent = new Intent(context, TidesWidgetService.class);

            views.setRemoteAdapter(R.id.winds_list_view, intent);


            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
