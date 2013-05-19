package org.rfcx.src_device;

import org.rfcx.src_android.RfcxSource;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class DeviceState {
	
	private static final String TAG = DeviceState.class.getSimpleName();
	
	// Services
	public static final boolean SERVICE_ENABLED = true;
	private static final int SERVICE_BATTERY_PERCENTAGE_THRESHOLD = 98;

	public int serviceSamplesPerMinute = 60;
	
	// Battery
	private int batteryLevel;
	private int batteryScale;
	private int batteryTemperature;
	private boolean batteryDisCharging;
	private boolean batteryCharged;
	
	private void setBatteryLevel(int batteryLevel) {
		this.batteryLevel = batteryLevel;
	}
	
	private void setBatteryScale(int batteryScale) {
		this.batteryScale = batteryScale;
	}
	
	private void setBatteryTemperature(int batteryTemperature) {
		this.batteryTemperature = batteryTemperature;
	}
	
	private void setBatteryDisCharging(int batteryStatus) {
		this.batteryDisCharging = (batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING);
	}
	
	private void setBatteryCharged(int batteryStatus) {
		this.batteryCharged = (batteryStatus == BatteryManager.BATTERY_STATUS_FULL);
	}
	
	public int getBatteryPercent() {
		return Math.round(100 * this.batteryLevel / (float) this.batteryScale);
	}
	
	public boolean isBatteryDisCharging() {
		return this.batteryDisCharging;
	}
	
	public boolean isBatteryCharged() {
		return this.batteryCharged;
	}
	
	public int getBatteryTemperature() {
		return batteryTemperature;
	}

	public void setBatteryState(Context context, Intent intent) {
		
		if (intent == null) intent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		setBatteryLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
		setBatteryScale(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
		setBatteryDisCharging(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
		setBatteryCharged  (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
		setBatteryTemperature(Math.round(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)/10));
		allowOrDisAllowServices(context);
	}
	
	private void allowOrDisAllowServices(Context context) {
		RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
		if (getBatteryPercent() > SERVICE_BATTERY_PERCENTAGE_THRESHOLD) {
			rfcxSource.setLowPowerMode(false);
			Log.d(TAG, "Battery: "+getBatteryPercent()+"% - System in Normal Power Mode.");
		} else if (!rfcxSource.isInLowPowerMode()){
			rfcxSource.setLowPowerMode(true);
			Log.d(TAG, "Battery: "+getBatteryPercent()+"% - System in Low Power Mode.");
		}
	}
	
	
	// Light Sensor
	private int lightLevel;
	
	public void setLightLevel(int lightLevel) {
		this.lightLevel = lightLevel;
	}
	
	public int getLightLevel() {
		return lightLevel;
	}
	
	
	
	
	
}
