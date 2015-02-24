package org.rfcx.guardian.receiver;

import java.util.Calendar;

import org.rfcx.guardian.RfcxGuardian;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

public class AirplaneModeReceiver extends BroadcastReceiver {

	private static final String TAG = "RfcxGuardian-"+AirplaneModeReceiver.class.getSimpleName();
	
	private RfcxGuardian app = null;
	private BluetoothAdapter bluetoothAdapter = null;
	private LocationManager locationManager = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
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
		
		// finally... location...
		
		// disable location services
		// TO DO
		
	}

}
