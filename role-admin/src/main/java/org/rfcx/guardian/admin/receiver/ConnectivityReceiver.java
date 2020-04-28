package org.rfcx.guardian.admin.receiver;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import org.rfcx.guardian.admin.RfcxGuardian;

import java.util.Date;

public class ConnectivityReceiver extends BroadcastReceiver {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ConnectivityReceiver");
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        
        int disconnectedFor = app.deviceConnectivity.updateConnectivityStateAndReportDisconnectedFor( !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) );

		if (disconnectedFor > 1000) { app.deviceSystemDb.dbOffline.insert(new Date(), disconnectedFor, ""); }

	}

}
