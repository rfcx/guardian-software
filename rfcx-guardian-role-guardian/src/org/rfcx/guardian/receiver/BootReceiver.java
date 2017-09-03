package org.rfcx.guardian.receiver;

import org.rfcx.guardian.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		// record boot time in database
		((RfcxGuardian) context.getApplicationContext()).deviceRebootDb.dbReboot.insert(System.currentTimeMillis());
		
		// launch background services
		((RfcxGuardian) context.getApplicationContext()).initializeRoleServices();
	
	}

}

