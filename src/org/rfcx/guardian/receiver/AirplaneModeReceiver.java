package org.rfcx.guardian.receiver;

import java.util.Calendar;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.TimeOfDay;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AirplaneModeReceiver extends BroadcastReceiver {

	private static final String TAG = "RfcxGuardian-"+AirplaneModeReceiver.class.getSimpleName();
	
	private RfcxGuardian app = null;
	private WifiManager wifiManager = null;
	private BluetoothAdapter bluetoothAdapter = null;
	private LocationManager locationManager = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (wifiManager == null) wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
		if (locationManager == null) locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		/*
		 * THIS WOULD BE A GREAT THING TO ADD TO THE DB DIAGNOSTICS
		 *	app.apiCore.resetSignalSearchClock();
		 */
		
//		 TimeOfDay timeOfDay = new TimeOfDay();

		if (app.verboseLog) Log.d(TAG,
				"AirplaneMode " + ( app.airplaneMode.isEnabled(context) ? "Enabled" : "Disabled" )
				+ " at "+(Calendar.getInstance()).getTime().toLocaleString());
		
		if (!app.airplaneMode.isEnabled(context)) {
			
			// disable location services
			// TO DO
			
			// disable/enable wifi
			wifiManager.setWifiEnabled(app.airplaneMode.allowWifi(context));

			// enable/disable bluetooth
			if (app.airplaneMode.allowBluetooth(context)) { bluetoothAdapter.enable(); } else { bluetoothAdapter.disable(); }
			
			
			//			if (timeOfDay.isDataGenerationEnabled(context) || app.ignoreOffHours) {
//				app.apiCore.setSignalSearchStart(Calendar.getInstance());
//				wifiManager.setWifiEnabled(app.airplaneMode.getAllowWifi());	
//			} else {
//				if (app.verboseLogging) Log.d(TAG, "API Check-In not allowed right now");
//			}
		}
	}
}
