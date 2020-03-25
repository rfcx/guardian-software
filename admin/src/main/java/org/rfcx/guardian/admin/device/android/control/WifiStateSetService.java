package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.control.DeviceADB;
import org.rfcx.guardian.utility.device.control.DeviceBluetooth;
import org.rfcx.guardian.utility.device.control.DeviceWifi;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class WifiStateSetService extends IntentService {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, WifiStateSetService.class);

	private static final String SERVICE_NAME = "WifiStateSet";

	public WifiStateSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		boolean prefsAdminEnableWifi = app.rfcxPrefs.getPrefAsBoolean("admin_enable_wifi");

		DeviceWifi deviceWifi = new DeviceWifi(context);
		deviceWifi.setHotspotConfig("rfcx-"+app.rfcxDeviceGuid.getDeviceGuid().substring(0,8), "rfcxrfcx", true);

		if (prefsAdminEnableWifi) {
			// turn hotspot ON
			deviceWifi.setPowerOff(); // wifi must be turned off before hotspot is enabled
			deviceWifi.setHotspotOn();
			// turn adb networking ON
			DeviceADB.enableADBoverTCP(4455, context);
		} else {
			// turn hotspot OFF
			deviceWifi.setHotspotOff();
			// turn adb networking ON
			DeviceADB.disableADBoverTCP(context);

		}


	}
	
	
}
