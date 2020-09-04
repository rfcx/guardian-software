package org.rfcx.guardian.utility.device.control;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceControlUtils {
	 
	public DeviceControlUtils(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceControlUtils");
		this.appRole = appRole;
	}

	private String logTag;
	private String appRole = "Guardian";
	
	public boolean runOrTriggerDeviceControl(String controlCommand, ContentResolver contentResolver) {

		// replace this with something that more dynamically determines whether the roles has root access
		boolean mustUseContentProvider = appRole.equalsIgnoreCase("Guardian");
			
		if (mustUseContentProvider) {
			try {
				String targetRole = controlCommand.equalsIgnoreCase("software_update") ? "updater" : "admin";
				Log.v(logTag, "Triggering '"+controlCommand+"' via "+targetRole+" role content provider.");
				Cursor deviceControlResponse =
						contentResolver.query(
							RfcxComm.getUri(targetRole, "control", controlCommand),
							RfcxComm.getProjection(targetRole, "control"),
							null, null, null);
				Log.v(logTag, deviceControlResponse.toString());
				deviceControlResponse.close();
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
//	public boolean runOrTriggerDbFromAdmin(String controlCommand, ContentResolver contentResolver) {
//
//		// replace this with something that more dynamically determines whether the roles has root access
//		boolean mustUseContentProvider = appRole.equalsIgnoreCase("Guardian");
//
//		if (mustUseContentProvider) {
//			try {
//				Log.v(logTag, "Triggering '"+controlCommand+"' via content provider.");
//				Cursor dbFetchingResponse =
//						contentResolver.query(
//								RfcxComm.getUri("admin", "database_get_latest_row", controlCommand),
//								RfcxComm.getProjection("admin", "database_get_latest_row"),
//								null, null, null);
//				Log.v(logTag, dbFetchingResponse.getCount()+"");
//				dbFetchingResponse.close();
//				return true;
//			} catch (Exception e) {
//				RfcxLog.logExc(logTag, e);
//				return false;
//			}
//		}
//		return false;
//	}
	
	
	
}
