package org.rfcx.guardian.system.receiver;

import java.util.Calendar;
import java.util.Date;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ConnectivityReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        app.isConnected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		if (app.isConnected) {
			app.lastConnectedAt = Calendar.getInstance().getTimeInMillis();
			Log.d(TAG, "Connectivity: YES");
			int disconnectedFor = (int) (app.lastConnectedAt - app.lastDisconnectedAt);
			if (disconnectedFor > 1000) app.deviceStateDb.dbOffline.insert(new Date(), disconnectedFor, "");
		} else {
			app.lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
			Log.d(TAG, "Connectivity: NO");
		}
        
	}

}
