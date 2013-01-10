package org.rfcx.rfcx_src_android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Home extends Activity {
	
	private static final String APP_NAME = "rfcx-src-android";

	Button bttnStart, bttnStop, bttnPowerOn, bttnPowerOff;
	Handler hndlr;

	private static final int REQUEST_ENABLE_BT = 1;
	final int MESSAGE_RECEPTION = 1;
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private StringBuilder sb = new StringBuilder();
	private ConnectedThread mConnectedThread;

	private static final UUID PHONE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static String ARDUINO_BT_MAC_ADDR = "00:12:09:29:60:54";
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		bttnStart = (Button) findViewById(R.id.bttnStart);
		bttnStop = (Button) findViewById(R.id.bttnStop);
		bttnPowerOn = (Button) findViewById(R.id.bttnPowerOn);
		bttnPowerOff = (Button) findViewById(R.id.bttnPowerOff);

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
	            			Log.d(APP_NAME, "'"+ rtrn +"'");
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

	    bttnStart.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		mConnectedThread.write("a");
	    	}
	    });

	    bttnStop.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		mConnectedThread.write("b");
	    	}
	    });
	    
	    bttnPowerOn.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		mConnectedThread.write("s");
	    	}
	    });
	    
	    bttnPowerOff.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		mConnectedThread.write("t");
	    	}
	    });
	}
	
	@Override
	public void onResume() {
		super.onResume();
		BluetoothDevice device = btAdapter.getRemoteDevice(ARDUINO_BT_MAC_ADDR);
		try {
			btSocket = device.createRfcommSocketToServiceRecord(PHONE_UUID);
		} catch (IOException e) {
			Log.d(APP_NAME, "onResume() and failed to create socket." + e.getMessage());
		}
		
		btAdapter.cancelDiscovery();
		try {
			btSocket.connect();
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				Log.d(APP_NAME, "onResume() and failed to close socket." + e2.getMessage());
			}
		}
		mConnectedThread = new ConnectedThread(btSocket);
		mConnectedThread.start();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	    Log.d(APP_NAME, "running onPause()");
	    try {
	    	btSocket.close();
	    } catch (IOException e2) {
	    	Log.d(APP_NAME, "onPause() and failed to close socket." + e2.getMessage());
	    }
	}
	
	private void checkBTState() {
		if (btAdapter==null) {
			Log.d(APP_NAME, "Bluetooth not supported.");
			finish();
		} else {
			if (btAdapter.isEnabled()) {
				Log.d(APP_NAME, "Bluetooth enabled.");
			} else {
				Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}
	
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) {
	        	
	        	Log.d(APP_NAME, e.toString());
	        }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[256];  // buffer store for the stream
	        int bytes; // bytes returned from read()

	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	        	try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    hndlr.obtainMessage(MESSAGE_RECEPTION, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(String message) {
//	    	Log.d(APP_NAME, "Sending BT Command: " + message);
	    	byte[] msgBuffer = message.getBytes();
	    	try {
	            mmOutStream.write(msgBuffer);
	        } catch (IOException e) {
	            Log.d(APP_NAME, "Error Sending BT Command: " + e.getMessage());     
	          }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
}
