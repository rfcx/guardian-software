package org.rfcx.guardian.reboot.service;

import org.rfcx.guardian.reboot.R;
import org.rfcx.guardian.reboot.RfcxGuardian;
import org.rfcx.guardian.utility.ShellCommands;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+R.string.log_name+"-"+RebootIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.reboot.REBOOT";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.reboot.RECEIVE_REBOOT_NOTIFICATIONS";
	
	public RebootIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		if (app.verboseLog) Log.d(TAG, "Running RebootIntentService");
		(new ShellCommands()).executeCommand("reboot",null,false,context);
	}

}
