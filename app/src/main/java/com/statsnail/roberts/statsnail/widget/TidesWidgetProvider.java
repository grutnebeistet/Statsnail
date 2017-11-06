package com.statsnail.roberts.statsnail.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.statsnail.roberts.statsnail.BuildConfig;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivity;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.activities.SignInActivity;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.io.IOException;

import timber.log.Timber;

/**
 * Created by Adrian on 30/10/2017.
 */

public class TidesWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tides);
            String place = "Somewhere";
            try {
                place = Utils.getPlaceName(context);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            views.setTextViewText(R.id.location_widget, place);

            Class main = BuildConfig.APPLICATION_ID.equals("com.statsnail.roberts.statsnail.full") ?
                    MainActivityFull.class : MainActivity.class;
            Intent detailsIntent = new Intent(context, main);

            PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, detailsIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_container,configPendingIntent);


      /*      PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(detailsIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_container, clickPendingIntentTemplate);*/

            Intent intent = new Intent(context, TidesWidgetService.class);

            views.setRemoteAdapter(R.id.widget_list_view, intent);

            ComponentName component = new ComponentName(context, TidesWidgetService.class);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
            appWidgetManager.updateAppWidget(appWidgetId, views);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(component, views);
        }
    }
}
