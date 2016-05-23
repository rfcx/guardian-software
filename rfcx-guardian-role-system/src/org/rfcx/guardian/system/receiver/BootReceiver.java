package org.rfcx.guardian.system.receiver;

import org.rfcx.guardian.system.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		((RfcxGuardian) context.getApplicationContext()).rebootDb.dbReboot.insert(System.currentTimeMillis());
//		((RfcxGuardian) context.getApplicationContext()).initializeRoleServices();
	}

}

