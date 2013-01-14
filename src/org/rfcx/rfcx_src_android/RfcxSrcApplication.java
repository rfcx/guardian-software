package org.rfcx.rfcx_src_android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class RfcxSrcApplication extends Application implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = RfcxSrcApplication.class.getSimpleName();
	private SharedPreferences prefs;
	
	Handler hndlr;
	Context context;

	ArduinoDbHelper arduinoDbHelper = new ArduinoDbHelper(this);

	
	final int MESSAGE_RECEPTION = 1;
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private StringBuilder sb = new StringBuilder();
	private BtConnectedThread mConnectedThread;
	
	private String arduino_bt_mac_addr;
	private UUID phone_uuid;
	
	
	public void sendBtCommand(String cmd) {
		mConnectedThread.write(cmd);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreated()");
		
		checkSetPreferences();
		
		hndlr = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case MESSAGE_RECEPTION:
					byte[] readBuf = (byte[]) msg.obj;
					sb.append(new String(readBuf, 0, msg.arg1));
	            	processArduinoResult();
	            	break;
	    		}
	        };
		};
		
	    btAdapter = BluetoothAdapter.getDefaultAdapter();
	    checkBTState();
	    
	    IntentFilter btIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
	    this.registerReceiver(btStateReceiver, btIntentFilter);
	    
	    IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	    this.registerReceiver(batteryStateReceiver, batteryIntentFilter);
	    
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		this.unregisterReceiver(btStateReceiver);
		Log.d(TAG, "onTerminated()");
	}
	
	public void appResume() {
		checkSetPreferences();
		connectToArduino();
	}
	
	public void appPause() {
	    Log.d(TAG, "appPause()");
	}
	
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "onSharedPreferenceChanged()");
	}
	
	private void checkSetPreferences() {
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefs.registerOnSharedPreferenceChangeListener(this);
		
		DeviceUuidFactory uuidFactory = new DeviceUuidFactory(context, this.prefs);
		phone_uuid = uuidFactory.getDeviceUuid();
		
		arduino_bt_mac_addr = this.prefs.getString("arduino_bt_mac_addr", "00:00:00:00:00:00");
		
		Log.d(TAG, phone_uuid + " - "+ arduino_bt_mac_addr);
		
		if (this.prefs.getString("arduino_bt_mac_addr", null) == null) {
			Log.e(TAG, "No preference value set for 'arduino_bt_mac_addr'");
		}
		
	}
	
	private void connectToArduino() {
		BluetoothDevice device = btAdapter.getRemoteDevice(arduino_bt_mac_addr);
		try {
			btSocket = device.createRfcommSocketToServiceRecord(phone_uuid);
		} catch (IOException e) {
			Log.d(TAG, "connectToArduino() failed to create socket " + e.getMessage());
		}
		btAdapter.cancelDiscovery();
		try {
			btSocket.connect();
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				Log.d(TAG, "connectToArduino() failed to close socket " + e2.getMessage());
			}
		}
		mConnectedThread = new BtConnectedThread(btSocket);
		mConnectedThread.start();
	}
	
	private void checkBTState() {
		if (btAdapter==null) {
			Log.e(TAG, "Bluetooth not supported");
		} else {
			if (btAdapter.isEnabled()) {
				Log.d(TAG, "bluetooth enabled");
			} else {
				Log.e(TAG, "bluetooth not enabled... enabling now...");
				btAdapter.enable();
			}
		}
	}
	
	private void toggleBtPower() {
		if (btAdapter.isEnabled()) {
			btAdapter.disable();
		}
	}
	
	private class BtConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	    
	    public BtConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) {	        	
	        	Log.d(TAG, e.toString());
	        }
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[256];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	        while (true) {
	        	try {
	                bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    hndlr.obtainMessage(MESSAGE_RECEPTION, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    public void write(String message) {
	    	byte[] msgBuffer = message.getBytes();
	    	try {
	            mmOutStream.write(msgBuffer);
	        } catch (IOException e) {
	            String err = e.getMessage();
	        	Log.d(TAG, "Error Sending BT Command: " + err);
	        	toggleBtPower();
	        	ContentValues values = new ContentValues();
	        	values.clear();
	        	values.put(ArduinoDbHelper.C_TYPE, "bt_");
	        	values.put(ArduinoDbHelper.C_MEASUREMENT, 0 );
	        	arduinoDbHelper.insertOrIgnore(values);
	          }
	    }
	 
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) {
	        	
	        }
	    }
	}
	
	
	
	
	public void processArduinoResult() {
		if (sb.toString().contains("*")) {
    		String rtrn_init = sb.substring(0, sb.indexOf("*"));
    		if ((rtrn_init.indexOf("_") >= 0) && rtrn_init.contains("^") && rtrn_init.contains("/")) {
    			saveArduinoResult(rtrn_init);
    		} else if (rtrn_init.contains("_") && rtrn_init.contains("^")) {
    			String cmd = rtrn_init.substring(1+sb.indexOf("^"));
    			mConnectedThread.write(cmd);
    		} else {
//    			Log.d(TAG, "Skipping: "+rtrn_init);
    		}
    		sb.delete(0, sb.length());
    	}
	}
	
	private void saveArduinoResult(String rtrn_init) {
		String command = rtrn_init.substring(1+sb.indexOf("^"));
		String results = rtrn_init.substring(1,sb.indexOf("^"));
		ContentValues values = new ContentValues();
		Log.d(TAG, "bt results: "+results);
		if (command.contains("a")) {
			// battery charging
			if (Integer.parseInt(results.substring(0,results.indexOf("/"))) == 1) {
				values.clear();
				values.put(ArduinoDbHelper.C_TYPE, "b_c");
				values.put(ArduinoDbHelper.C_MEASUREMENT, 1 );
				arduinoDbHelper.insertOrIgnore(values);
			}
			// battery fully charged
			if (Integer.parseInt(results.substring(1+results.indexOf("/"))) == 1) {
				values.clear();
				values.put(ArduinoDbHelper.C_TYPE, "b_f");
				values.put(ArduinoDbHelper.C_MEASUREMENT, 1 );
				arduinoDbHelper.insertOrIgnore(values);
			}
			
		} else if (command.contains("b")) {
			// temperature
			values.clear();
			values.put(ArduinoDbHelper.C_TYPE, "tmp");
			values.put(ArduinoDbHelper.C_MEASUREMENT, (int) Math.round(Double.parseDouble(results.substring(0,results.indexOf("/")))) );
			arduinoDbHelper.insertOrIgnore(values);
			// humidity
			values.clear();
			values.put(ArduinoDbHelper.C_TYPE, "hmd");
			values.put(ArduinoDbHelper.C_MEASUREMENT, (int) Math.round(Double.parseDouble(results.substring(1+results.indexOf("/")))) );
			arduinoDbHelper.insertOrIgnore(values);
		}
		
	}
	
	
	
	private final BroadcastReceiver btStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
	        final String action = intent.getAction();
	        if (btAdapter != null) {
	        	// this 'if' statement might be redundant based on the IntentFilter above...
	        	if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
	        		final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
	        		
	        		switch (state) {
	        			case BluetoothAdapter.STATE_OFF:
	        				Log.d(TAG,"bluetooth found to be in a disabled state... turning it back on...");
	        				btAdapter.enable();
	        				break;
	        			case BluetoothAdapter.STATE_ON:
	        				Log.d(TAG,"bluetooth is now on... connecting to arduino...");
	        				connectToArduino();
	        				break;
	        		}
	        	}
	        }
		}
	};
	
	private final BroadcastReceiver batteryStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
	        int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	        int batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	        float batteryPct = batteryLevel / (float) batteryScale;	
	        Log.d(TAG,"battery pct: "+batteryPct);
		}
	};
	
	
}
