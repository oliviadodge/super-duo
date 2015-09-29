package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;

/**
 * Created by olivi on 9/20/2015.
 *
 *Much of this code comes from the open source app Sunshine, built by Google. See
 *the Licenses tab under the menu.
 */
public class ScoresWidgetIntentService extends IntentService {

    private static final String TAG = ScoresWidgetIntentService.class.getSimpleName();
    private static final String[] SCORES_COLUMNS = {
            DatabaseContract.scores_table.MATCH_ID,
            DatabaseContract.scores_table.TIME_COL,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.DATE_COL
    };
    // these indices must match the projection
    private static final int INDEX_MATCH_ID = 0;
    private static final int INDEX_TIME = 1;
    private static final int INDEX_HOME = 2;
    private static final int INDEX_AWAY = 3;
    private static final int INDEX_HOME_GOALS = 4;
    private static final int INDEX_AWAY_GOALS =5;
    private static final int INDEX_DATE =6;

    public ScoresWidgetIntentService() {
        super("ScoresWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                ScoresWidgetProvider.class));

        // Get today's data from the ContentProvider
//        String location = Utility.getPreferredLocation(this);

        //Set up the parameters to query the content provider
        Date today = new Date(System.currentTimeMillis());
        Date tomorrow = new Date(System.currentTimeMillis()+ 86400000);
        Date nextDay = new Date(System.currentTimeMillis()+((2)*86400000));

        Date[] days = {today, tomorrow, nextDay};

        Cursor data = null;

        //Try to find the first day where data is available

        for (int i = 0; i < days.length; i++) {
             data = getMatchesByDay(days[i]);
            if (data != null && data.moveToFirst()) {
                break;
            }
        }

        //If no data has been found by the 3rd day, return
        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }


        //Find first match that has not yet finished (ie scores will be below
        //zero).
        Log.i(TAG, "data.getString(INDEX_HOME_GOALS) = " + Integer.parseInt(data.getString(INDEX_HOME_GOALS)));
        while (Integer.parseInt(data.getString(INDEX_HOME_GOALS)) >= 0) {
            if (!data.moveToNext()) {
                //If the end of the cursor has been reached, that means there are no upcoming
                //matches available.
                return;
            }

        }

        // Extract the data from the Cursor
        String matchTime = data.getString(INDEX_TIME);
        String day = data.getString(INDEX_DATE);
        String home = data.getString(INDEX_HOME);
        String away = data.getString(INDEX_AWAY);
        data.close();

        // Perform this loop procedure for each Today widget
        for (int appWidgetId : appWidgetIds) {
            // Find the correct layout based on the widget's width
            int widgetWidth = getWidgetWidth(appWidgetManager, appWidgetId);
            int defaultWidth = getResources().getDimensionPixelSize(R.dimen.widget_scores_default_width); //TODO set default width
            int largeWidth = getResources().getDimensionPixelSize(R.dimen.widget_scores_large_width);
            int layoutId = R.layout.widget_scores;
            if (widgetWidth >= largeWidth) {
//                layoutId = R.layout.widget_today_large; TODO make large widget layout
            } else if (widgetWidth >= defaultWidth) {
                layoutId = R.layout.widget_scores;
            } else {
//                layoutId = R.layout.widget_today_small; TODO make small widget layout
            }
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            // Add the data to the RemoteViews
            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, matchTime);
            }

            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_launcher);

            views.setTextViewText(R.id.widget_date_time, day + " @" + matchTime);
            views.setTextViewText(R.id.widget_home, home);
            views.setTextViewText(R.id.widget_away, away);
            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    //Helper method to get a cursor of the day's matches from the scores table
    private Cursor getMatchesByDay(Date day) {
        SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
        String[] scoresDate = {mformat.format(day)};
        Uri scoresWithDate = DatabaseContract.scores_table.buildScoreWithDate();
        return getContentResolver().query(scoresWithDate, SCORES_COLUMNS, null,
                scoresDate, DatabaseContract.scores_table.DATE_COL + ", "
                        + DatabaseContract.scores_table.TIME_COL + " ASC");
    }

    private int getWidgetWidth(AppWidgetManager appWidgetManager, int appWidgetId) {
        // Prior to Jelly Bean, widgets were always their default size
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return getResources().getDimensionPixelSize(R.dimen.widget_scores_default_width);
        }
        // For Jelly Bean and higher devices, widgets can be resized - the current size can be
        // retrieved from the newly added App Widget Options
        return getWidgetWidthFromOptions(appWidgetManager, appWidgetId);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getWidgetWidthFromOptions(AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            int minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            // The width returned is in dp, but we'll convert it to pixels to match the other widths
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minWidthDp,
                    displayMetrics);
        }
        return  getResources().getDimensionPixelSize(R.dimen.widget_scores_default_width);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_icon, description);
    }
}
