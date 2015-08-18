package org.rfcx.guardian.carrier.receiver;

import org.rfcx.guardian.carrier.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		((RfcxGuardian) context.getApplicationContext()).initializeRoleServices(context);
	}

}

