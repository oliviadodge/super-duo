package barqsoft.footballscores.widget;

import barqsoft.footballscores.service.myFetchService;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

/**
 * Created by olivi on 9/20/2015.
 */
public class ScoresWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {


        //update database with any new data from the api
        Intent service_start = new Intent(context, myFetchService.class);
        context.startService(service_start);

        //start service to query for data and update widget view
        context.startService(new Intent(context, ScoresWidgetIntentService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        //start service to query for data and update widget view
        context.startService(new Intent(context, ScoresWidgetIntentService.class));
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        //if data is updated in the app, update the widget view too
        if (myFetchService.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            context.startService(new Intent(context, ScoresWidgetIntentService.class));
        }
    }
}
