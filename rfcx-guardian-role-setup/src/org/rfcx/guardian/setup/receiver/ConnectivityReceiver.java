package org.rfcx.guardian.setup.receiver;

import java.util.Calendar;

import org.rfcx.guardian.setup.R;
import org.rfcx.guardian.setup.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class ConnectivityReceiver extends BroadcastReceiver {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ConnectivityReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        
        int disconnectedFor = 
        		app.deviceConnectivity.updateConnectivityStateAndReportDisconnectedFor(
        			!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)
        		);
    
		// added to ensure that multiple checkins don't occur at each connectivity reception
		if (app.lastApiCheckTriggeredAt < (app.deviceConnectivity.lastConnectedAt() - 2000)) {
			if (app.apiCore.allowTriggerCheckIn()) {
				app.rfcxServiceHandler.triggerService("ApiCheckVersion",true);
			}
		}
			
	
	}

	
	
	
	
}
