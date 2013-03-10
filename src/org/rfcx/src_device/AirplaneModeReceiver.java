package org.rfcx.src_device;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AirplaneModeReceiver extends BroadcastReceiver {

	private static final String TAG = AirplaneModeReceiver.class.getSimpleName();
	
	private WifiManager wifiManager = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (RfcxSource.VERBOSE) { Log.d(TAG, "onReceive()"); }
		setWifiIfAllowed(context);
	}

	private void setWifiIfAllowed(Context context) {
		RfcxSource app = (RfcxSource) context.getApplicationContext();
		if (!app.airplaneMode.isEnabled(context)) {
			if (wifiManager == null) {
				wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			}
			wifiManager.setWifiEnabled(app.airplaneMode.getAllowWifi());
		}
	}
	
}
