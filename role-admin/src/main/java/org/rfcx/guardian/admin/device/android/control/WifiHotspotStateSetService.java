package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.control.DeviceWifi;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class WifiHotspotStateSetService extends IntentService {

	public static final String SERVICE_NAME = "WifiHotspot";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "WifiHotspotStateSetService");

	public WifiHotspotStateSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		boolean prefsAdminEnableWifiHotspot = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_WIFI_HOTSPOT);

		DeviceWifi deviceWifi = new DeviceWifi(context);
		deviceWifi.setHotspotConfig(
				"rfcx-"+app.rfcxGuardianIdentity.getGuid().substring(0,8),
				app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_HOTSPOT_PASSWORD),
				true);

		if (prefsAdminEnableWifiHotspot) {
			// turn hotspot ON
			deviceWifi.setPowerOff(); // wifi must be turned off before hotspot is enabled
//			Log.e(logTag, "Blocking WiFi hotspot function because of an apparent bug in DeviceWiFi (core library) that spikes CPU usage. This needs to be fixed...");
			deviceWifi.setHotspotOn();

		} else {
			// turn hotspot OFF
			deviceWifi.setPowerOff();
//			Log.e(logTag, "Blocking WiFi hotspot function because of an apparent bug in DeviceWiFi (core library) that spikes CPU usage. This needs to be fixed...");
			deviceWifi.setHotspotOff();

		}

	}
	
	
}
