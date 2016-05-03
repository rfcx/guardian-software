package org.rfcx.guardian.sentinel.receiver;

import org.rfcx.guardian.sentinel.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		((RfcxGuardian) context.getApplicationContext()).initializeRoleServices(context);
	}

}

