package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceNetworkName {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceNetworkName");

	public static void setName(String networkName, Context context) {
		Log.v(logTag, "Setting device network name: '"+networkName+"'");
		ShellCommands.executeCommandAsRootAndIgnoreOutput("setprop net.hostname "+networkName, context);
	}
	
}
