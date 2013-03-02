package org.rfcx.src_state;

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
        if (RfcxSource.verboseLog()) { Log.d(TAG,"onReceive()"); }
	}
	
	private void setBatteryState(Context context, Intent intent) {
		RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
		rfcxSource.batteryState.setLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
		rfcxSource.batteryState.setScale(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
		rfcxSource.batteryState.setTemperature(Math.round(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)/10));
		int batteryPct = rfcxSource.batteryState.getPercent();
		rfcxSource.deviceStateDb.dbBattery.insert(batteryPct);
	}
	
}
