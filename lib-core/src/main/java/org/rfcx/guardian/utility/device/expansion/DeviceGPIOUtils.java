package org.rfcx.guardian.utility.device.expansion;

import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeviceGPIOUtils {

	public DeviceGPIOUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceGPIO");
	}

	private final String logTag;

	private String gpioHandlerFilepath;
	private boolean isHandlerReadable = false;

	private static final String[] GPIO_POSSIBLE_COMMANDS = new String[] { "MODE", "PULL_SEL", "DOUT", "PULL_EN", "DIR", "IES", "SMT" };

	private final Map<String, String> pinAddrMap = new HashMap<String, String>();
	private final Map<String, String> pinDirMap = new HashMap<String, String>();


	public void setupPins(Map<String, String[]> pinMap) {
		for (Map.Entry pins : pinMap.entrySet()) {
			String pinName = pins.getKey().toString();
			setPinAddrByName(pinName, pinMap.get(pinName)[0]);
			setPinDirByName(pinName, pinMap.get(pinName)[1]);
		}
	}

	private boolean checkSetIsHandlerAccessible() {
		if (!this.isHandlerReadable) {
			File handlerFile = (new File(gpioHandlerFilepath));
			if (handlerFile.exists() && handlerFile.canRead() && handlerFile.canWrite()) {
				this.isHandlerReadable = true;
			} else {
				Log.e(logTag, "Could not access GPIO Handler: "+ gpioHandlerFilepath);
			}
		}
		return this.isHandlerReadable;
	}

	private String checkSetGpioCommand(String gpioCmd) {
		String outputCmd = null;

		if (ArrayUtils.doesStringArrayContainString(GPIO_POSSIBLE_COMMANDS, gpioCmd.toUpperCase(Locale.US))) {
			if ((gpioCmd.length() > 5) && gpioCmd.substring(0,5).equalsIgnoreCase("PULL_")) {
				gpioCmd = "P"+gpioCmd.substring(5);
			}
			outputCmd = gpioCmd;
		} else {
			Log.e(logTag, "Command '"+gpioCmd+"' could not be matched to a GPIO command");
		}

		return outputCmd;
	}

	public void setGpioHandlerFilepath(String handlerFilepath) {
		this.gpioHandlerFilepath = handlerFilepath;
	}

	public void runGPIOCommand(String cmd, String pinName, boolean enableOrDisable) {

		try {

			int pinAddr = getPinAddrByName(pinName);
			String pinCmd = checkSetGpioCommand(cmd);

			if (checkSetIsHandlerAccessible() && (pinAddr != 0) && (pinCmd != null)) {

				String execStr = "echo"
						+ " -w" + pinCmd.toLowerCase(Locale.US)
						+ " " + pinAddr
						+ " " + (enableOrDisable ? "1" : "0")
						+ " > " + gpioHandlerFilepath + ";";

				Log.v(logTag, ((enableOrDisable) ? "Enabling" : "Disabling") + " GPIO "+pinCmd.toUpperCase(Locale.US)+" " + pinName);

				ShellCommands.executeCommandAndIgnoreOutput(execStr);
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}


	public boolean readGPIOPin(String address, String readField) {

		try {

			int pinAddr = getPinAddrByName(address);

			if (checkSetIsHandlerAccessible() && (pinAddr != 0)) {

				String execStr = "cat " + gpioHandlerFilepath + " | grep '" + pinAddr + ":';";

				for (String execRtrn : ShellCommands.executeCommand(execStr)) {
					String rtrnStr = execRtrn.substring(execRtrn.indexOf(":")+1, execRtrn.lastIndexOf("-"));
					int readFieldInd = ArrayUtils.indexOfStringInStringArray(GPIO_POSSIBLE_COMMANDS, readField.toUpperCase(Locale.US));
					if (readFieldInd >= 0) {
						return 1 == Integer.parseInt(rtrnStr.substring(readFieldInd, readFieldInd + 1), 2);
					}
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}




	private void setPinAddrByName(String pinName, int pinNumber) {
		this.pinAddrMap.remove(pinName.toUpperCase(Locale.US));
		this.pinAddrMap.put(pinName.toUpperCase(Locale.US), ""+pinNumber);
	}

	private void setPinAddrByName(String pinName, String pinNumber) {
		setPinAddrByName(pinName, Integer.parseInt(pinNumber));
	}

	private int getPinAddrByName(String pinName) {
		int pinNmbr = 0;
		if (this.pinAddrMap.containsKey(pinName.toUpperCase(Locale.US))) {
			pinNmbr = Integer.parseInt(this.pinAddrMap.get(pinName.toUpperCase(Locale.US)));
		} else {
			Log.e(logTag, "No GPIO pin assignment for '"+pinName+"'");
		}
		return pinNmbr;
	}



	private void setPinDirByName(String pinName, String pinDirection) {
		this.pinDirMap.remove(pinName.toUpperCase(Locale.US));
		this.pinDirMap.put(pinName.toUpperCase(Locale.US), pinDirection);
		boolean isWrite = pinDirection.equalsIgnoreCase("write") || pinDirection.equalsIgnoreCase("w");
		runGPIOCommand("DIR", pinName, isWrite);
	}

	private String getPinDirByName(String pinName) {
		String pinDir = "write";
		if (this.pinDirMap.containsKey(pinName.toUpperCase(Locale.US))) {
			pinDir = this.pinDirMap.get(pinName.toUpperCase(Locale.US));
		} else {
			Log.e(logTag, "No GPIO direction assignment for '"+pinName+"'");
		}
		return pinDir;
	}

}
