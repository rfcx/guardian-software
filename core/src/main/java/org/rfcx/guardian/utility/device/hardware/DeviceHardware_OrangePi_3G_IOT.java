package org.rfcx.guardian.utility.device.hardware;

import android.content.Context;

import org.rfcx.guardian.utility.device.control.DeviceAndroidSystemBuildDotPropFile;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceHardware_OrangePi_3G_IOT {

	public DeviceHardware_OrangePi_3G_IOT(String appRole) {

	}
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceHardware_OrangePi_3G_IOT.class);

	public static final String[] DEVICE_MANUFACTURER = new String[] { "OrangePi", "alps" };
	public static final String[] DEVICE_MODEL = new String[] { "3G_IOT", "hexing72_cwet_kk" };
		
	public static boolean isDevice_OrangePi_3G_IOT() {
		for (String manufacturer : DEVICE_MANUFACTURER) { if (DeviceHardwareUtils.getDeviceHardwareManufacturer().equalsIgnoreCase(manufacturer)) {
			for (String model : DEVICE_MODEL) { if (DeviceHardwareUtils.getDeviceHardwareModel().equalsIgnoreCase(model)) {
				return true;
			} }
		} }
		return false;
	}

	public static void setDeviceDefaultState(Context context) {

		String[] propertiesAndValues = new String[] {
				"ro.product.brand="+DEVICE_MANUFACTURER[0],
				"ro.product.manufacturer="+DEVICE_MANUFACTURER[0],
				"ro.product.model="+DEVICE_MODEL[0],
				"ro.product.name="+DEVICE_MODEL[0],
				"ro.product.device="+DEVICE_MODEL[0],
				"ro.build.product="+DEVICE_MODEL[0]
		};

		DeviceAndroidSystemBuildDotPropFile.updateBuildDotPropFile(propertiesAndValues, context);

	}

}
