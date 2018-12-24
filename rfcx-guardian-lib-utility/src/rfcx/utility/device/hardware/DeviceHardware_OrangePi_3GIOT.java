package rfcx.utility.device.hardware;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceHardware_OrangePi_3GIOT {

	public DeviceHardware_OrangePi_3GIOT(String appRole) {

	}
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceHardware_OrangePi_3GIOT.class);
	
	public static final String DEVICE_NAME = "OrangePi 3GIOT";
		
	public static boolean isDevice_OrangePi_3GIOT() {
		return DeviceHardwareUtils.getDeviceHardwareName().equalsIgnoreCase(DEVICE_NAME);
	}

}
