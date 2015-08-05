package org.rfcx.guardian.updater.receiver;

import org.rfcx.guardian.updater.RfcxGuardianUpdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		((RfcxGuardianUpdater) context.getApplicationContext()).initializeRoleIntentServices(context);
	}

}

