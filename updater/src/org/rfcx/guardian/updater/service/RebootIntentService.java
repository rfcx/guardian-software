package org.rfcx.guardian.updater.service;

import org.rfcx.guardian.updater.RfcxGuardianUpdater;
import org.rfcx.guardian.utility.ShellCommands;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootIntentService extends IntentService {

	private static final String TAG = "RfcxGuardianUpdater-"+RebootIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.updater.REBOOT";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.updater.RECEIVE_REBOOT_NOTIFICATIONS";
	
	public RebootIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		RfcxGuardianUpdater app = (RfcxGuardianUpdater) getApplication();
		Context context = app.getApplicationContext();
		if (app.verboseLog) Log.d(TAG, "Running RebootIntentService");
		(new ShellCommands()).executeCommandAsRoot("reboot",null,context);
	}

}