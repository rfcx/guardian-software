package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxGarbageCollection;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceReboot {

	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceReboot.class);




	public static boolean triggerForcedRebootAsRoot(Context context) {
		int rebootPreDelay = 3;
		Log.v(logTag, "Attempting graceful reboot... then after "+rebootPreDelay+" seconds, killing RFCx processes and forcing reboot...");
		RfcxGarbageCollection.runAndroidGarbageCollection();
		ShellCommands.executeCommandAsRoot(""
						+"am start -a android.intent.action.REBOOT; "
						+"am broadcast android.intent.action.ACTION_SHUTDOWN; "
						+"sleep "+rebootPreDelay
						+" && kill $(ps | grep org.rfcx.org.rfcx.guardian.guardian | cut -d \" \" -f 5)"
						+" && umount -vl "+ Environment.getExternalStorageDirectory().toString()
						+" && reboot; "
						+"sleep "+rebootPreDelay+" && reboot; "
				, context);
		return true;
	}


	
}
