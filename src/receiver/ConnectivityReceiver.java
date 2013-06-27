package receiver;

import org.rfcx.src_android.RfcxSource;

import utility.TimeOfDay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

	private static final String TAG = ConnectivityReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
        RfcxSource app = (RfcxSource) context.getApplicationContext();
        TimeOfDay timeOfDay = new TimeOfDay();
        final boolean isConnected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		app.apiComm.setConnectivity(isConnected);
		if (app.verboseLogging) Log.d(TAG, "Connectivity Detected... (RfcxSource)");
		if (isConnected) {
			app.apiComm.sendAnyAlerts(context);
			if (timeOfDay.isDataGenerationEnabled(context) || app.ignoreOffHours) {
				if (app.verboseLogging) Log.d(TAG, "Check-in request allowed. Doing it.");
				app.apiComm.sendCheckIn(context);
			} else {
				if (app.verboseLogging) Log.d(TAG, "Check-in not allowed right now.");
			}
		}
	}

}
