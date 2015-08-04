package org.rfcx.guardian.installer.service;

import org.rfcx.guardian.installer.RfcxGuardianInstaller;
import org.rfcx.guardian.utility.ShellCommands;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootIntentService extends IntentService {

	private static final String TAG = "RfcxGuardianInstaller-"+RebootIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.installer.REBOOT";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.installer.RECEIVE_REBOOT_NOTIFICATIONS";
	
	public RebootIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		RfcxGuardianInstaller app = (RfcxGuardianInstaller) getApplication();
		Context context = app.getApplicationContext();
		if (app.verboseLog) Log.d(TAG, "Running RebootIntentService");
		(new ShellCommands()).executeCommandAsRoot("reboot",null,context);
	}

}
