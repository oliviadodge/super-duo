package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;

/**
 * RemoteViewsService controlling the data being shown in the scrollable weather detail widget
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {
    public final String LOG_TAG = DetailWidgetRemoteViewsService.class.getSimpleName();

    public final static String EXTRA_MATCH_ID = "extra_match_id";
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

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                //Set up the parameters to query the content provider
                Date today = new Date(System.currentTimeMillis());

                data = getMatchesByDay(today);

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

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                String time = data.getString(INDEX_TIME);
                String home = data.getString(INDEX_HOME);
                String away = data.getString(INDEX_AWAY);
                String homeGoals = data.getString(INDEX_HOME_GOALS);
                String awayGoals = data.getString(INDEX_AWAY_GOALS);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if (Integer.parseInt(homeGoals) >= 0 && Integer.parseInt(awayGoals) >= 0) {
                        setRemoteContentDescription(views, home + " " + away + " "
                                + homeGoals + " to " + awayGoals);
                    } else {
                        setRemoteContentDescription(views, home + " versus " + away + " at "
                                + time);
                    }
                }
                views.setTextViewText(R.id.widget_detail_time, "Time: " + time);
                views.setTextViewText(R.id.widget_detail_home, home);
                views.setTextViewText(R.id.widget_detail_away, away);

                if (Integer.parseInt(homeGoals) >= 0) {
                    views.setTextViewText(R.id.widget_detail_home_goals, homeGoals);
                    views.setTextViewText(R.id.widget_detail_away_goals, awayGoals);
                } else {
                    views.setTextViewText(R.id.widget_detail_home_goals, "-");
                    views.setTextViewText(R.id.widget_detail_away_goals, "-");
                }

                //Set up the intent that will open the detail view of the match
                String matchId = data.getString(INDEX_MATCH_ID);
                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(EXTRA_MATCH_ID, matchId);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                Log.i(LOG_TAG, "onClickFillInIntent set!! ");
                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                views.setContentDescription(R.id.widget_icon, description);
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_MATCH_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}