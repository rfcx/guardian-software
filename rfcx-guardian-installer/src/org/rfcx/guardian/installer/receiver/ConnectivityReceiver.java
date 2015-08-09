package org.rfcx.guardian.installer.receiver;

import java.util.Calendar;

import org.rfcx.guardian.installer.R;
import org.rfcx.guardian.installer.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class ConnectivityReceiver extends BroadcastReceiver {

	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.RfcxConstants.ROLE_NAME+"-"+ConnectivityReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		app.isConnected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		if (app.isConnected) {
			app.lastConnectedAt = Calendar.getInstance().getTimeInMillis();
			// added to ensure that multiple checkins don't occur at each connectivity reception
			if (app.lastApiCheckTriggeredAt < (app.lastConnectedAt-2000)) {
				if (app.apiCore.allowTriggerCheckIn()) {
					app.triggerService("ApiCheckVersion",true);
				}
			}
		} else {
			app.lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
		}
	}

	
	
	
	
}
