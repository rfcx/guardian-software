package rfcx.utility.device.control;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceBluetooth {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceBluetooth.class);
	
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
	
	public static void setOn() {
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
	
	public static void setOff(Context context) {
	    	if (isEnabled()) {
    			Log.v(logTag, "Deactivating Bluetooth");
	    		BluetoothAdapter.getDefaultAdapter().disable();
	    	}
	}
	
}
