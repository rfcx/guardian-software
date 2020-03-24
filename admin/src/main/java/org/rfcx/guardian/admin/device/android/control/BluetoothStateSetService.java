package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
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

		if (prefsAdminEnableBluetooth) {
			// turn power ON
			DeviceBluetooth.setPowerOn();
			// turn tethering ON
			DeviceBluetooth deviceBluetooth = new DeviceBluetooth(context);
			deviceBluetooth.setTetheringOn();

			DeviceBluetooth.setNetworkName("rfcx-"+app.rfcxDeviceGuid.getDeviceGuid().substring(0,8));

		} else {
			// turn power OFF
			DeviceBluetooth.setPowerOff(context);
			// we do not disable tethering, as it is not relevant when bluetooth is off

		}


	}
	
	
}