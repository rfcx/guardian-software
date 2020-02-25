package org.rfcx.guardian.utility.device;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceConnectivity {
	
	public DeviceConnectivity(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceConnectivity.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", DeviceConnectivity.class);
	
	private boolean isConnected = false;
	private long lastConnectedAt = System.currentTimeMillis();
	private long lastDisconnectedAt = System.currentTimeMillis();
	
	public int updateConnectivityStateAndReportDisconnectedFor(boolean isConnected) {
		this.isConnected = isConnected;
		if (isConnected) {
			this.lastConnectedAt = System.currentTimeMillis();
			int disconnectedFor = (int) (this.lastConnectedAt - this.lastDisconnectedAt);
			return disconnectedFor;
		} else {
			this.lastDisconnectedAt = System.currentTimeMillis();
			return 0;
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
