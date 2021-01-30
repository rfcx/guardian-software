package org.rfcx.guardian.utility.device.expansion;

import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;

public class DeviceUARTUtils {

	public DeviceUARTUtils(String appRole, int uartInterface) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceUART");
		this.uartInterface = uartInterface;
	}

	private String logTag;

	private int uartAdapterReceipt;

	// uartInterface should be a low integer, including zero, as in /dev/ttyMT0 or /dev/ttyMT1
	private final int uartInterface;

	public void initializeOrReInitialize() {

		Log.i(logTag, "Attempting to initialize UART interface '/dev/ttyMT"+this.uartInterface +"'");

		if (isUartHandlerAccessible()) {

//			this.i2cTools = new I2cTools();
//			this.i2cTools.i2cDeInit(this.uartInterface);
//			this.uartAdapterReceipt = i2cTools.i2cInit(this.uartInterface);

			if (isInitialized(true)) {
				Log.i(logTag, "UART interface '/dev/ttyMT" + this.uartInterface + "' successfully initialized.");
			}

		} else {
			Log.e(logTag, "UART handler '/dev/ttyMT"+this.uartInterface +"' is NOT accessible. Initialization failed.");
		}
	}

	public boolean isInitialized(boolean printFeedbackInLog) {

		boolean isInitialized = (this.uartAdapterReceipt >= 0) && isUartHandlerAccessible();

		if (printFeedbackInLog && !isInitialized) {
			Log.e(logTag, "UART interface '/dev/ttyMT"+this.uartInterface +"' is NOT initialized.");
		}

		return isInitialized;
	}

	private void throwExceptionIfNotInitialized() throws Exception {
		if (!isInitialized(false)) {
			throw new Exception("UART Initialization Failed");
		}
	}

	public boolean isUartHandlerAccessible() {
		return (new File("/dev/ttyMT"+this.uartInterface)).canRead();
	}


	
}
