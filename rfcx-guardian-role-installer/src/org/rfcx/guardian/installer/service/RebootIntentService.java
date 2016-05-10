package org.rfcx.guardian.installer.service;

import org.rfcx.guardian.installer.RfcxGuardian;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+RebootIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".REBOOT";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".RECEIVE_REBOOT_NOTIFICATIONS";
	
	public RebootIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		Log.d(TAG, "Running RebootIntentService");
		ShellCommands.executeCommand("reboot",null,false,context);
	}

}
