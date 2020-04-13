package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.control.DeviceADB;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ADBStateSetService extends IntentService {

	private static final String SERVICE_NAME = "ADBStateSet";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ADBStateSetService");

	public ADBStateSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		// set ADB networking state
		boolean prefsAdminEnableAdb = app.rfcxPrefs.getPrefAsBoolean("admin_enable_tcp_adb");
		DeviceADB.setADBoverTCP(prefsAdminEnableAdb, app.getApplicationContext());
	
	}
	
	
}
