package org.rfcx.guardian.system.device;

import java.util.Date;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.RfcxConstants;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.text.TextUtils;
import android.util.Log;

public class DeviceState {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceState.class.getSimpleName();
	
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
	
	private LocationManager locationManager;
	private double geoLocationLatitude = 0;
	private double geoLocationLongitude = 0;
	private double geoLocationPrecision = 0;

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
	
//	private void updateGeoLocation() {
//		
//		this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//		Criteria criteria = new Criteria();
//		String bestProvider = locationManager.getBestProvider(criteria, false);
//		Location location = locationManager.getLastKnownLocation(bestProvider);
//		try {
//			this.geoLocationLatitude = (double) location.getLatitude();
//			this.geoLocationLongitude = (double) location.getLongitude();
//			this.geoLocationPrecision = 0;
//		} catch (Exception e) {
//			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//		}
//	}
	
	public double[] getGeoLocation() {
		return new double[] { this.geoLocationLatitude, this.geoLocationLongitude, this.geoLocationPrecision };
	}
	
	
	
}
