package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.root.DeviceADB;
import org.rfcx.guardian.utility.device.control.DeviceBluetooth;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class BluetoothStateSetService extends IntentService {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BluetoothStateSetService.class);

	private static final String SERVICE_NAME = "BluetoothStateSet";

	public BluetoothStateSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		boolean prefsAdminEnableBluetooth = app.rfcxPrefs.getPrefAsBoolean("admin_enable_bluetooth");

		if (app.deviceBluetooth == null) {
			app.deviceBluetooth = new DeviceBluetooth(context);
		}

		if (prefsAdminEnableBluetooth && !app.deviceBluetooth.isBluetoothEnabled()) {

			Log.e(logTag, "DISCLAIMER:"
					+"\nBluetooth tethering (via RFCx) is currently UNSTABLE and is not recommended for anything other than testing or troubleshooting."
					+"\nFor wireless tethering, please use WiFi.");

			// turn power ON
			app.deviceBluetooth.setPowerOn();

			// wait for bluetooth to be enabled...
			while (!app.deviceBluetooth.isBluetoothEnabled()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
			// set network name
			app.deviceBluetooth.setNetworkName("rfcx-"+app.rfcxDeviceGuid.getDeviceGuid().substring(0,8));

			// turn tethering ON
			Log.e(logTag, "Bluetooth has been powered on but tethering will NOT be enabled at this time (see disclaimer above).");
			//app.deviceBluetooth.setTetheringOn();

		} else if (app.deviceBluetooth.isBluetoothEnabled()) {
			// turn tethering OFF
		//	app.deviceBluetooth.setTetheringOff();
			// turn power OFF
			app.deviceBluetooth.setPowerOff();

		}

	}
	
	
}
