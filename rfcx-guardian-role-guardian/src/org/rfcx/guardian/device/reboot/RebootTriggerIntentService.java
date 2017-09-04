package org.rfcx.guardian.device.reboot;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;

public class RebootTriggerIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+RebootTriggerIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "RebootTrigger";
	
	public RebootTriggerIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		
		((RfcxGuardian) getApplication()).deviceRebootUtils.executeDeviceReboot();
//		ShellCommands.executeCommand("reboot",null,false,((RfcxGuardian) getApplication()).getApplicationContext());
	}

}
