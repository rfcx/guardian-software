package org.rfcx.rfcx_src_android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class RfcxSrcApplication extends Application implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = RfcxSrcApplication.class.getSimpleName();
	private SharedPreferences prefs;
	
	Handler hndlr;
	
	final int MESSAGE_RECEPTION = 1;
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private StringBuilder sb = new StringBuilder();
	private BtConnectedThread mConnectedThread;

	private static final UUID PHONE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static String ARDUINO_BT_MAC_ADDR = "00:12:09:29:60:54";
	
	public float envTemperature = 0;
	public float envHumidity = 0;
	public boolean envBatteryCharging = false;
	public boolean phoneBatteryCharging = false;
	
	public void sendBtCommand(String cmd) {
		mConnectedThread.write(cmd);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefs.registerOnSharedPreferenceChangeListener(this);
		Log.i(TAG, "onCreated()");
		
		hndlr = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case MESSAGE_RECEPTION:
					byte[] readBuf = (byte[]) msg.obj;
					sb.append(new String(readBuf, 0, msg.arg1));

	            	if (sb.toString().contains("*")) {
	            		String rtrn_init = sb.substring(0, sb.indexOf("*"));
	            		if ((rtrn_init.indexOf("_") >= 0) && rtrn_init.contains("^") && rtrn_init.contains("/")) {
	            			String rtrn = rtrn_init.substring(1,sb.indexOf("^"));
	            			Log.d(TAG, "'"+ rtrn +"'");
	            		} else if (rtrn_init.contains("_") && rtrn_init.contains("^")) {
	            			String cmd = rtrn_init.substring(1+sb.indexOf("^"));
	            			mConnectedThread.write(cmd);
	            		} else {
//	            			Log.d(TAG, "Skipping: "+rtrn_init);
	            		}
	            		sb.delete(0, sb.length());
	            	}

	            	break;
	    		}
	        };
		};
		
	    btAdapter = BluetoothAdapter.getDefaultAdapter();
	    checkBTState();
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		Log.i(TAG, "onTerminated()");
	}
	
	public void appResume() {
		BluetoothDevice device = btAdapter.getRemoteDevice(ARDUINO_BT_MAC_ADDR);
		try {
			btSocket = device.createRfcommSocketToServiceRecord(PHONE_UUID);
		} catch (IOException e) {
			Log.d(TAG, "appResume() failed to create socket " + e.getMessage());
		}
		btAdapter.cancelDiscovery();
		try {
			btSocket.connect();
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				Log.d(TAG, "appResume() failed to close socket " + e2.getMessage());
			}
		}
		mConnectedThread = new BtConnectedThread(btSocket);
		mConnectedThread.start();
	}
	
	public void appPause() {
	    Log.d(TAG, "appPause()");
	    try {
	    	btSocket.close();
	    } catch (IOException e2) {
	    	Log.d(TAG, "appPause() failed to close socket." + e2.getMessage());
	    }
	}
	
	public synchronized void onSharedPreferenceChanged(
			SharedPreferences sharedPreferences, String key) {
	//	this.twitter = null;
	}
	
	private void checkBTState() {
		if (btAdapter==null) {
			Log.e(TAG, "Bluetooth not supported");
		} else {
			if (btAdapter.isEnabled()) {
				Log.i(TAG, "Bluetooth enabled");
			} else {
				Log.e(TAG, "Bluetooth not enabled");
//				Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
			}
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
//	    	Log.d(TAG, "BT sent: '" + message +"'");
	    	byte[] msgBuffer = message.getBytes();
	    	try {
	            mmOutStream.write(msgBuffer);
	        } catch (IOException e) {
	            Log.d(TAG, "Error Sending BT Command: " + e.getMessage());     
	          }
	    }
	 
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) {
	        	
	        }
	    }
	}
}
