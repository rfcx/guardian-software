package org.rfcx.rfcx_src_android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import org.rfcx.src_database.*;
import org.rfcx.src_state.*;
import org.rfcx.src_util.*;

public class RfcxSource extends Application implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = RfcxSource.class.getSimpleName();
	private SharedPreferences sharedPreferences;
	
	Context context;
	ArduinoDb arduinoDb = new ArduinoDb(this);

	// for reading battery charge state
	public BatteryState batteryState = new BatteryState();
	private final BroadcastReceiver batteryStateReceiver = new BatteryReceiver();
	
	// for viewing and controlling arduino microcontroller via bluetooth
	public ArduinoState arduinoState = new ArduinoState();
	private final BroadcastReceiver arduinoStateReceiver = new ArduinoReceiver();
	final int arduinoMessageReception = 1;
	private StringBuilder arduinoMessage = new StringBuilder();
	private ArduinoConnectThread arduinoConnectThread;
	Handler arduinoHandler;
	
	// for viewing and controlling airplane mode
	public AirplaneMode airplaneMode = new AirplaneMode();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	
	// for viewing cpu usage
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate()");
		
		checkSetPreferences();
		setupArduinoHandler();
	    
	    this.registerReceiver(arduinoStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	    this.registerReceiver(batteryStateReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		Log.d(TAG, "onTerminate()");
		
		this.unregisterReceiver(arduinoStateReceiver);
		this.unregisterReceiver(batteryStateReceiver);
		this.unregisterReceiver(airplaneModeReceiver);
	}
	
	public void appResume() {
		Log.d(TAG, "appResume()");
		checkSetPreferences();
		connectToArduino();
	}
	
	public void appPause() {
	    Log.d(TAG, "appPause()");
	}
	
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "onSharedPreferenceChanged()");
		checkSetPreferences();
	}
	
	private void checkSetPreferences() {
		Log.d(TAG, "checkSetPreferences()");
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		
		FactoryDeviceUuid uuidFactory = new FactoryDeviceUuid(context, this.sharedPreferences);
		arduinoState.setDeviceUUID(uuidFactory.getDeviceUuid());
		
		arduinoState.setBluetoothMAC(this.sharedPreferences.getString("arduino_bt_mac_addr", "00:00:00:00:00:00"));
		if (this.sharedPreferences.getString("arduino_bt_mac_addr", null) == null) {
			Log.e(TAG, "You must set preference value for 'arduino_bt_mac_addr'");
		}
		
		airplaneMode.setAllowWifi(this.sharedPreferences.getBoolean("allow_wifi", false));
	}
	
	public void connectToArduino() {
		Log.d(TAG, "connectToArduino()");
		arduinoState.preConnect();
		arduinoConnectThread = new ArduinoConnectThread(arduinoState.getBluetoothSocket());
		arduinoConnectThread.start();
	}
	
	public void setupArduinoHandler() {
		arduinoHandler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case arduinoMessageReception:
					byte[] readBuf = (byte[]) msg.obj;
					arduinoMessage.append(new String(readBuf, 0, msg.arg1));
					processArduinoResult(arduinoMessage);
	            	break;
	    		}
	        };
		};
		arduinoState.setBluetoothAdapter(BluetoothAdapter.getDefaultAdapter());
		arduinoState.checkState();
	}
	
	public void sendArduinoCommand(String cmd) {
		arduinoConnectThread.write(cmd);
	}
	
	private class ArduinoConnectThread extends Thread {
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	    
	    public ArduinoConnectThread(BluetoothSocket socket) {
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
	        byte[] buffer = new byte[256];
	        int bytes;
	        while (true) {
	        	try {
	                bytes = mmInStream.read(buffer);
                    arduinoHandler.obtainMessage(arduinoMessageReception, bytes, -1, buffer).sendToTarget();
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
	        	Log.d(TAG, "Error Sending BT Command: " + e.getMessage());
	        	if (arduinoState.getBluetoothAdapter().isEnabled()) {
	        		arduinoState.getBluetoothAdapter().disable();
	    		}
	        }
	    }
	}
	
	public void processArduinoResult(StringBuilder arduinoMessage) {
		if (arduinoMessage.toString().contains("*")) {
    		String rtrn_init = arduinoMessage.substring(0, arduinoMessage.indexOf("*"));
    		if ((rtrn_init.indexOf("_") >= 0) && rtrn_init.contains("^") && rtrn_init.contains("/")) {
    			saveArduinoResult(rtrn_init);
    		} else if (rtrn_init.contains("_") && rtrn_init.contains("^")) {
    			String cmd = rtrn_init.substring(1+arduinoMessage.indexOf("^"));
    			arduinoConnectThread.write(cmd);
    		} else {
//    			Log.d(TAG, "Skipping: "+rtrn_init);
    		}
    		arduinoMessage.delete(0, arduinoMessage.length());
    	}
	}

	
	private void saveArduinoResult(String rtrn_init) {
		String command = rtrn_init.substring(1+arduinoMessage.indexOf("^"));
		String results = rtrn_init.substring(1,arduinoMessage.indexOf("^"));
//		ContentValues values = new ContentValues();
		Log.d(TAG, "bt results: "+results);
		if (command.contains("a")) {
//			// battery charging
//			if (Integer.parseInt(results.substring(0,results.indexOf("/"))) == 1) {
//				values.clear();
//				values.put("type", "b_c");
//				values.put("measurement", 1 );
//				arduinoDbHelper.insertOrIgnore(values);
//			}
//			// battery fully charged
//			if (Integer.parseInt(results.substring(1+results.indexOf("/"))) == 1) {
//				values.clear();
//				values.put("type", "b_f");
//				values.put("measurement", 1 );
//				arduinoDbHelper.insertOrIgnore(values);
//			}
			
		} else if (command.contains("b")) {
			
			arduinoDb.dbTemperature.insert((int) Math.round(Double.parseDouble(results.substring(0,results.indexOf("/")))));
			arduinoDb.dbHumidity.insert((int) Math.round(Double.parseDouble(results.substring(1+results.indexOf("/")))));
//			// temperature
//			values.clear();
//			values.put("type", "tmp");
//			values.put("measurement", (int) Math.round(Double.parseDouble(results.substring(0,results.indexOf("/")))) );
//			arduinoDbHelper.insertOrIgnore(values);
//			// humidity
//			values.clear();
//			values.put("type", "hmd");
//			values.put("measurement", (int) Math.round(Double.parseDouble(results.substring(1+results.indexOf("/")))) );
//			arduinoDbHelper.insertOrIgnore(values);
		}
	}
	
		
}
