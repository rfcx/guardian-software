package org.rfcx.src_arduino;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;


public class PartOfAppClass_RfcxSource {
	
	// the code below should be include in the main application class RfcxSource...
	// It has been stored here in preparation for its archival and removal
	
	
	
	/*
		// for viewing and controlling arduino microcontroller via bluetooth
		public ArduinoState arduinoState = new ArduinoState();
		private final BroadcastReceiver arduinoStateReceiver = new BluetoothReceiver();
		final int arduinoMessageReception = 1;
		private StringBuilder arduinoMessage = new StringBuilder();
		private ArduinoConnectThread arduinoConnectThread;
		Handler arduinoHandler;
		
		public boolean isServiceRunning_ArduinoState = false;
	
	
	*/
	
	// part of onCreate()
	/*	setupArduinoHandler();
		this.registerReceiver(arduinoStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	*/
	
	// part of onTerminate()
	/*	this.unregisterReceiver(arduinoStateReceiver);
	*/
	
	// part of appResume()
	/* connectToArduino();
	*/
	
	// part of checkSetPreferences()
	/*	arduinoState.setDeviceUUID(getDeviceId());
		arduinoState.setBluetoothMAC(this.sharedPreferences.getString("arduino_bt_mac_addr", "00:00:00:00:00:00"));
		if (this.sharedPreferences.getString("arduino_bt_mac_addr", null) == null) {
			Log.e(TAG, "You must set preference value for 'arduino_bt_mac_addr'");
		}
	 */
	
	// part of launchServices
	/*
	 	if (ArduinoState.isArduinoEnabled() && !isServiceRunning_ArduinoState) {
			context.startService(new Intent(context, ArduinoService.class));
		} else if (isServiceRunning_ArduinoState) {
			Log.d(TAG, "ArduinoStateService already running. Not re-started...");
		}
	 */
	
	// part of suspendServices
	/*
	 	if (ArduinoState.isArduinoEnabled() && isServiceRunning_ArduinoState) {
			context.stopService(new Intent(context, ArduinoService.class));
		} else if (!isServiceRunning_ArduinoState) {
			Log.d(TAG, "ArduinoStateService not running. Not stopped...");
		}
	 */
	
	
	/*
	
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
	
	*/
	
}
