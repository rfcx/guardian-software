package org.rfcx.guardian.receiver;

import java.util.Date;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class ConnectivityReceiver extends BroadcastReceiver {
	
	private String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ConnectivityReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        
        int disconnectedFor = 
        		app.deviceConnectivity.updateConnectivityStateAndReportDisconnectedFor(
        			!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)
        		);
        
        if (disconnectedFor > 1000) {
        		app.deviceSystemDb.dbOffline.insert(new Date(), disconnectedFor, "");
        }
        
	}

}
