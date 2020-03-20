package org.rfcx.guardian.admin.receiver;

import org.rfcx.guardian.utility.device.control.DeviceBluetooth;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.rfcx.guardian.admin.RfcxGuardian;

public class BluetoothStateReceiver extends BroadcastReceiver {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BluetoothStateReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		final String intentAction = intent.getAction();

        if (intentAction.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        		
            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
			boolean prefsAdminEnableBluetooth = app.rfcxPrefs.getPrefAsBoolean("admin_enable_bluetooth");

            if (		(prefsAdminEnableBluetooth
						&&	(/*	(bluetoothState == BluetoothAdapter.STATE_OFF)
							||*/	(bluetoothState == BluetoothAdapter.STATE_TURNING_OFF)
						))
				||		(!prefsAdminEnableBluetooth
						&&	(/*	(bluetoothState == BluetoothAdapter.STATE_ON)
							||*/	(bluetoothState == BluetoothAdapter.STATE_TURNING_ON)
						))
				) {
					app.rfcxServiceHandler.triggerService("BluetoothStateSet", false);
            }
        }
		
	}

}
