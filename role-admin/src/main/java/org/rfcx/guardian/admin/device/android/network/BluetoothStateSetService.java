package org.rfcx.guardian.admin.device.android.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.control.DeviceBluetooth;
import org.rfcx.guardian.utility.device.control.DeviceWifi;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class BluetoothStateSetService extends IntentService {

	public static final String SERVICE_NAME = "BluetoothStateSet";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "BluetoothStateSetService");

	public BluetoothStateSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();

		String prefsAdminBluetoothFunction = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_BLUETOOTH_FUNCTION);
		boolean enableBluetoothPan = prefsAdminBluetoothFunction.equalsIgnoreCase("pan");

		String[] authCreds = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_HOTSPOT_AUTH_CREDS).split(":");
		String panName = !authCreds[0].equalsIgnoreCase("[ssid]") ? authCreds[0] : "rfcx-"+app.rfcxGuardianIdentity.getGuid().substring(0,8);
//		String wifiPswd = !authCreds[1].equalsIgnoreCase("[password]") ? authCreds[1] : "rfcxrfcx";

		DeviceBluetooth deviceBluetooth = new DeviceBluetooth(context);

		if (enableBluetoothPan) {
			// turn BT PAN ON
			deviceBluetooth.setPowerOn();
			deviceBluetooth.setPanName(panName);
			deviceBluetooth.setPanOn();

		} else {
			// turn BT PAN OFF
			deviceBluetooth.setPanOff();
			deviceBluetooth.setPowerOff();

		}

	}
	
	
}
