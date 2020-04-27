package org.rfcx.guardian.utility.device.capture;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.text.TextUtils;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceBattery {
	
	public DeviceBattery(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceBattery");
	}
	
	private String logTag;
	
	private Intent getIntent(Context context, Intent intent) {
		return (intent == null) ? intent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)) : intent;
	}
	
	public int[] getBatteryState(Context context, Intent intent) {
		return new int[] { 
				getBatteryChargePercentage(context, intent),
				getBatteryTemperature(context, intent),
				(isBatteryCharging(context, intent)) ? 1 : 0,
				(isBatteryCharged(context, intent)) ? 1 : 0
		};
	}

	public String getBatteryStateAsConcatString(Context context, Intent intent) {
		String[] batteryState = new String[] {
				""+System.currentTimeMillis(),
				""+getBatteryChargePercentage(context, intent),
				""+getBatteryTemperature(context, intent),
				""+((isBatteryCharging(context, intent)) ? 1 : 0),
				""+((isBatteryCharged(context, intent)) ? 1 : 0)
		};
		return TextUtils.join("*", batteryState);
	}
	
	public int getBatteryChargePercentage(Context context, Intent intent) {
		int batteryLevel = getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int batteryScale = getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		return Math.round(100 * batteryLevel / (float) batteryScale);
	}
	
	private int getBatteryTemperature(Context context, Intent intent) {
		return Math.round(getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)/10);
	}
	
	public boolean isBatteryCharged(Context context, Intent intent) {
		return ( getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL );
	}
	
	private boolean isBatteryCharging(Context context, Intent intent) {
		return ( getIntent(context,intent).getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING );
	}

	private boolean isBatteryDischarging(Context context, Intent intent) {
		return !isBatteryCharging(context, intent);
	}
	
}

