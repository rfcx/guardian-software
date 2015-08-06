package org.rfcx.guardian.system.device;

import java.util.Date;

import org.rfcx.guardian.utility.DateTimeUtils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.os.BatteryManager;

public class DeviceState {
	
	public int serviceSamplesPerMinute = 60;
	
	// Battery
	private int batteryLevel;
	private int batteryScale;
	private int batteryTemperature;
	private boolean batteryDisCharging;
	private boolean batteryCharged;
	
	private Date trafficStatsStart = new Date();
	private Date trafficStatsEnd = new Date();
	private long trafficStatsReceived = 0;
	private long trafficStatsSent = 0;
	private long trafficStatsReceivedTotal = 0;
	private long trafficStatsSentTotal = 0;

	private DateTimeUtils dateTimeUtils = new DateTimeUtils();
	
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
		setBatteryCharged(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
		setBatteryTemperature(Math.round(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)/10));
	}
		
	
	public long[] updateDataTransferStats() {
		
		long mobileRxBytes = TrafficStats.getMobileRxBytes();
		long mobileTxBytes = TrafficStats.getMobileTxBytes();
		
		this.trafficStatsStart = this.trafficStatsEnd;
		this.trafficStatsEnd = new Date();
		this.trafficStatsReceived = mobileRxBytes - this.trafficStatsReceivedTotal;
		this.trafficStatsSent = mobileTxBytes - this.trafficStatsSentTotal;
		this.trafficStatsReceivedTotal = mobileRxBytes;
		this.trafficStatsSentTotal = mobileTxBytes;
				
		return new long[] { this.trafficStatsStart.getTime(), this.trafficStatsEnd.getTime(), this.trafficStatsReceived, this.trafficStatsSent, this.trafficStatsReceivedTotal, this.trafficStatsSentTotal };
	}
	
	
	
	
}
