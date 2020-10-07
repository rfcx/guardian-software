package org.rfcx.guardian.utility.device.control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DeviceBluetooth {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceBluetooth");

	private Context context;
	private BluetoothAdapter bluetoothAdapter;

	public DeviceBluetooth(Context context) {
		this.context = context;
		this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	public boolean isBluetoothEnabled() {
//		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

//
//	public void setPowerOn() {
//		if (!isBluetoothEnabled()) {
////			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//			if (bluetoothAdapter != null) {
//				Log.v(logTag, "Activating Bluetooth Power");
//				bluetoothAdapter.enable();
//			} else {
//				Log.v(logTag, "Bluetooth hardware not present (cannot activate).");
//			}
//		}
//	}
//
//	public void setPowerOff() {
//		if (isBluetoothEnabled()) {
//			Log.v(logTag, "Deactivating Bluetooth Power");
//			bluetoothAdapter.disable();
//		}
//	}
//
//	// Network Name controls
//
//	public void setNetworkName(String networkName) {
//		if (isBluetoothEnabled()) {
////			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//			if (bluetoothAdapter != null) {
//				bluetoothAdapter.setName(networkName);
//				Log.v(logTag, "Bluetooth Network Name: '"+bluetoothAdapter.getName()+"'");
//			}
//		}
//	}
//
//	// Tethering controls
//
//	Object tetherInstance = null;
//	Method isTetheringOn = null;
//	Method setTetheringOn = null;
//	Method setTetheringOff = null;
//	Object tetherMutex = new Object();
//	boolean tetherEnableOrDisable;
//
//	public void setTetheringOn() {
//		this.tetherEnableOrDisable = true;
//		setTethering();
//	}
//
//	public void setTetheringOff() {
//		this.tetherEnableOrDisable = false;
//		setPowerOff(); // this is kind of cheating...
////		setTethering();
//	}
//
//	private void setTethering() {
//
//		try {
//
//			String sClassName = "android.bluetooth.BluetoothPan";
//			Class<?> classBluetoothPan = Class.forName(sClassName);
//
//			Constructor<?> tetherConstructor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
//			tetherConstructor.setAccessible(true);
//
//			Class[] enableTetheringParamSet = new Class[1];
//			enableTetheringParamSet[0] = boolean.class;
//
//			// THIS IS PROBABLY NOT RIGHT —— NEED TO SET REAL PARAMS FOR DISABLING TETHERING
//			Class[] disableTetheringParamSet = new Class[1];
//			disableTetheringParamSet[0] = boolean.class;
//			// THIS IS PROBABLY NOT RIGHT —— NEED TO SET REAL PARAMS FOR DISABLING TETHERING
//
//			synchronized (tetherMutex) {
//				isTetheringOn = classBluetoothPan.getDeclaredMethod("isTetheringOn", null);
//
//				if (this.tetherEnableOrDisable) {
//					setTetheringOn = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", enableTetheringParamSet);
//				} else {
//					// THIS IS PROBABLY NOT RIGHT —— NEED TO KNOW PARAMS FOR DISABLING TETHERING
//					setTetheringOff = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", disableTetheringParamSet);
//					// THIS IS PROBABLY NOT RIGHT —— NEED TO KNOW PARAMS FOR DISABLING TETHERING
//				}
//
//				tetherInstance = tetherConstructor.newInstance(context, new BluetoothTetherServiceListener(context, this.tetherEnableOrDisable));
//			}
//		} catch (ClassNotFoundException e) {
//			RfcxLog.logExc(logTag, e);
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}
//	}
//
//	public class BluetoothTetherServiceListener implements BluetoothProfile.ServiceListener {
//
//		private final Context context;
//		private final boolean tetherEnableOrDisable;
//
//		public BluetoothTetherServiceListener(final Context context, final boolean enableOrDisable) {
//			this.context = context;
//			this.tetherEnableOrDisable = enableOrDisable;
//		}
//
//		@Override
//		public void onServiceConnected(final int profile, final BluetoothProfile proxy) {
//
//			try {
//				synchronized (tetherMutex) {
//					if (!(Boolean)isTetheringOn.invoke(tetherInstance, null)) {
//						Log.v(logTag, "Bluetooth Tethering is disabled");
//						if (this.tetherEnableOrDisable) {
//							Log.v(logTag, "Attempting to activate Bluetooth Tethering");
//
//							setTetheringOn.invoke(tetherInstance, true);
//							if ((Boolean)isTetheringOn.invoke(tetherInstance, null)) {
//								Log.v(logTag, "Bluetooth Tethering has been activated");
//							} else {
//								Log.e(logTag, "Failed to activate Bluetooth Tethering");
//							}
//						}
//					} else if ((Boolean)isTetheringOn.invoke(tetherInstance, null)) {
//						Log.v(logTag, "Bluetooth Tethering is enabled");
//						if (!this.tetherEnableOrDisable) {
//							Log.v(logTag, "Attempting to de-activate Bluetooth Tethering");
//							setTetheringOff.invoke(tetherInstance, true);
//							if (!(Boolean)isTetheringOn.invoke(tetherInstance, null)) {
//								Log.v(logTag, "Bluetooth Tethering has been de-activated");
//							} else {
//								Log.e(logTag, "Failed to de-activate Bluetooth Tethering");
//							}
//						}
//					}
//				}
//			} catch (InvocationTargetException e) {
//				RfcxLog.logExc(logTag, e);
//			} catch (IllegalAccessException e) {
//				RfcxLog.logExc(logTag, e);
//			}
//		}
//
//		@Override
//		public void onServiceDisconnected(final int profile) {
//		}
//
//
//	}

}
