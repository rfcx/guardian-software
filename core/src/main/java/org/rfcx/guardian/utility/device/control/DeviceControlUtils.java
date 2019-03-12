package org.rfcx.guardian.utility.device.control;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceControlUtils {
	 
	public DeviceControlUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceControlUtils.class);
		this.appRole = appRole;
	}

	private String logTag = RfcxLog.generateLogTag("Utils", DeviceControlUtils.class);
	private String appRole = "Utils";
	
	public boolean runOrTriggerDeviceControl(String controlCommand, ContentResolver contentResolver) {

		// replace this with something that more dynamically determines whether the roles has root access
		boolean mustUseContentProvider = appRole.equalsIgnoreCase("org.rfcx.guardian.guardian");
			
		if (mustUseContentProvider) {
			try { 
				Log.v(logTag, "Triggering '"+controlCommand+"' via content provider.");
				Cursor deviceControlResponse = 
						contentResolver.query(
							RfcxComm.getUri("org.rfcx.org.rfcx.guardian.guardian.admin", "control", controlCommand),
							RfcxComm.getProjection("org.rfcx.org.rfcx.guardian.guardian.admin", "control"),
							null, null, null);
				return true;
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				return false;
			}
		} else {
			
			if (controlCommand.equalsIgnoreCase("reboot")) {
				// should we trigger the service(s) directly here?
			}
		}
		return false;
	}
	
	
	
	
}
