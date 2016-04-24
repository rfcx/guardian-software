package org.rfcx.guardian.reboot.service;

import org.rfcx.guardian.reboot.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.ShellCommands;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+RebootIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".REBOOT";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".RECEIVE_REBOOT_NOTIFICATIONS";
	
	public RebootIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		Log.i(TAG, "Running RebootIntentService");
		(new ShellCommands()).executeCommand("reboot",null,false,context);
	}

}
