package org.rfcx.src_device;

import org.rfcx.src_android.RfcxSource;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AirplaneModeReceiver extends BroadcastReceiver {

	private static final String TAG = AirplaneModeReceiver.class.getSimpleName();
	
	private RfcxSource rfcxSource = null;
	private WifiManager wifiManager = null;
	private BluetoothAdapter bluetoothAdapter = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (RfcxSource.VERBOSE) Log.d(TAG, "BroadcastReceiver: "+TAG+" - Enabled");
		
		if (rfcxSource == null) rfcxSource = (RfcxSource) context.getApplicationContext();
		if (wifiManager == null) wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (!rfcxSource.airplaneMode.isEnabled(context)) {
			wifiManager.setWifiEnabled(rfcxSource.airplaneMode.getAllowWifi());
			if (bluetoothAdapter.isEnabled()) bluetoothAdapter.disable();
		}
	}
	
}
