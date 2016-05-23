package org.rfcx.guardian.system.receiver;

import java.util.Date;

import org.rfcx.guardian.system.RfcxGuardian;

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
        
        if (disconnectedFor > 1000) {
        	app.deviceStateDb.dbOffline.insert(new Date(), disconnectedFor, "");
        }
        
	}

}
