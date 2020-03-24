package org.rfcx.guardian.utility.device.control;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceADB {

	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceADB.class);

	public static void enableADBoverTCP(int tcpPort, Context context) {
		Log.v(logTag, "Enabling ADB over TCP on port "+tcpPort);
		ShellCommands.executeCommandAsRootAndIgnoreOutput("setprop persist.adb.tcp.port "+tcpPort, context);
	}

	public static void disableADBoverTCP(Context context) {
		Log.v(logTag, "Disabling ADB over TCP");
		ShellCommands.executeCommandAsRootAndIgnoreOutput("setprop persist.adb.tcp.port \"\"", context);
	}
	
}
