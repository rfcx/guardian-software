package org.rfcx.src_state;

import java.io.IOException;
import java.util.UUID;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.bluetooth.*;
import android.util.Log;

public class ArduinoState {

	private static final String TAG = ArduinoState.class.getSimpleName();
	
	private static final boolean ARDUINO_ENABLED = false;
	
	private boolean batteryCharging;
	private boolean batteryCharged;
	
	private int humidity;
	private int temperature;

	private BluetoothAdapter bluetoothAdapter = null;
	private BluetoothSocket bluetoothSocket = null;
	private String bluetoothMAC = null;
	private BluetoothDevice bluetoothDevice;
	private UUID deviceUUID;
	
	public void setBluetoothMAC(String bluetoothMAC) {
		this.bluetoothMAC = bluetoothMAC;
	}
	
	public String getBluetoothMAC() {
		return bluetoothMAC;
	}
	
	public void setDeviceUUID(UUID deviceUUID) {
		this.deviceUUID = deviceUUID;
	}
	
	public UUID getDeviceUUID() {
		return deviceUUID;
	}
	
	public void setBluetoothDevice() {
		this.bluetoothDevice = this.getBluetoothAdapter().getRemoteDevice(this.bluetoothMAC);
	}
	
	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}
	
	public void setBatteryCharged(boolean batteryCharged) {
		this.batteryCharged = batteryCharged;
	}
	
	public boolean getBatteryCharged() {
		return batteryCharged;
	}
	
	public void setBatteryCharging(boolean batteryCharging) {
		this.batteryCharging = batteryCharging;
	}
	
	public boolean getBatteryCharging() {
		return batteryCharging;
	}
	
	public void setHumidity(int humidity) {
		this.humidity = humidity;
	}
	
	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}
	
	public int getHumidity() {
		return humidity;
	}
	
	public int getTemperature() {
		return temperature;
	}
	
	public void setBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
		this.bluetoothAdapter = bluetoothAdapter;
	}
	
	public BluetoothAdapter getBluetoothAdapter() {
		return bluetoothAdapter;
	}
	
	public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
		this.bluetoothSocket = bluetoothSocket;
	}
	
	public BluetoothSocket getBluetoothSocket() {
		return bluetoothSocket;
	}
	
	public void checkState() {
		if (bluetoothAdapter == null) {
			Log.e(TAG, "Bluetooth not supported");
		} else {
			if (bluetoothAdapter.isEnabled()) {
				if (RfcxSource.verboseLog()) { Log.d(TAG, "Bluetooth enabled"); }
			} else {
				if (RfcxSource.verboseLog()) { Log.d(TAG, "Bluetooth disabled: re-enabling"); }
				bluetoothAdapter.enable();
			}
		}
	}
	
	public void preConnect() {
		setBluetoothDevice();
		try {
			setBluetoothSocket(bluetoothDevice.createRfcommSocketToServiceRecord(getDeviceUUID()));
		} catch (IOException e) {
			if (RfcxSource.verboseLog()) { Log.d(TAG, "connectToArduino() failed to create socket " + e.getMessage()); }
		}
		getBluetoothAdapter().cancelDiscovery();
		try {
			getBluetoothSocket().connect();
		} catch (IOException e) {
			try {
				getBluetoothSocket().close();
			} catch (IOException e2) {
				if (RfcxSource.verboseLog()) { Log.d(TAG, "connectToArduino() failed to close socket " + e2.getMessage()); }
			}
		}
	}
	
	public static boolean isArduinoEnabled() {
		return ARDUINO_ENABLED;
	}
	
}
