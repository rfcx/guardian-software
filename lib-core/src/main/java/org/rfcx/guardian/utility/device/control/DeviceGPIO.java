package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.Locale;

public class DeviceGPIO {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceGPIO");

	private static final String[] COMMAND_OPTIONS = new String[] { "MODE", "PULL_SEL", "DIN", "DOUT", "PULL_EN", "DIR", "INV", "IES" };
	private static final String GPIO_PIN_FILEPATH = "/sys/devices/virtual/misc/mtgpio/pin";

	public static void setGPIO(String cmd, int gpioAddress, boolean enableOrDisable) {

		if (ArrayUtils.doesStringArrayContainString(COMMAND_OPTIONS, cmd.toUpperCase(Locale.US))) {

			Log.v(logTag, ((enableOrDisable) ? "Enabling" : "Disabling") + " GPIO "+cmd.toUpperCase(Locale.US)+" " + gpioAddress);

			if (cmd.substring(0,5).equalsIgnoreCase("PULL_")) { cmd = "P"+cmd.substring(5); }

			ShellCommands.executeCommandAndIgnoreOutput(
					"echo"
							+ " -w" + cmd.toLowerCase(Locale.US)
							+ " " + gpioAddress
							+ " " + (enableOrDisable ? "1" : "0")
							+ " > " + GPIO_PIN_FILEPATH + ";"
			);

		} else {
			Log.e(logTag, "Command '"+cmd+"' could not be matched to a GPIO command");
		}
	}

//	public static void setADBoverTCP(boolean enableOrDisable, Context context) {
//		setADBoverTCP(enableOrDisable, DEFAULT_TCP_PORT, context);
//	}
//
//	public static void enableADBoverTCP(int tcpPort, Context context) {
//		setADBoverTCP(true, tcpPort, context);
//	}
//
//	public static void enableADBoverTCP(Context context) {
//		setADBoverTCP(true, DEFAULT_TCP_PORT, context);
//	}
//
//	public static void disableADBoverTCP(Context context) {
//		setADBoverTCP(false, 0, context);
//	}
	
}
