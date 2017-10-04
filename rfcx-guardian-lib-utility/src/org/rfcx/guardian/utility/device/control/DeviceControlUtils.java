package org.rfcx.guardian.utility.device.control;

import java.util.Locale;

import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class DeviceControlUtils {
	 
	public DeviceControlUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceControlUtils.class);
		this.appRole = appRole;
	}

	private String logTag = RfcxLog.generateLogTag("Utils", DeviceControlUtils.class);
	private String appRole = "Utils";
	
	public boolean runOrTriggerDeviceControl(String controlCommand, ContentResolver contentResolver) {

		// replace this with something that more dynamically determines whether the roles has root access
		boolean mustUseContentProvider = appRole.equalsIgnoreCase("guardian");
			
		if (mustUseContentProvider) {
			try { 
				Log.v(logTag, "Triggering '"+controlCommand+"' via content provider.");
				Cursor deviceControlResponse = 
						contentResolver.query(
							RfcxComm.getUri("admin", "control", controlCommand),
							RfcxComm.getProjection("admin", "control"),
							null, null, null);
				return true;
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				return false;
			}
		}
		return false;
	}
	
	
	
	
}
