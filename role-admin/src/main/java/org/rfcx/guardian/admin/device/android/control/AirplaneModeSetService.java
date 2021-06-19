package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.control.DeviceSystemProperties;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class AirplaneModeSetService extends IntentService {

	public static final String SERVICE_NAME = "AirplaneModeSet";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AirplaneModeSetService");

	public AirplaneModeSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_AIRPLANE_MODE)) {
			app.deviceAirplaneMode.setOn(app.getApplicationContext());
		} else {
			app.deviceAirplaneMode.setOff(app.getApplicationContext());
		}
	
	}
	
	
}
