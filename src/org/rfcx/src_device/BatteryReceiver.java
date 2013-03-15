package org.rfcx.src_device;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryReceiver extends BroadcastReceiver {

	private static final String TAG = BatteryReceiver.class.getSimpleName();
	
	private RfcxSource rfcxSource = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
        setBatteryState(context, intent);
        allowOrDisAllowServices(context, intent);
	}
	
	private void setBatteryState(Context context, Intent intent) {
		if (rfcxSource == null) rfcxSource = (RfcxSource) context.getApplicationContext();
		DeviceState deviceState = rfcxSource.deviceState;
		deviceState.setBatteryLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
		deviceState.setBatteryScale(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
		deviceState.setBatteryTemperature(Math.round(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)/10));
		rfcxSource.deviceStateDb.dbBattery.insert(deviceState.getBatteryPercent());
		if (RfcxSource.VERBOSE) Log.d(TAG, "BroadcastReceiver: "+TAG+" - Level: "+deviceState.getBatteryPercent());
	}
	
	private void allowOrDisAllowServices(Context context, Intent intent) {
		DeviceState deviceState = ((RfcxSource) context.getApplicationContext()).deviceState;
		if (deviceState.allowServices()) {
			Log.d(TAG, "Battery: "+deviceState.getBatteryPercent()+"% - Services Allowed.");
			if (rfcxSource.areServicesHalted_ExpensiveServices) {
				rfcxSource.launchAllServices(context);
			}
		} else {
			Log.d(TAG, "Battery: "+deviceState.getBatteryPercent()+"% - Services NOT Allowed.");
			rfcxSource.suspendExpensiveServices(context);
		}
	}
	
}
