package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceADB {

	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceADB.class);

	public static final int DEFAULT_TCP_PORT = 4455;

	public static void setADBoverTCP(boolean enableOrDisable, int tcpPort, Context context) {
		if (tcpPort <= 0) { tcpPort = DEFAULT_TCP_PORT; }
		Log.v(logTag, ((enableOrDisable) ? "Enabling" : "Disabling") + " ADB over TCP on port "+tcpPort);
		ShellCommands.executeCommandAsRootAndIgnoreOutput("setprop persist.adb.tcp.port "+((enableOrDisable) ? tcpPort : "\"\""), context);
	}

	public static void setADBoverTCP(boolean enableOrDisable, Context context) {
		setADBoverTCP(enableOrDisable, DEFAULT_TCP_PORT, context);
	}

	public static void enableADBoverTCP(int tcpPort, Context context) {
		setADBoverTCP(true, tcpPort, context);
	}

	public static void enableADBoverTCP(Context context) {
		setADBoverTCP(true, DEFAULT_TCP_PORT, context);
	}

	public static void disableADBoverTCP(Context context) {
		setADBoverTCP(false, 0, context);
	}
	
}
