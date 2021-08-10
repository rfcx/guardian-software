package org.rfcx.guardian.guardian.companion;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.api.methods.clocksync.ClockSyncJobService;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class SocketServerSetService extends IntentService {

	public static final String SERVICE_NAME = "SocketServerSet";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SocketServerSetService");

	public SocketServerSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		boolean prefsAdminEnableWifiSocket = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_WIFI_SOCKET);
		String prefsAdminWifiFunction = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION);
		boolean isWifiEnabled = prefsAdminWifiFunction.equals("hotspot") || prefsAdminWifiFunction.equals("client");

		if (prefsAdminEnableWifiSocket && !isWifiEnabled) {
			Log.e( logTag, "WiFi Socket Server could not be enabled because 'admin_wifi_function' is set to off.");
		}

		try {
			if (prefsAdminEnableWifiSocket && isWifiEnabled) {
				app.apiSocketUtils.startServer();
			} else {
				app.apiSocketUtils.stopServer();
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

	
	}
	
	
}
