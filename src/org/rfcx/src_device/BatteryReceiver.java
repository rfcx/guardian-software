package org.rfcx.src_device;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryReceiver extends BroadcastReceiver {

	private static final String TAG = BatteryReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
        setBatteryState(context, intent);
        allowOrDisAllowServices(context, intent);
        if (RfcxSource.verboseLog()) { Log.d(TAG,"onReceive()"); }
	}
	
	private void setBatteryState(Context context, Intent intent) {
		RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
		rfcxSource.deviceState.setBatteryLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
		rfcxSource.deviceState.setBatteryScale(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
		rfcxSource.deviceState.setBatteryTemperature(Math.round(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)/10));
		int batteryPct = rfcxSource.deviceState.getBatteryPercent();
		rfcxSource.deviceStateDb.dbBattery.insert(batteryPct);
	}
	
	private void allowOrDisAllowServices(Context context, Intent intent) {
		RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
		if (rfcxSource.deviceState.allowServices()) {
			Log.d(TAG, "Services SHOULD be allowed to run...");
		} else {
			Log.d(TAG, "Services should NOT be allowed to run...");
		}
	}
	
}
