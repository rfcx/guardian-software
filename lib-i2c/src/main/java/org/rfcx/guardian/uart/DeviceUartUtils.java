package org.rfcx.guardian.uart;

import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.rfcx.guardian.uart.serialport.SerialPort;

public class DeviceUartUtils {

	public DeviceUartUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceUartUtils");
	}

	private final String logTag;

	private String uartHandlerFilepath;
	private boolean isHandlerReadable = false;

	private final Map<String, String> addrMap = new HashMap<String, String>();


	public void setUartHandlerFilepath(String handlerFilepath) {
		this.uartHandlerFilepath = handlerFilepath;
	}

	public void setupAddresses(Map<String, String[]> addrMap) {
		for (Map.Entry addr : addrMap.entrySet()) {
			String addrName = addr.getKey().toString();
			setAddrNumByName(addrName, addrMap.get(addrName)[0]);
		}
	}

	private boolean checkSetIsHandlerAccessible(int addrNum) {
		if (!this.isHandlerReadable) {
			File handlerFile = (new File(uartHandlerFilepath+addrNum));
			if (handlerFile.exists() && handlerFile.canRead() && handlerFile.canWrite()) {
				this.isHandlerReadable = true;
			} else {
				Log.e(logTag, "Could not access UART Handler: "+ uartHandlerFilepath+addrNum);
			}
		}
		return this.isHandlerReadable;
	}

	private void setAddrNumByName(String addrName, int addrNum) {
		this.addrMap.remove(addrName.toUpperCase(Locale.US));
		this.addrMap.put(addrName.toUpperCase(Locale.US), ""+addrNum);
	}

	private void setAddrNumByName(String addrName, String addrNum) {
		setAddrNumByName(addrName, Integer.parseInt(addrNum));
	}

	private int getAddrNumByName(String addrName) {
		int addrNum = 0;
		if (this.addrMap.containsKey(addrName.toUpperCase(Locale.US))) {
			addrNum = Integer.parseInt(this.addrMap.get(addrName.toUpperCase(Locale.US)));
		} else {
			Log.e(logTag, "No UART Interface assignment for '"+addrName+"'");
		}
		return addrNum;
	}

//	// uartInterface should be a low integer, including zero, as in /dev/ttyMT0 or /dev/ttyMT1
//	private final int uartInterface;
//
//	public void initializeOrReInitialize() {
//
//		Log.i(logTag, "Attempting to initialize UART interface '/dev/ttyMT"+this.uartInterface +"'");
//
//		if (isUartHandlerAccessible()) {
//
////			this.i2cTools = new I2cTools();
////			this.i2cTools.i2cDeInit(this.uartInterface);
////			this.uartAdapterReceipt = i2cTools.i2cInit(this.uartInterface);
//
//			if (isInitialized(true)) {
//				Log.i(logTag, "UART interface '/dev/ttyMT" + this.uartInterface + "' successfully initialized.");
//			}
//
//		} else {
//			Log.e(logTag, "UART handler '/dev/ttyMT"+this.uartInterface +"' is NOT accessible. Initialization failed.");
//		}
//	}
//
//	public boolean isInitialized(boolean printFeedbackInLog) {
//
//		boolean isInitialized = (this.uartAdapterReceipt >= 0) && isUartHandlerAccessible();
//
//		if (printFeedbackInLog && !isInitialized) {
//			Log.e(logTag, "UART interface '/dev/ttyMT"+this.uartInterface +"' is NOT initialized.");
//		}
//
//		return isInitialized;
//	}
//
//	private void throwExceptionIfNotInitialized() throws Exception {
//		if (!isInitialized(false)) {
//			throw new Exception("UART Initialization Failed");
//		}
//	}
//
//	public boolean isUartHandlerAccessible() {
//		return (new File("/dev/ttyMT"+this.uartInterface)).canRead();
//	}








	protected SerialPort mSerialPort;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;

	byte[] mBuffer;


	protected void onDataReceived(final byte[] buffer, final int size) {

	}


	public void testSerialConn() {


		try {

			mSerialPort = new SerialPort(new File("/dev/ttyMT0"), 19200, 0);

//			mOutputStream = mSerialPort.getOutputStream();
//			mInputStream = mSerialPort.getInputStream();
//
//			mBuffer = new byte[512];
//			Arrays.fill(mBuffer, (byte) 0x55);
//
//
//			try {
//				if (mOutputStream != null) {
//					mOutputStream.write(mBuffer);
//				} else {
//					return;
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

//			int size;
//			try {
//				byte[] buffer = new byte[64];
//				if (mInputStream == null) return;
//				size = mInputStream.read(buffer);
//				if (size > 0) {
//					onDataReceived(buffer, size);
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}


//			if (mSerialPort != null) {
//				mSerialPort.close();
//				mSerialPort = null;
//			}

		} catch (IOException e) {
			e.printStackTrace();
		}

//		SerialPort[] comPort = SerialPort.getCommPorts();
//
//		Log.e(logTag, "Serial Ports: "+comPort.length);

//		comPort.openPort();
//		comPort.addDataListener(new SerialPortDataListener() {
//			@Override
//			public int getListeningEvents() {
//				return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
//			}
//
//			@Override
//			public void serialEvent(SerialPortEvent event) {
//				byte[] newData = event.getReceivedData();
//				Log.e(logTag, "Received data of size: " + newData.length);
//				String str = "Data: ";
//				for (int i = 0; i < newData.length; ++i) {
//					str += (char) newData[i];
//				}
//				Log.e(logTag, str);
//			}
//		});
	}

}
