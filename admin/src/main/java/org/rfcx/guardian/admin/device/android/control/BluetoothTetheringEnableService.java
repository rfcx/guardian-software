package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.control.DeviceBluetooth;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class BluetoothTetheringEnableService extends IntentService {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BluetoothTetheringEnableService.class);

	private static final String SERVICE_NAME = "BluetoothTetheringEnable";

	public BluetoothTetheringEnableService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		DeviceBluetooth deviceBluetooth = new DeviceBluetooth(((RfcxGuardian) getApplication()).getApplicationContext());
		deviceBluetooth.enableTethering();

	}
	
	
}
