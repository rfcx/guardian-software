package org.rfcx.guardian.receiver;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	private String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BootReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		// record boot time in database
		((RfcxGuardian) context.getApplicationContext()).rebootDb.dbReboot.insert(System.currentTimeMillis());
		
		// launch background services
		((RfcxGuardian) context.getApplicationContext()).initializeRoleServices();
	
	}

}

