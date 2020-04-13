package org.rfcx.guardian.admin.device.android.control;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ForceRoleRelaunchService extends IntentService {

	private static final String SERVICE_NAME = "ForceRoleRelaunch";
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ForceRoleRelaunchService");
		
	public ForceRoleRelaunchService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		Log.e(logTag, "ROLE RELAUNCH NEEDS TO BE RE-WRITTEN BECAUSE 'ps' AND 'cut' DID NOT WORK ON ORANGE PI. AT THIS TIME THIS COMMAND DOES NOTHING.");
//		DeviceAndroidApps.killAndReLaunchGuardianAppRoles(new String[] { "org.rfcx.guardian.guardian", "org/rfcx/guardian/admin"}, ((RfcxGuardian) getApplication()).getApplicationContext());
	}
	
	
}
