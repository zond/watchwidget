
package cx.ath.troja.android.watchwidget;

import java.util.concurrent.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;

import java.util.*;

import com.darekxan.insanity.toolbox.*;

public class WatchWidgetService extends Service {

    private static WatchWidgetService instance = null;

    protected static void registerWidget(int widgetId) {
	if (instance != null) {
	    instance.widgets.add(widgetId);
	}
    }

    protected static void unregisterWidget(int widgetId) {
	if (instance != null) {
	    instance.widgets.remove(widgetId);
	}
    }
    
    private class Receiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
	    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
		for (Integer widgetId : WatchWidgetService.this.widgets) {
		    WatchWidgetProvider.unRegisterWidgetUpdateIntent(context, widgetId);
		}
	    } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
		for (Integer widgetId : WatchWidgetService.this.widgets) {
		    context.sendBroadcast(WatchWidgetProvider.getWidgetUpdateIntent(context, widgetId, false));
		    WatchWidgetProvider.unRegisterWidgetUpdateIntent(context, widgetId);
		}
	    }
	}
    }

    private Receiver receiver = null;
    private HashSet<Integer> widgets = new HashSet<Integer>();
    
    @Override
    public void onCreate() {
	instance = this;
	receiver = new Receiver();
	registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
	registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }    

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	return START_STICKY;
    }

    @Override
    public void onDestroy() {
	unregisterReceiver(receiver);
	instance = null;
    }

}