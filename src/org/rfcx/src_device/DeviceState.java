package org.rfcx.src_device;

import android.util.Log;

public class DeviceState {
	
	private static final String TAG = DeviceState.class.getSimpleName();
	
	// Services
	public static final boolean SERVICE_ENABLED = true;
	private static final int SERVICE_BATTERY_PERCENTAGE_THRESHOLD = 95;
	
	public boolean allowServices() {
		return (getBatteryPercent() >= SERVICE_BATTERY_PERCENTAGE_THRESHOLD) ? true : false;
	}
	
	// Battery
	private int batteryLevel;
	private int batteryScale;
	private int batteryTemperature;
	
	public void setBatteryLevel(int batteryLevel) {
		this.batteryLevel = batteryLevel;
	}
	
	public void setBatteryScale(int batteryScale) {
		this.batteryScale = batteryScale;
	}
	
	public int getBatteryLevel() {
		return batteryLevel;
	}
	
	public int getBatteryScale() {
		return batteryScale;
	}
	
	public int getBatteryPercent() {
		return Math.round(100 * this.batteryLevel / (float) this.batteryScale);
	}
	
	public int getBatteryTemperature() {
		return batteryTemperature;
	}
	
	public void setBatteryTemperature(int batteryTemperature) {
		this.batteryTemperature = batteryTemperature;
	}
	
	// Light Sensor
	private int lightLevel;
	
	public void setLightLevel(int lightLevel) {
		this.lightLevel = lightLevel;
		Log.d(TAG, "Light level: "+lightLevel);
	}
	
	public int getLightLevel() {
		return lightLevel;
	}
	
}
