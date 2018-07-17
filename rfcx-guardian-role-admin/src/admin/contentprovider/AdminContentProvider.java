package admin.contentprovider;

import rfcx.utility.device.DeviceSmsUtils;
import rfcx.utility.rfcx.RfcxComm;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxRole;

import admin.RfcxGuardian;
import admin.device.sentinel.DeviceSentinelPowerUtils;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class AdminContentProvider extends ContentProvider {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AdminContentProvider.class);
	
	private static final String appRole = RfcxGuardian.APP_ROLE;

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
		
			// get role "version" endpoints
			
			if (RfcxComm.uriMatch(uri, appRole, "version", null)) {
				return RfcxComm.getProjectionCursor(appRole, "version", new Object[] { appRole, RfcxRole.getRoleVersion(app.getApplicationContext(), logTag) });

			// "prefs" function endpoints
			
			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", null)) {
				MatrixCursor cursor = RfcxComm.getProjectionCursor(appRole, "prefs", null);
				for (String prefKey : app.rfcxPrefs.listPrefsKeys()) {
					cursor.addRow(new Object[] { prefKey, app.rfcxPrefs.getPrefAsString(prefKey) });
				}
				return cursor;
				
			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", "*")) {
				String prefKey = uri.getLastPathSegment();
				return RfcxComm.getProjectionCursor(appRole, "prefs", new Object[] { prefKey, app.rfcxPrefs.getPrefAsString(prefKey) });
				
			// "control" function endpoints
			
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "reboot")) {
				app.rfcxServiceHandler.triggerService("RebootTrigger", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "reboot", null, System.currentTimeMillis() });
				
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "relaunch")) {
				app.rfcxServiceHandler.triggerService("ForceRoleRelaunch", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "relaunch", null, System.currentTimeMillis() });
				
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "screenshot")) {
				app.rfcxServiceHandler.triggerService("ScreenShotJob", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "screenshot", null, System.currentTimeMillis() });
			
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_off")) {
				app.rfcxServiceHandler.triggerService("AirplaneModeOffJob", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "airplanemode_off", null, System.currentTimeMillis() });
			
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_on")) {
				app.rfcxServiceHandler.triggerService("AirplaneModeOnJob", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "airplanemode_on", null, System.currentTimeMillis() });
				
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "datetime_sntp_sync")) {
				app.rfcxServiceHandler.triggerService("DateTimeSntpSyncJob", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "datetime_sntp_sync", null, System.currentTimeMillis() });

			} else if (RfcxComm.uriMatch(uri, appRole, "sms_send", "*")) {
				String pathSeg = uri.getLastPathSegment();
				String pathSegAddress = pathSeg.substring(0,pathSeg.indexOf("|"));
				String pathSegMessage = pathSeg.substring(1+pathSeg.indexOf("|"));
				DeviceSmsUtils.sendSmsMessage(pathSegAddress, pathSegMessage);
				return RfcxComm.getProjectionCursor(appRole, "sms_send", new Object[] { pathSegAddress+"|"+pathSegMessage, null, System.currentTimeMillis() });	
				
			// "database" function endpoints
			
			} else if (RfcxComm.uriMatch(uri, appRole, "database_get_all_rows", "*")) {
				String pathSeg = uri.getLastPathSegment();
				
				if (pathSeg.equalsIgnoreCase("sms")) {
					return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[] { "sms", DeviceSmsUtils.getSmsMessagesAsJsonArray(app.getApplicationContext().getContentResolver()).toString(), System.currentTimeMillis() });
				
				} else if (pathSeg.equalsIgnoreCase("sentinel_power")) {
					return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[] { "sentinel_power", DeviceSentinelPowerUtils.getSentinelPowerValuesAsJsonArray(app.getApplicationContext()).toString(), System.currentTimeMillis() });
				
				} else {
					return null;
				}
				
			} else if (RfcxComm.uriMatch(uri, appRole, "database_get_latest_row", "*")) {
				String pathSeg = uri.getLastPathSegment();
				
				if (pathSeg.equalsIgnoreCase("screenshots")) {
					return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[] { "screenshots", app.deviceScreenShotDb.dbCaptured.getLatestRowAsJsonArray().toString(), System.currentTimeMillis() });
				
				} else if (pathSeg.equalsIgnoreCase("logs")) {
					return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[] { "logs", app.deviceLogCatCaptureDb.dbCaptured.getLatestRowAsJsonArray().toString(), System.currentTimeMillis() });
				
				} else {
					return null;
				}
			
			} else if (RfcxComm.uriMatch(uri, appRole, "database_delete_row", "*")) {
				String pathSeg = uri.getLastPathSegment();
				String pathSegTable = pathSeg.substring(0,pathSeg.indexOf("|"));
				String pathSegId = pathSeg.substring(1+pathSeg.indexOf("|"));
				
				if (pathSegTable.equalsIgnoreCase("sms")) {
					return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[] { pathSeg, DeviceSmsUtils.deleteSmsMessage(pathSegId, app.getApplicationContext().getContentResolver()), System.currentTimeMillis() });	
				
				} else if (pathSegTable.equalsIgnoreCase("screenshots")) {
					return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[] { pathSeg, app.deviceScreenShotDb.dbCaptured.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis() });	

				} else if (pathSegTable.equalsIgnoreCase("logs")) {
					return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[] { pathSeg, app.deviceLogCatCaptureDb.dbCaptured.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis() });	
									
				} else {
					return null;
				}
				
			} else if (RfcxComm.uriMatch(uri, appRole, "database_delete_rows_before", "*")) {
				String pathSeg = uri.getLastPathSegment();
				String pathSegTable = pathSeg.substring(0,pathSeg.indexOf("|"));
				String pathSegTimeStamp = pathSeg.substring(1+pathSeg.indexOf("|"));
				
				if (pathSegTable.equalsIgnoreCase("sentinel_power")) {
					return RfcxComm.getProjectionCursor(appRole, "database_delete_rows_before", new Object[] { pathSeg, DeviceSentinelPowerUtils.deleteSentinelPowerValuesBeforeTimestamp(pathSegTimeStamp, app.getApplicationContext()), System.currentTimeMillis() });	
				
				}
				
			}
			
		
			
			
			
			
			return null;
						
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return 0;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return 0;
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public String getType(Uri uri) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return null;
	}
	
}
