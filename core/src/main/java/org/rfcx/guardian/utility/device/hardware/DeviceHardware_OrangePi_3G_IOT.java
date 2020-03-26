package org.rfcx.guardian.utility.device.hardware;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceHardware_OrangePi_3G_IOT {

	public DeviceHardware_OrangePi_3G_IOT(String appRole) {

	}
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceHardware_OrangePi_3G_IOT.class);

	public static final String[] DEVICE_MANUFACTURER = new String[] { "alps", "OrangePi" };
	public static final String[] DEVICE_MODEL = new String[] { "hexing72_cwet_kk", "3G-IOT" };
		
	public static boolean isDevice_OrangePi_3G_IOT() {
		for (String manufacturer : DEVICE_MANUFACTURER) { if (DeviceHardwareUtils.getDeviceHardwareManufacturer().equalsIgnoreCase(manufacturer)) {
			for (String model : DEVICE_MODEL) { if (DeviceHardwareUtils.getDeviceHardwareModel().equalsIgnoreCase(model)) {
				return true;
			} }
		} }
		return false;
	}

}
