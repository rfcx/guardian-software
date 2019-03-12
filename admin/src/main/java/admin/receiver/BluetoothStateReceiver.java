package admin.receiver;

import rfcx.utility.device.control.DeviceBluetooth;
import rfcx.utility.rfcx.RfcxLog;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import admin.RfcxGuardian;

public class BluetoothStateReceiver extends BroadcastReceiver {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BluetoothStateReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		final String intentAction = intent.getAction();

        if (intentAction.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        		
            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
           
            if (		(bluetoothState == BluetoothAdapter.STATE_OFF)
            		||	(bluetoothState == BluetoothAdapter.STATE_TURNING_OFF)
            		) {
            	
            		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
            		boolean prefsAdminEnableBluetooth = app.rfcxPrefs.getPrefAsBoolean("admin_enable_bluetooth");
            		
            		if (prefsAdminEnableBluetooth) {
            			DeviceBluetooth.setOn();
            		}
            	
            }
        }
		
	}

}
