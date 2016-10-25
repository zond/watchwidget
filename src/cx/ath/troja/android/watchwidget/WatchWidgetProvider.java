
package cx.ath.troja.android.watchwidget;

import android.appwidget.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import android.os.*;
import android.net.*;
import android.app.*;

import java.util.*;
import java.io.*;
import java.math.*;

import com.darekxan.insanity.toolbox.*;

public class WatchWidgetProvider extends AppWidgetProvider {

    public static final String URI_SCHEME = "watch_widget";
    public static final String REFRESH = WatchWidgetProvider.class.getName() + ".REFRESH";
    private static final String INTERACTIVE_PATH = "/interactive/";
    private static final String AUTOMATED_PATH = "/automated/";
    private static final String CONFIGURE_PATH = "/configure/";

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
	for (int appWidgetId : appWidgetIds) {
	    WatchWidgetService.unregisterWidget(appWidgetId);
	    unRegisterWidgetUpdateIntent(context, appWidgetId);
	}
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	for (int appWidgetId : appWidgetIds) {
	    update(context, appWidgetManager, appWidgetId);
	}
    }

    @Override
    public void onReceive(Context context, Intent intent) {
	Uri data = intent.getData();
	if (data != null && data.getPath().matches("^" + INTERACTIVE_PATH + ".*")) {
	    ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(30);
	}
	super.onReceive(context, intent);
    }

    private void update(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
	SharedPreferences config = context.getSharedPreferences(WatchWidgetConfiguration.class.getName() + "." + appWidgetId, Context.MODE_PRIVATE);
	
	RemoteViews remoteView = new RemoteViews(context.getPackageName(),
						 R.layout.watch_widget);
	
	remoteView.setFloat(R.id.watch_widget_content, "setTextSize", (float) config.getInt(WatchWidgetConfiguration.FONT_SIZE, 10));
	remoteView.setInt(R.id.watch_widget_content, "setBackgroundColor", 
			  new BigInteger(config.getString(WatchWidgetConfiguration.BACKGROUND_COLOR, "88000000"), 16).intValue());
	remoteView.setInt(R.id.watch_widget_content, "setTextColor", 
			  new BigInteger(config.getString(WatchWidgetConfiguration.FONT_COLOR, "ffffffff"), 16).intValue());
	remoteView.setTextViewText(R.id.watch_widget_content, getContent(config));
	
	Intent widgetConfigure = new Intent(context, WatchWidgetConfiguration.class);
	widgetConfigure.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
	widgetConfigure.setData(Uri.withAppendedPath(Uri.parse(WatchWidgetProvider.URI_SCHEME + "://widget" + CONFIGURE_PATH), String.valueOf(appWidgetId)));
	remoteView.setOnClickPendingIntent(R.id.watch_widget_configure, PendingIntent.getActivity(context,
												  0,
												  widgetConfigure,
												  PendingIntent.FLAG_CANCEL_CURRENT));
	
	remoteView.setOnClickPendingIntent(R.id.watch_widget_content,
					   getWidgetUpdatePendingIntent(context, appWidgetId, true));
	
	appWidgetManager.updateAppWidget(appWidgetId, remoteView);
	registerWidgetUpdateIntent(context, appWidgetId);
	WatchWidgetService.registerWidget(appWidgetId);
    }

    
    @Override
    public void onEnabled(Context context) {
	context.startService(new Intent(context, WatchWidgetService.class));
    }

    @Override
    public void onDisabled(Context context) {
	context.stopService(new Intent(context, WatchWidgetService.class));
    }

    private void appendFromStream(StringBuffer output, InputStream stream) throws IOException {
	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	byte[] buffer = new byte[1024];
	int read = 0;
	while ((read = stream.read(buffer)) != -1) {
	    bytes.write(buffer, 0, read);
	}
	output.append(new String(bytes.toByteArray()));
    }

    private String getInput(SharedPreferences config) throws IOException {
	String type = config.getString(WatchWidgetConfiguration.TYPE, WatchWidgetConfiguration.TYPE_FILE);
	if (type.equals(WatchWidgetConfiguration.TYPE_FILE)) {
	    String path = config.getString(WatchWidgetConfiguration.PATH, "");
	    if (path.equals("")) {
		throw new IOException("Not configured");
	    } else {
		StringBuffer buffer = new StringBuffer();
		FileInputStream stream = new FileInputStream(path);
		try {
		    appendFromStream(buffer, stream);
		    return buffer.toString();
		} finally {
		    stream.close();
		}
	    }
	} else {
	    String command = config.getString(WatchWidgetConfiguration.COMMAND, "");
	    if (command.equals("")) {
		throw new IOException("Not configured");
	    } else {
		StringBuffer buffer = new StringBuffer();
		java.lang.Process process = Runtime.getRuntime().exec(command);
		try {
		    InputStream input = process.getInputStream();
		    InputStream error = process.getErrorStream();
		    appendFromStream(buffer, input);
		    appendFromStream(buffer, error);
		    return buffer.toString();
		} finally {
		    try {
			process.waitFor();
		    } catch (InterruptedException e) {
			throw new IOException(e.getMessage());
		    }
		}
	    }
	}
    }
    
    private String getContent(SharedPreferences config) {
	try {
	    String input = getInput(config);
	    String regexp = config.getString(WatchWidgetConfiguration.REGEXP, "");
	    if (!regexp.equals("")) {
		return input.replaceAll(regexp, config.getString(WatchWidgetConfiguration.REPLACEMENT, ""));
	    } else {
		return input;
	    }
	} catch (Exception e) {
	    return e.getMessage();
	}
    }

    protected static Intent getWidgetUpdateIntent(Context context, int widgetId, boolean interactive) {
	Intent widgetUpdate = new Intent();
	widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
	widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { widgetId });
	if (interactive) {
	    widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(WatchWidgetProvider.URI_SCHEME + "://widget" + INTERACTIVE_PATH), String.valueOf(widgetId)));
	} else {
	    widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(WatchWidgetProvider.URI_SCHEME + "://widget" + AUTOMATED_PATH), String.valueOf(widgetId)));
	}
	return widgetUpdate;
    }

    private static PendingIntent getWidgetUpdatePendingIntent(Context context, int widgetId, boolean interactive) {
	return PendingIntent.getBroadcast(context, 
					  0, 
					  getWidgetUpdateIntent(context, widgetId, interactive), 
					  PendingIntent.FLAG_CANCEL_CURRENT);
    }

    protected static void registerWidgetUpdateIntent(Context context, int widgetId) {
	AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	SharedPreferences config = context.getSharedPreferences(WatchWidgetConfiguration.class.getName() + "." + widgetId, Context.MODE_PRIVATE);
	alarms.set(AlarmManager.ELAPSED_REALTIME, 
		   SystemClock.elapsedRealtime() + (config.getInt(WatchWidgetConfiguration.INTERVAL, 1) * 1000),
		   getWidgetUpdatePendingIntent(context, widgetId, false));
    }


    protected static void unRegisterWidgetUpdateIntent(Context context, int widgetId) {
	AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	alarms.cancel(getWidgetUpdatePendingIntent(context, widgetId, false));
    }

}

