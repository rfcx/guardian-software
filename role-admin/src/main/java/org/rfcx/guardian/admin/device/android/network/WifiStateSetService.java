package org.rfcx.guardian.admin.device.android.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.control.DeviceWifi;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class WifiStateSetService extends IntentService {

	public static final String SERVICE_NAME = "WifiStateSet";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "WifiStateSetService");

	public WifiStateSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		String prefsAdminWifiFunction = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION);
		boolean enableWifiAsHotspot = prefsAdminWifiFunction.equalsIgnoreCase("hotspot");
		boolean enableWifiAsClient = prefsAdminWifiFunction.equalsIgnoreCase("client");

		DeviceWifi deviceWifi = new DeviceWifi(context);
		deviceWifi.setHotspotConfig(
				"rfcx-"+app.rfcxGuardianIdentity.getGuid().substring(0,8),
				app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_HOTSPOT_PASSWORD),
				true);

		if (enableWifiAsHotspot) {
			// turn hotspot ON
			deviceWifi.setPowerOff(); // wifi must be turned off before hotspot is enabled
//			Log.e(logTag, "Blocking WiFi hotspot function because of an apparent bug in DeviceWiFi (core library) that spikes CPU usage. This needs to be fixed...");
			deviceWifi.setHotspotOn();

		} else if (enableWifiAsClient) {

			// turn Wifi power on
			deviceWifi.setPowerOn();

		} else {
			// turn hotspot OFF
			deviceWifi.setPowerOff();
//			Log.e(logTag, "Blocking WiFi hotspot function because of an apparent bug in DeviceWiFi (core library) that spikes CPU usage. This needs to be fixed...");
			deviceWifi.setHotspotOff();

		}

		if (enableWifiAsClient) {

			// turn Wifi power on
			deviceWifi.setPowerOn();

		}

	}
	
	
}
