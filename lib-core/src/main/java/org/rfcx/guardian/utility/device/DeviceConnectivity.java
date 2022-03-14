package org.rfcx.guardian.utility.device;

import android.content.Intent;
import android.net.ConnectivityManager;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceConnectivity {

    private final String logTag;
    private boolean isConnected = false;
    private long lastConnectedAt = System.currentTimeMillis();
    private long lastDisconnectedAt = System.currentTimeMillis();
    public DeviceConnectivity(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceConnectivity");
    }

    public void updateConnectivityState(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected) {
            this.lastConnectedAt = System.currentTimeMillis();
        } else {
            this.lastDisconnectedAt = System.currentTimeMillis();
        }
    }

    public void updateConnectivityState(Intent intent) {
        updateConnectivityState(!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
    }

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

    public int updateConnectivityStateAndReportDisconnectedFor(Intent intent) {
        return updateConnectivityStateAndReportDisconnectedFor(!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
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
