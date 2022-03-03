package org.rfcx.guardian.guardian.receiver;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.rfcx.guardian.guardian.RfcxGuardian;

public class ConnectivityReceiver extends BroadcastReceiver {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ConnectivityReceiver");
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        
        app.deviceConnectivity.updateConnectivityState(intent);

        app.apiMqttUtils.confirmOrCreateConnectionToBroker(false);
        
	}

}
