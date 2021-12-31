package org.rfcx.guardian.utility.device.hardware;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.rfcx.guardian.utility.device.root.SystemBuildDotPropFile;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeviceHardware_OrangePi_3G_IOT {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceHardware_OrangePi_3G_IOT");

	public static final String[] DEVICE_MANUFACTURER = new String[] { "OrangePi", "alps" };
	public static final String[] DEVICE_MODEL = new String[] { "3G_IOT", "hexing72_cwet_kk" };
	public static final String[] DEVICE_BRAND = new String[] { "RFCx", "alps" };
	public static final String[] DEVICE_PRODUCT = new String[] { "Guardian", "hexing72_cwet_kk" };

	// I2C settings for OrangePi 3G-IoT
	public static final int DEVICE_I2C_INTERFACE = 1;

	// UART settings for OrangePi 3G-IoT
	public static final String DEVICE_TTY_FILEPATH_SATELLITE = "/dev/ttyMT1";

	// GPIO settings for OrangePi 3G-IoT
	public static final String DEVICE_GPIO_HANDLER_FILEPATH = "/sys/devices/virtual/misc/mtgpio/pin";
	public static final Map<String, String[]> DEVICE_GPIO_MAP = Collections.unmodifiableMap(new HashMap<String, String[]>() {{
		put("sentry_power", 	new String[] { "56", "write" });
		put("satellite_state", 	new String[] { "58", "read" });
		put("satellite_power", 	new String[] { "128", "write" });
		put("voltage_refr", 	new String[] { "56", "write" });
	}});

	// Busybox filepath for OrangePi 3G-IoT
	public static final String BUSYBOX_FILEPATH = "/system/xbin/busybox";
		
	public static boolean isDevice_OrangePi_3G_IOT() {
		for (String manufacturer : DEVICE_MANUFACTURER) { if (DeviceHardwareUtils.getManufacturer().equalsIgnoreCase(manufacturer)) {
			for (String model : DEVICE_MODEL) { if (DeviceHardwareUtils.getModel().equalsIgnoreCase(model)) {
				return true;
			} }
		} }
		return false;
	}

	public static void checkSetDeviceHardwareIdentification(Context context) {
		if (!DeviceHardwareUtils.getManufacturer().equalsIgnoreCase(DEVICE_MANUFACTURER[0])) {
			Log.i(logTag, "Device Hardware Identification has not yet been set. Building update script now...");
			String[] hardwareIdentificationPropertiesAndValues = new String[]{
					"ro.product.brand=" + DEVICE_BRAND[0],
					"ro.product.manufacturer=" + DEVICE_MANUFACTURER[0],
					"ro.product.model=" + DEVICE_MODEL[0],
					"ro.product.device=" + DEVICE_MODEL[0],
					"ro.product.name=" + DEVICE_PRODUCT[0],
					"ro.build.product=" + DEVICE_PRODUCT[0]
			};
			SystemBuildDotPropFile.updateBuildDotPropFile(hardwareIdentificationPropertiesAndValues, context, true);
		}
	}

	public static final Map<String, String[]> DEVICE_SYSTEM_SETTINGS = Collections.unmodifiableMap(new HashMap<String, String[]>() {{
		put("user_setup_complete", 			new String[] { "secure", "i", "1" });
		put("data_roaming", 				new String[] { "global", "i", "1" });
		put("preferred_network_mode", 		new String[] { "global", "i", "0" });
		put("sms_default_application", 		new String[] { "secure", "s", "org.rfcx.guardian.admin" });
		put("sms_outgoing_check_max_count", new String[] { "global", "i", "99999" });
		put("assisted_gps_enabled", 		new String[] { "global", "i", "1" });
		put("airplane_mode_radios", 		new String[] { "global", "s", "cell,nfc,wimax" });
		put("set_install_location", 		new String[] { "global", "i", "1" });
		put("power_sounds_enabled", 		new String[] { "global", "i", "0" });
		put("lockscreen_sounds_enabled", 	new String[] { "system", "i", "0" });
//		put("screen_off_timeout", 			new String[] { "system", "i", "300000" });
//		put("screen_off_timeout", 			new String[] { "system", "i", "15000" });
		put("screen_off_timeout", 			new String[] { "system", "i", "-1" });
	}});


	public static final String DEVICE_CPU_GOVERNOR_DIRPATH = "/sys/devices/system/cpu";
	public static final Map<String, String[]> DEVICE_CPU_GOVERNOR_SETTINGS = Collections.unmodifiableMap(new HashMap<String, String[]>() {{

		put("cpu_down_differential",		new String[] { "hotplug", "20" });
		put("down_differential",			new String[] { "hotplug", "40" });
		put("up_threshold",					new String[] { "hotplug", "98" });
		put("powersave_bias",				new String[] { "hotplug", "150" });

/*
		put("cpu_down_avg_times",			new String[] { "hotplug", "50" });
		put("cpu_up_avg_times",				new String[] { "hotplug", "10" });
		put("cpu_up_threshold",				new String[] { "hotplug", "50" });
		put("cpu_num_limit",				new String[] { "hotplug", "2" });
		put("screenoff_maxfreq",			new String[] { "hotplug", "0" });
		put("ignore_nice_load",				new String[] { "hotplug", "0" });
		put("io_is_busy",					new String[] { "hotplug", "1" });
		put("od_threshold",					new String[] { "hotplug", "98" });
		put("sampling_down_factor",			new String[] { "hotplug", "1" });
		put("sampling_rate",				new String[] { "hotplug", "30000" });
		put("thermal_dispatch_avg_times",	new String[] { "hotplug", "30" });
		put("cpu_input_boost_enable",		new String[] { "hotplug", "1" });
		put("is_cpu_hotplug_disable",		new String[] { "hotplug", "0" });
		put("cpu_num_base",					new String[] { "hotplug", "1" });
*/
	}});





}
