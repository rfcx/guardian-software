package admin.receiver;

import java.util.Date;

import rfcx.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import admin.RfcxGuardian;

public class ConnectivityReceiver extends BroadcastReceiver {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ConnectivityReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        
        int disconnectedFor = app.deviceConnectivity.updateConnectivityStateAndReportDisconnectedFor( !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) );
        
	}

}
