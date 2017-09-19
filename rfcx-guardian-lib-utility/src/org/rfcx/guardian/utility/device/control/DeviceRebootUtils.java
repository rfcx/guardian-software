package org.rfcx.guardian.utility.device.control;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.util.Log;

public class DeviceRebootUtils {
	
	public DeviceRebootUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, this.getClass());
		this.appRole = appRole;
	}

	private String logTag = RfcxLog.generateLogTag("Utils", this.getClass());
	private String appRole = "Utils";
	
	public void executeDeviceReboot() {
		
		Log.d(logTag, "Here we would execute a device reboot!!! this would either trigger a content provider or the actual reboot, based on role permissions.");
		
		//		ShellCommands.executeCommand("reboot",null,false,((RfcxGuardian) getApplication()).getApplicationContext());
	}
	
	
	
	
}
