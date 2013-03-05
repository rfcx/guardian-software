package org.rfcx.src_arduino;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver {

	private static final String TAG = BluetoothReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
        RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
        if ((rfcxSource.arduinoState.getBluetoothAdapter() != null) && intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        	final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        	if (org.rfcx.src_arduino.ArduinoState.isArduinoEnabled()) {
	        	switch (state) {
	        		case BluetoothAdapter.STATE_OFF:
	        			if (RfcxSource.verboseLog()) { Log.d(TAG,"Bluetooth disabled. Re-enabling."); }
	        			rfcxSource.arduinoState.getBluetoothAdapter().enable();
	        			break;
	        		case BluetoothAdapter.STATE_ON:
	        			if (RfcxSource.verboseLog()) { Log.d(TAG,"Bluetooth enabled. Connecting to Arduino."); }
	        			rfcxSource.connectToArduino();
	        			break;
	        	}
        	} else if (state == BluetoothAdapter.STATE_ON) {
        		rfcxSource.arduinoState.getBluetoothAdapter().disable();
        	}
        }

	}
	
}