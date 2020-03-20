package org.rfcx.guardian.utility.device.control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DeviceBluetooth {

	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceBluetooth.class);

	private Context context;

	public DeviceBluetooth(Context context) {
		this.context = context;
	}

	public static boolean isBluetoothEnabled() {
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
	
	public static void setPowerOn() {
	    	if (!isBluetoothEnabled()) {
	    		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    		if (bluetoothAdapter != null) {
	    			Log.v(logTag, "Activating Bluetooth Power");
		    		BluetoothAdapter.getDefaultAdapter().enable();
	    		} else {
	    			Log.v(logTag, "Bluetooth hardware not present (cannot activate).");
	    		}
	    	}
	}
	
	public static void setPowerOff(Context context) {
	    	if (isBluetoothEnabled()) {
    			Log.v(logTag, "Deactivating Bluetooth Power");
	    		BluetoothAdapter.getDefaultAdapter().disable();
	    	}
	}

	// ADB Connection controls
	// add some code here

	// Network Name controls
	// add some code here

	// Tethering controls

	Object tetherInstance = null;
	Method isTetheringOn = null;
	Method setTetheringOn = null;
	Method setTetheringOff = null;
	Object tetherMutex = new Object();
	boolean tetherEnableOrDisable;

	public void setTetheringOn() {
		this.tetherEnableOrDisable = true;
		setTethering();
	}

	public void setTetheringOff() {
		this.tetherEnableOrDisable = false;
		setTethering();
	}

	private void setTethering() {

		try {

			String sClassName = "android.bluetooth.BluetoothPan";
			Class<?> classBluetoothPan = Class.forName(sClassName);

			Constructor<?> tetherConstructor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
			tetherConstructor.setAccessible(true);

			Class[] enableTetheringParamSet = new Class[1];
			enableTetheringParamSet[0] = boolean.class;

			// THIS IS PROBABLY NOT RIGHT —— NEED TO KNOW PARAMS FOR DISABLING TETHERING
			Class[] disableTetheringParamSet = new Class[1];
			disableTetheringParamSet[0] = boolean.class;

			synchronized (tetherMutex) {
				isTetheringOn = classBluetoothPan.getDeclaredMethod("isTetheringOn", null);
				setTetheringOn = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", enableTetheringParamSet);

				// THIS IS PROBABLY NOT RIGHT —— NEED TO KNOW PARAMS FOR DISABLING TETHERING
				setTetheringOff = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", disableTetheringParamSet);

				tetherInstance = tetherConstructor.newInstance(context, new BluetoothTetherServiceListener(context, this.tetherEnableOrDisable));
			}
		} catch (ClassNotFoundException e) {
			RfcxLog.logExc(logTag, e);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

	}

	public class BluetoothTetherServiceListener implements BluetoothProfile.ServiceListener {

		private final Context context;
		private final boolean tetherEnableOrDisable;

		public BluetoothTetherServiceListener(final Context context, final boolean enableOrDisable) {
			this.context = context;
			this.tetherEnableOrDisable = enableOrDisable;
		}

		@Override
		public void onServiceConnected(final int profile, final BluetoothProfile proxy) {

			try {
				synchronized (tetherMutex) {
					if (!(Boolean)isTetheringOn.invoke(tetherInstance, null)) {
						Log.v(logTag, "Bluetooth Tethering is disabled");
						if (this.tetherEnableOrDisable) {
							Log.v(logTag, "Attempting to activate Bluetooth Tethering");
							setTetheringOn.invoke(tetherInstance, true);
							if ((Boolean)isTetheringOn.invoke(tetherInstance, null)) {
								Log.v(logTag, "Bluetooth Tethering has been activated");
							} else {
								Log.e(logTag, "Failed to activate Bluetooth Tethering");
							}
						}
					} else if ((Boolean)isTetheringOn.invoke(tetherInstance, null)) {
						Log.v(logTag, "Bluetooth Tethering is enabled");
						if (!this.tetherEnableOrDisable) {
							Log.v(logTag, "Attempting to de-activate Bluetooth Tethering");
							// THIS IS PROBABLY NOT RIGHT —— NEED TO KNOW PARAMS FOR DISABLING TETHERING
							setTetheringOff.invoke(tetherInstance, true);
							if (!(Boolean)isTetheringOn.invoke(tetherInstance, null)) {
								Log.v(logTag, "Bluetooth Tethering has been de-activated");
							} else {
								Log.e(logTag, "Failed to de-activate Bluetooth Tethering");
							}
						}
					}
				}
			} catch (InvocationTargetException e) {
				RfcxLog.logExc(logTag, e);
			} catch (IllegalAccessException e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		@Override
		public void onServiceDisconnected(final int profile) {
		}


	}

}
