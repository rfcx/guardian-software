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


	// Bluetooth Tethering controls

	public DeviceBluetooth(Context context) {
		this.context = context;
	}

	private Context context;
	Object instance = null;
	Method setTetheringOn = null;
	Method isTetheringOn = null;
	Object mutex = new Object();

	public void enableTethering() {

		try {

			String sClassName = "android.bluetooth.BluetoothPan";
			Class<?> classBluetoothPan = Class.forName(sClassName);

			Constructor<?> ctor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
			ctor.setAccessible(true);
			//  Set Tethering ON
			Class[] paramSet = new Class[1];
			paramSet[0] = boolean.class;

			synchronized (mutex) {
				setTetheringOn = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", paramSet);
				isTetheringOn = classBluetoothPan.getDeclaredMethod("isTetheringOn", null);
				instance = ctor.newInstance(context, new BluetoothTetherServiceListener(context));
			}
		} catch (ClassNotFoundException e) {
			RfcxLog.logExc(logTag, e);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

	}

	public class BluetoothTetherServiceListener implements BluetoothProfile.ServiceListener {

		private final Context context;

		public BluetoothTetherServiceListener(final Context context) {
			this.context = context;
		}

		@Override
		public void onServiceConnected(final int profile, final BluetoothProfile proxy) {

			try {
				synchronized (mutex) {
					if (!(Boolean)isTetheringOn.invoke(instance, null)) {
						Log.v(logTag, "Bluetooth Tethering is disabled");
						setTetheringOn.invoke(instance, true);
						if ((Boolean)isTetheringOn.invoke(instance, null)) {
							Log.v(logTag, "Activating Bluetooth Tethering");
						} else {
							Log.e(logTag, "Bluetooth Tethering failed to activate");
						}
					} else {
						Log.v(logTag, "Bluetooth Tethering is enabled");
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
