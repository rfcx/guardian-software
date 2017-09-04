package org.rfcx.guardian.utility.device.control;

import android.util.Log;

public class DeviceRebootUtils {
	
	public DeviceRebootUtils(String appRole) {
		this.logTag = this.logTag = (new StringBuilder()).append("Rfcx-").append(appRole).append("-").append(DeviceRebootUtils.class.getSimpleName()).toString();
		this.appRole = appRole;
	}

	private String logTag = (new StringBuilder()).append("Rfcx-Utils-").append(DeviceRebootUtils.class.getSimpleName()).toString();
	
	private String appRole = "Utils";

	
	public void executeDeviceReboot() {
		Log.d(logTag, "Here we would execute a device reboot!!!");
		
		
		//		ShellCommands.executeCommand("reboot",null,false,((RfcxGuardian) getApplication()).getApplicationContext());
	}
	
	
	
	
}
