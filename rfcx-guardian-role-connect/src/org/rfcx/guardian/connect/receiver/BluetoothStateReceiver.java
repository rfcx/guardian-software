package org.rfcx.guardian.connect.receiver;

import org.rfcx.guardian.connect.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxConstants;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStateReceiver extends BroadcastReceiver {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+BluetoothStateReceiver.class.getSimpleName();
	
	private RfcxGuardian app = null;
	private BluetoothAdapter bluetoothAdapter = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
		
		final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            
            switch (state) {
	            case BluetoothAdapter.STATE_OFF:
	            	bluetoothAdapter.enable();
	                break;
	            case BluetoothAdapter.STATE_TURNING_OFF:
	            	bluetoothAdapter.enable();
	                break;
            }
            
        }
		
	}

}
