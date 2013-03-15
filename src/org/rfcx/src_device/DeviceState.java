package org.rfcx.src_device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.util.Log;

public class DeviceState {
	
	private static final String TAG = DeviceState.class.getSimpleName();
	
	// Services
	public static final boolean SERVICE_ENABLED = true;
	private static final int SERVICE_BATTERY_PERCENTAGE_THRESHOLD = 95;
	
	public boolean allowServices() {
		return (getBatteryPercent() > SERVICE_BATTERY_PERCENTAGE_THRESHOLD) ? true : false;
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
	}
	
	public int getLightLevel() {
		return lightLevel;
	}
	
	// CPU Clock Speed

	private int cpuClockSpeed;
	
	private void setCpuClockSpeed(int cpuClockSpeed) {
		this.cpuClockSpeed = cpuClockSpeed;
	}
	
	public int getCpuClockSpeed() {
		return cpuClockSpeed;
	}
	
	public void updateCpuClockSpeed() {
		File scaling_cur_freq = new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
		if (scaling_cur_freq.exists()) {
			try {
				FileInputStream fileInputStream = new FileInputStream(scaling_cur_freq);		
				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				int clockSpeed = Integer.parseInt(bufferedReader.readLine());
				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();
				setCpuClockSpeed(clockSpeed);
			}
			catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}
	
}
