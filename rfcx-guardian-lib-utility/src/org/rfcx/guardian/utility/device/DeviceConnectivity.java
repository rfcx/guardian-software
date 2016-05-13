package org.rfcx.guardian.utility.device;

import java.util.Calendar;
import java.util.Date;

import android.util.Log;


public class DeviceConnectivity {
	
	public DeviceConnectivity(String appRole) {
		this.logTag = "Rfcx-"+appRole+"-"+DeviceConnectivity.class.getSimpleName();
	}
	
	private String logTag = "Rfcx-Utils-"+DeviceConnectivity.class.getSimpleName();
	
	private boolean isConnected = false;
	private long lastConnectedAt = Calendar.getInstance().getTimeInMillis();
	private long lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
	
	public int updateConnectivityStateAndReportDisconnectedFor(boolean isConnected) {
		this.isConnected = isConnected;
		if (isConnected) {
			this.lastConnectedAt = Calendar.getInstance().getTimeInMillis();
//			Log.d(TAG, "Connectivity: YES");
			int disconnectedFor = (int) (this.lastConnectedAt - this.lastDisconnectedAt);
			return disconnectedFor;
		} else {
			this.lastDisconnectedAt = Calendar.getInstance().getTimeInMillis();
			return 0;
//			Log.d(TAG, "Connectivity: NO");
		}
	}
	
	public boolean isConnected() {
		return this.isConnected;
	}
	
	public long lastConnectedAt() {
		return this.lastConnectedAt;
	}

	public long lastDisconnectedAt() {
		return this.lastDisconnectedAt;
	}
	
	
	
}
