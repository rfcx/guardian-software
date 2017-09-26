package org.rfcx.guardian.utility.device.control;

import java.util.Locale;

import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

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
		
		ContentValues contentValues = new ContentValues();
		
		if (mustUseContentProvider) {
			try { 
				contentResolver.update(Uri.parse(RfcxRole.ContentProvider.admin.URI_CONTROL+"/"+controlCommand.toLowerCase(Locale.US)), contentValues, null, null);
				return true;
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				return false;
			}
		}
		return false;
	}
	
	
	
	
}
