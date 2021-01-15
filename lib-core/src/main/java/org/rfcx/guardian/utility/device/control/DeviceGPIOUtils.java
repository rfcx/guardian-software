package org.rfcx.guardian.utility.device.control;

import android.util.Log;

import org.rfcx.guardian.utility.device.hardware.DeviceHardware_OrangePi_3G_IOT;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeviceGPIOUtils {

	public DeviceGPIOUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceGPIO");
	}

	private String logTag;

	private String gpioHandlerFilepath;

	private static final String[] GPIO_POSSIBLE_COMMANDS = new String[] { "MODE", "PULL_SEL", "DIN", "DOUT", "PULL_EN", "DIR", "INV", "IES" };

	private Map<String, String> pinMap = new HashMap<String, String>();

	public void setPinByName(String pinName, int pinNumber) {
		this.pinMap.remove(pinName.toUpperCase(Locale.US));
		this.pinMap.put(pinName.toUpperCase(Locale.US), ""+pinNumber);
	}

	public void setPinByName(String pinName, String pinNumber) {
		setPinByName(pinName, Integer.parseInt(pinNumber));
	}

	public void setPinsByName(Map<String, String> pinMap) {
		for (Map.Entry pins : pinMap.entrySet()) {
			setPinByName(pins.getKey().toString(), pinMap.get(pins.getKey().toString()));
		}
	}

	private int getPinByName(String pinName) {
		int pinNmbr = 0;
		if (this.pinMap.containsKey(pinName.toUpperCase(Locale.US))) {
			pinNmbr = Integer.parseInt(this.pinMap.get(pinName.toUpperCase(Locale.US)));
		}
		return pinNmbr;
	}

	public void setGpioHandlerFilepath(String handlerFilepath) {
		this.gpioHandlerFilepath = handlerFilepath;
	}

	public void runGPIOCommand(String cmd, String address, boolean enableOrDisable) {

		if (ArrayUtils.doesStringArrayContainString(GPIO_POSSIBLE_COMMANDS, cmd.toUpperCase(Locale.US))) {

			if ((cmd.length() > 5) && cmd.substring(0,5).equalsIgnoreCase("PULL_")) { cmd = "P"+cmd.substring(5); }

			int pinAddr = getPinByName(address);

			String execStr = "echo"
					+ " -w" + cmd.toLowerCase(Locale.US)
					+ " " + pinAddr
					+ " " + (enableOrDisable ? "1" : "0")
					+ " > " + gpioHandlerFilepath + ";";

			if (pinAddr == 0) {
				Log.e(logTag, "No GPIO pin assignment for '"+address+"'");
			} else if (FileUtils.exists(gpioHandlerFilepath)) {
				Log.v(logTag, ((enableOrDisable) ? "Enabling" : "Disabling") + " GPIO "+cmd.toUpperCase(Locale.US)+" " + address);
				ShellCommands.executeCommandAndIgnoreOutput(execStr);
			} else {
				Log.e(logTag, "Could not find GPIO handler: "+ gpioHandlerFilepath);
			}
		} else {
			Log.e(logTag, "Command '"+cmd+"' could not be matched to a GPIO command");
		}
	}

	
}
