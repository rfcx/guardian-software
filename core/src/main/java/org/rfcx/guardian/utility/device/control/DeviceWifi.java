package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceWifi {

	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceWifi.class);

	public DeviceWifi(Context context) {
		this.context = context;
	}

	private Context context;

	private boolean isWifiEnabled() {
		WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null) {
			int wifiState = wifiManager.getWifiState();
			switch (wifiState) {
				case WifiManager.WIFI_STATE_DISABLED:
	            		return false;
				case WifiManager.WIFI_STATE_DISABLING:
            			return false;
				case WifiManager.WIFI_STATE_ENABLED:
            			return true;
				case WifiManager.WIFI_STATE_ENABLING:
        				return true;
			}
		}
		return false;
	}
	
	public void setPowerOn() {
		if (!isWifiEnabled()) {
			WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
	    	Log.v(logTag, "Activating Wifi Power");
			wifiManager.setWifiEnabled(true);
		}
	}
	
	public void setPowerOff() {
		if (isWifiEnabled()) {
			WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
			Log.v(logTag, "Deactivating Wifi Power");
			wifiManager.setWifiEnabled(false);
		}
	}

	// ADB Connection controls
	// add some code here

	// Network Name controls

	public static void setNetworkName(String networkName) {
//		if (isBluetoothEnabled()) {
//			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//			if (bluetoothAdapter != null) && (!bluetoothAdapter.getName().equalsIgnoreCase(networkName)) {
//				bluetoothAdapter.setName(networkName);
//			}
//		}
	}



}
