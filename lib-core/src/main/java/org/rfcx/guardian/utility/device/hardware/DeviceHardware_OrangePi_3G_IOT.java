package org.rfcx.guardian.utility.device.hardware;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.device.root.SystemBuildDotPropFile;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceHardware_OrangePi_3G_IOT {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceHardware_OrangePi_3G_IOT");

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

	public static void checkSetDeviceHardwareIdentification(Context context) {
		if (!DeviceHardwareUtils.getDeviceHardwareManufacturer().equalsIgnoreCase(DEVICE_MANUFACTURER[0])) {
			Log.i(logTag, "Device Hardware Identification has not yet been set. Building update script now...");
			String[] hardwareIdentificationPropertiesAndValues = new String[]{
					"ro.product.brand=" + DEVICE_MANUFACTURER[0],
					"ro.product.manufacturer=" + DEVICE_MANUFACTURER[0],
					"ro.product.model=" + DEVICE_MODEL[0],
					"ro.product.name=" + DEVICE_MODEL[0],
					"ro.product.device=" + DEVICE_MODEL[0],
					"ro.build.product=" + DEVICE_MODEL[0]
			};
			SystemBuildDotPropFile.updateBuildDotPropFile(hardwareIdentificationPropertiesAndValues, context, true);
		}
	}

}
