package org.rfcx.guardian.receiver;

import org.rfcx.guardian.RfcxGuardian;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStateReceiver extends BroadcastReceiver {

	private static final String TAG = "RfcxGuardian-"+BluetoothStateReceiver.class.getSimpleName();
	
	private RfcxGuardian app = null;
	private BluetoothAdapter bluetoothAdapter = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
		
		final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            
            if (app.airplaneMode.allowBluetooth(context)) {
	            switch (state) {
		            case BluetoothAdapter.STATE_OFF:
		            	bluetoothAdapter.enable();
		                break;
		            case BluetoothAdapter.STATE_TURNING_OFF:
		            	bluetoothAdapter.enable();
		                break;
	            }
            } else {
	            switch (state) {
		            case BluetoothAdapter.STATE_ON:
		            	bluetoothAdapter.disable();
		                break;
		            case BluetoothAdapter.STATE_TURNING_ON:
		            	bluetoothAdapter.disable();
		                break;
	            }
            }
        }
		
	}

}
