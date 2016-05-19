package org.rfcx.guardian.setup.receiver;

import org.rfcx.guardian.setup.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
//		((RfcxGuardian) context.getApplicationContext()).initializeRoleServices();
	}

}

