package org.rfcx.guardian.installer.receiver;

import org.rfcx.guardian.installer.RfcxGuardianInstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		((RfcxGuardianInstaller) context.getApplicationContext()).initializeInstallerIntentServices(context);
	}

}

