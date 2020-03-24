package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

	public boolean isHotspotEnabled() {
		WifiManager wifiManager = (WifiManager) this.context.getSystemService(context.WIFI_SERVICE);
		try {
			Method wifiManagerMethods = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
			wifiManagerMethods.setAccessible(true);
			return (Boolean) wifiManagerMethods.invoke(wifiManager);
		} catch (NoSuchMethodException e) {
			RfcxLog.logExc(logTag, e);
		} catch (IllegalAccessException e) {
			RfcxLog.logExc(logTag, e);
		} catch (InvocationTargetException e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}

	public void setHotspotConfig() {

	}

	public void setHotspotOn() {

	}

	public void setHotspotOff() {

	}

}
