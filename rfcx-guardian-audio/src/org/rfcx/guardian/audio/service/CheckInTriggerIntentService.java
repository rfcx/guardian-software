package org.rfcx.guardian.audio.service;

import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class CheckInTriggerIntentService extends IntentService {
	
	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+CheckInTriggerIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".CHECKIN_TRIGGER";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".RECEIVE_CHECKIN_TRIGGER_NOTIFICATIONS";
	
	public CheckInTriggerIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		Log.d(TAG,"TRIGGERING CHECK-IN...");
	
	}

}
