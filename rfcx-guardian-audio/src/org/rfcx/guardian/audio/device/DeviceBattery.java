package org.rfcx.guardian.audio.device;

import org.rfcx.guardian.audio.RfcxGuardian;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class DeviceBattery {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceBattery.class.getSimpleName();
	
	public int getBatteryChargePercentage(Context context, Intent intent) {
		if (intent == null) intent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		return Math.round(100 * batteryLevel / (float) batteryScale);
	}
		
	
}

