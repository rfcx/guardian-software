package org.rfcx.guardian.receiver;

import java.util.Calendar;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.TimeOfDay;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

	private static final String TAG = ConnectivityReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        long timeStamp = Calendar.getInstance().getTimeInMillis();
        
        final boolean isConnected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        app.isConnected = isConnected;
        
		if (isConnected) {
			app.lastConnectedAt = timeStamp;
			int disconnectedFor = (int) (timeStamp - app.lastDisconnectedAt);
			if (app.verboseLog) { Log.d(TAG, "Disconnected for: "+disconnectedFor+"ms"); }
		// 1000ms is an arbitrarily chosen cut-off. We might want to revisit this choice at some point.
			if (disconnectedFor > 1000) {
				app.deviceStateDb.dbNetworkSearch.insert(disconnectedFor);
			}
		} else {
			app.lastDisconnectedAt = timeStamp;
		}
	}

}
