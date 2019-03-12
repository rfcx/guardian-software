package org.rfcx.guardian.utility.device.hardware;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceHardware_OrangePi_3GIOT {

	public DeviceHardware_OrangePi_3GIOT(String appRole) {

	}
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceHardware_OrangePi_3GIOT.class);
	
	public static final String DEVICE_NAME = "OrangePi 3GIOT";
		
	public static boolean isDevice_OrangePi_3GIOT() {
		return DeviceHardwareUtils.getDeviceHardwareName().equalsIgnoreCase(DEVICE_NAME);
	}

}
