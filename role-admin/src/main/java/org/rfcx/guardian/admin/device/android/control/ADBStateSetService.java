package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.control.DeviceSystemProperties;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class ADBStateSetService extends IntentService {

	public static final String SERVICE_NAME = "ADBStateSet";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ADBStateSetService");

	public static final int DEFAULT_TCP_PORT = 7329; // RFCX (7329)

	public ADBStateSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		// set ADB networking state
		boolean prefsAdminEnableAdb = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_TCP_ADB);

		boolean prefsAdminEnableWifi = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_WIFI);
		if (prefsAdminEnableAdb && !prefsAdminEnableWifi) {
			Log.e(logTag,"ADB over TCP could not be enabled because 'admin_enable_wifi' is disabled");
		}

		boolean enableOrDisable = prefsAdminEnableAdb && prefsAdminEnableWifi;
		Log.v(logTag, ((enableOrDisable) ? "Enabling" : "Disabling") + " ADB over TCP on port "+DEFAULT_TCP_PORT);
		DeviceSystemProperties.setVal("persist.adb.tcp.port", (enableOrDisable) ? ""+DEFAULT_TCP_PORT : "");
	
	}
	
	
}
