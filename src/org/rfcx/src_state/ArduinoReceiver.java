package org.rfcx.src_state;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ArduinoReceiver extends BroadcastReceiver {

	private static final String TAG = ArduinoReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        RfcxSource app = (RfcxSource) context.getApplicationContext();
        if ((app.arduinoState.getBluetoothAdapter() != null) && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        	final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        	if (org.rfcx.src_state.ArduinoState.isArduinoEnabled()) {
	        	switch (state) {
	        		case BluetoothAdapter.STATE_OFF:
	        			if (RfcxSource.verboseLog()) { Log.d(TAG,"Bluetooth disabled. Re-enabling."); }
	        			app.arduinoState.getBluetoothAdapter().enable();
	        			break;
	        		case BluetoothAdapter.STATE_ON:
	        			if (RfcxSource.verboseLog()) { Log.d(TAG,"Bluetooth enabled. Connecting to Arduino."); }
	        			app.connectToArduino();
	        			break;
	        	}
        	}
        }

	}
	
}
