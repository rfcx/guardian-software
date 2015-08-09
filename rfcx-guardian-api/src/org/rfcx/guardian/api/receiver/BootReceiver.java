package org.rfcx.guardian.api.receiver;

import org.rfcx.guardian.api.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		((RfcxGuardian) context.getApplicationContext()).initializeRoleServices(context);
	}

}

