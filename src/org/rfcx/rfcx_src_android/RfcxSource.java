package org.rfcx.rfcx_src_android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.rfcx.src_api.*;
import org.rfcx.src_arduino.*;
import org.rfcx.src_audio.*;
import org.rfcx.src_database.*;
import org.rfcx.src_device.*;
import org.rfcx.src_util.*;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class RfcxSource extends Application implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = RfcxSource.class.getSimpleName();
	private static final boolean LOG_VERBOSE = true;
	private SharedPreferences sharedPreferences;
	Context context;
	
	// device characteristics
	private UUID deviceId = null;
	
	// database access helpers
	public ArduinoDb arduinoDb = new ArduinoDb(this);
	public DeviceStateDb deviceStateDb = new DeviceStateDb(this);
	public AudioDb audioDb = new AudioDb(this);

	// for reading battery charge state
	public DeviceState deviceState = new DeviceState();
	private final BroadcastReceiver batteryDeviceStateReceiver = new BatteryReceiver();
	
	
	// for viewing and controlling arduino microcontroller via bluetooth
	public ArduinoState arduinoState = new ArduinoState();
	private final BroadcastReceiver arduinoStateReceiver = new BluetoothReceiver();
	final int arduinoMessageReception = 1;
	private StringBuilder arduinoMessage = new StringBuilder();
	private ArduinoConnectThread arduinoConnectThread;
	Handler arduinoHandler;
	
	// for viewing and controlling airplane mode
	public AirplaneMode airplaneMode = new AirplaneMode();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	
	// for monitoring cpu usage
	public DeviceCpuUsage deviceCpuUsage = new DeviceCpuUsage();

	// for transmitting api data
	public ApiComm apiComm = new ApiComm();
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	// for analyzing captured audio
	public AudioState audioState = new AudioState();
	
	// android service running flags
	public boolean isServiceRunning_DeviceState = false;
	public boolean isServiceRunning_ArduinoState = false;
	public boolean isServiceRunning_ApiComm = false;
	public boolean isServiceRunning_AudioCapture = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onCreate()"); }
		
		checkSetPreferences();

		setupArduinoHandler();

		this.registerReceiver(arduinoStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	    this.registerReceiver(batteryDeviceStateReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	    this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onTerminate()"); }

		this.unregisterReceiver(arduinoStateReceiver);
		this.unregisterReceiver(batteryDeviceStateReceiver);
		this.unregisterReceiver(airplaneModeReceiver);
		this.unregisterReceiver(connectivityReceiver);
	}
	
	public void appResume() {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "appResume()"); }
		checkSetPreferences();
		connectToArduino();
	}
	
	public void appPause() {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "appPause()"); }
	}
	
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onSharedPreferenceChanged()"); }
		checkSetPreferences();
	}
	
	private void checkSetPreferences() {
		Log.d(TAG, "checkSetPreferences()");
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		
		arduinoState.setDeviceUUID(getDeviceId());
		arduinoState.setBluetoothMAC(this.sharedPreferences.getString("arduino_bt_mac_addr", "00:00:00:00:00:00"));
		if (this.sharedPreferences.getString("arduino_bt_mac_addr", null) == null) {
			Log.e(TAG, "You must set preference value for 'arduino_bt_mac_addr'");
		}
		
		airplaneMode.setAllowWifi(this.sharedPreferences.getBoolean("allow_wifi", false));
		apiComm.setDomain(this.sharedPreferences.getString("api_domain", "api.rfcx.org"));
	}
	
	public UUID getDeviceId() {
		if (deviceId == null) {
			FactoryDeviceUuid uuidFactory = new FactoryDeviceUuid(context, this.sharedPreferences);
			deviceId = uuidFactory.getDeviceUuid();
		}
		return deviceId;
	}
	
	public void launchServices(Context context) {
		
		if (ArduinoState.isArduinoEnabled() && !isServiceRunning_ArduinoState) {
			context.startService(new Intent(context, ArduinoService.class));
		} else if (isServiceRunning_ArduinoState) {
			Log.d(TAG, "ArduinoStateService already running. Not re-started...");
		}
		if (AudioState.isAudioEnabled() && !isServiceRunning_AudioCapture) {
			context.startService(new Intent(context, AudioCaptureService.class));
		} else if (isServiceRunning_AudioCapture) {
			Log.d(TAG, "AudioCaptureService already running. Not re-started...");
		}
		if (DeviceStateService.isDeviceStateEnabled() && !isServiceRunning_DeviceState) {
			context.startService(new Intent(context, DeviceStateService.class));
		} else if (isServiceRunning_DeviceState) {
			Log.d(TAG, "DeviceStateService already running. Not re-started...");
		}
		if (ApiComm.isApiCommEnabled() && !isServiceRunning_ApiComm) {
			context.startService(new Intent(context, ApiCommService.class));
		} else if (isServiceRunning_ApiComm) {
			Log.d(TAG, "ApiCommService already running. Not re-started...");
		}
	}
	
	public void suspendExpensiveServices(Context context) {

		if (ArduinoState.isArduinoEnabled() && isServiceRunning_ArduinoState) {
			context.stopService(new Intent(context, ArduinoService.class));
		} else if (!isServiceRunning_ArduinoState) {
			Log.d(TAG, "ArduinoStateService not running. Not stopped...");
		}
		if (AudioState.isAudioEnabled() && isServiceRunning_AudioCapture) {
			context.stopService(new Intent(context, AudioCaptureService.class));
		} else if (!isServiceRunning_AudioCapture) {
			Log.d(TAG, "AudioCaptureService not running. Not stopped...");
		}
		if (DeviceStateService.isDeviceStateEnabled() && isServiceRunning_DeviceState) {
			context.stopService(new Intent(context, DeviceStateService.class));
		} else if (!isServiceRunning_DeviceState) {
			Log.d(TAG, "DeviceStateService not running. Not stopped...");
		}
		if (ApiComm.isApiCommEnabled() && isServiceRunning_ApiComm) {
			context.stopService(new Intent(context, ApiCommService.class));
		} else if (!isServiceRunning_ApiComm) {
			Log.d(TAG, "ApiCommService not running. Not stopped...");
		}
	}
	
	
	
	
	// Arduino stuff below here
	
	public void connectToArduino() {
		if (RfcxSource.verboseLog()) { Log.d(TAG, "connectToArduino()"); }
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
	        	if (RfcxSource.verboseLog()) { Log.d(TAG, e.toString()); }
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
	        	if (RfcxSource.verboseLog()) { Log.d(TAG, "Error Sending Bluetooth Command: " + e.getMessage()); }
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
		String cmd = rtrn_init.substring(1+arduinoMessage.indexOf("^"));
		String res = rtrn_init.substring(1,arduinoMessage.indexOf("^"));
		if (cmd.contains("a")) {
			int charging = Integer.parseInt(res.substring(0,res.indexOf("/")));
			int charged = Integer.parseInt(res.substring(1+res.indexOf("/")));
			arduinoDb.dbCharge.insert( (charged == 1) ? 2 : charging );
		} else if (cmd.contains("b")) {
			arduinoDb.dbTemperature.insert((int) Math.round(Double.parseDouble(res.substring(0,res.indexOf("/")))));
			arduinoDb.dbHumidity.insert((int) Math.round(Double.parseDouble(res.substring(1+res.indexOf("/")))));
		} else if (cmd.contains("s") || cmd.contains("t")) {
			int lastState = Integer.parseInt(res.substring(0,res.indexOf("/")));
			int currState = Integer.parseInt(res.substring(1+res.indexOf("/")));
			Log.d(TAG,"Power: "+currState);
		}
	}
	
	public void toggleDevicePower(boolean onOff) {
		if (onOff) {
			sendArduinoCommand("s");
		} else {
			sendArduinoCommand("t");
		}
	}
	
	public static boolean verboseLog() {
		return LOG_VERBOSE;
	}

		
}
