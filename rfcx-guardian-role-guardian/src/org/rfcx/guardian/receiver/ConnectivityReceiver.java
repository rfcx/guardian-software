package org.rfcx.guardian.receiver;

import java.util.Date;

import org.rfcx.guardian.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class ConnectivityReceiver extends BroadcastReceiver {
	
	public ConnectivityReceiver(String appRole) {
		this.logTag = "Rfcx-"+appRole+"-"+ConnectivityReceiver.class.getSimpleName();
	}

	private String logTag = "Rfcx-Utils-"+ConnectivityReceiver.class.getSimpleName();
	
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
