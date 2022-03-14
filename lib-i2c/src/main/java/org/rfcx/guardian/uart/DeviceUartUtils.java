package org.rfcx.guardian.uart;

import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeviceUartUtils {

    private final String logTag;
    private final Map<String, String> addrMap = new HashMap<String, String>();
    private String uartHandlerFilepath;
    private boolean isHandlerReadable = false;

    public DeviceUartUtils(String appRole) {
        this.logTag = RfcxLog.generateLogTag(appRole, "DeviceUartUtils");
    }

    public static boolean isUartHandlerAccessible(int uartInterface) {
        return (new File("/dev/ttyMT" + uartInterface)).canRead() && (new File("/dev/ttyMT" + uartInterface)).canWrite();
    }

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
            File handlerFile = (new File(uartHandlerFilepath + addrNum));
            if (handlerFile.exists() && handlerFile.canRead() && handlerFile.canWrite()) {
                this.isHandlerReadable = true;
            } else {
                Log.e(logTag, "Could not access UART Handler: " + uartHandlerFilepath + addrNum);
            }
        }
        return this.isHandlerReadable;
    }

    private void setAddrNumByName(String addrName, int addrNum) {
        this.addrMap.remove(addrName.toUpperCase(Locale.US));
        this.addrMap.put(addrName.toUpperCase(Locale.US), "" + addrNum);
    }

    private void setAddrNumByName(String addrName, String addrNum) {
        setAddrNumByName(addrName, Integer.parseInt(addrNum));
    }

    private int getAddrNumByName(String addrName) {
        int addrNum = 0;
        if (this.addrMap.containsKey(addrName.toUpperCase(Locale.US))) {
            addrNum = Integer.parseInt(this.addrMap.get(addrName.toUpperCase(Locale.US)));
        } else {
            Log.e(logTag, "No UART Interface assignment for '" + addrName + "'");
        }
        return addrNum;
    }

//	public boolean isInitialized(boolean printFeedbackInLog, int uartInterface) {
//
//		boolean isInitialized = (this.uartAdapterReceipt >= 0) && isUartHandlerAccessible();
//
//		if (printFeedbackInLog && !isInitialized) {
//			Log.e(logTag, "UART interface '/dev/ttyMT"+uartInterface +"' is NOT initialized.");
//		}
//
//		return isInitialized;
//	}
//
////	private void throwExceptionIfNotInitialized() throws Exception {
////		if (!isInitialized(false)) {
////			throw new Exception("UART Initialization Failed");
////		}
////	}

    public void initializeOrReInitialize(int uartInterface) {

        Log.i(logTag, "Attempting to initialize UART interface '/dev/ttyMT" + uartInterface + "'");

        if (isUartHandlerAccessible(uartInterface)) {

//			if (isInitialized(true)) {
//				Log.i(logTag, "UART interface '/dev/ttyMT" + uartInterface + "' successfully initialized.");
//			}

        } else {
            Log.e(logTag, "UART handler '/dev/ttyMT" + uartInterface + "' is NOT accessible. Initialization failed.");
        }
    }


}
