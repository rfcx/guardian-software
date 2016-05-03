package org.rfcx.guardian.utility.device;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class DeviceBattery {

	private static final String TAG = "Rfcx-Utils-"+DeviceBattery.class.getSimpleName();
	
	private Intent getIntent(Context context, Intent intent) {
		return (intent == null) ? intent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)) : intent;
	}
	
	public int getBatteryChargePercentage(Context context, Intent intent) {
		int batteryLevel = getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int batteryScale = getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		return Math.round(100 * batteryLevel / (float) batteryScale);
	}
	
	public int getBatteryTemperature(Context context, Intent intent) {
		return Math.round(getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)/10);
	}
	
	public boolean isBatteryCharged(Context context, Intent intent) {
		return ( getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL );
	}
	
	public boolean isBatteryDischarging(Context context, Intent intent) {
		return ( getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_DISCHARGING );
	}
	
}

