package org.rfcx.guardian.utility.device.control;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

public class DeviceBluetooth {

	public DeviceBluetooth(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceBluetooth.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", DeviceBluetooth.class);
	
	public static boolean isEnabled() {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter != null) {
			int bluetoothState = bluetoothAdapter.getState();
			switch (bluetoothState) {
	            case BluetoothAdapter.STATE_OFF:
	            		return false;
	            case BluetoothAdapter.STATE_TURNING_OFF:
            			return false;
	            case BluetoothAdapter.STATE_ON:
            			return true;
	            case BluetoothAdapter.STATE_TURNING_ON:
        				return true;
			}
		}
		return false;
	}
	
//	public static boolean isConnected() {
//		// this would be a useful method to have
//		return false;
//	}
	
	public void setOn() {
	    	if (!isEnabled()) {
	    		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    		if (bluetoothAdapter != null) {
	    			Log.v(logTag, "Activating Bluetooth");
		    		BluetoothAdapter.getDefaultAdapter().enable();
	    		} else {
	    			Log.v(logTag, "Bluetooth hardware not present (cannot activate).");
	    		}
	    	}
	}
	
	public void setOff(Context context) {
	    	if (isEnabled()) {
    			Log.v(logTag, "Deactivating Bluetooth");
	    		BluetoothAdapter.getDefaultAdapter().disable();
	    	}
	}
	
}
