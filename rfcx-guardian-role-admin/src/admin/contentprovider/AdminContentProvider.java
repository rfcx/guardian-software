package admin.contentprovider;

import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import admin.RfcxGuardian;
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
				
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "screenshot")) {
				app.rfcxServiceHandler.triggerService("ScreenShotJob", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "screenshot", null, System.currentTimeMillis() });
			
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_off")) {
				app.rfcxServiceHandler.triggerService("AirplaneModeOffJob", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "airplanemode_off", null, System.currentTimeMillis() });
			
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_on")) {
				app.rfcxServiceHandler.triggerService("AirplaneModeOnJob", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "airplanemode_on", null, System.currentTimeMillis() });
			
			// "database" function endpoints
			
			} else if (RfcxComm.uriMatch(uri, appRole, "database_get_all_rows", "*")) {
				String pathSeg = uri.getLastPathSegment();
				
				if (pathSeg.equalsIgnoreCase("sms")) {
					return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[] { "sms", DeviceSmsUtils.getSmsMessagesAsJson(app.getApplicationContext().getContentResolver()), System.currentTimeMillis() });
				
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
				String pathSegTable = pathSeg.substring(0,pathSeg.indexOf("-"));
				String pathSegId = pathSeg.substring(1+pathSeg.indexOf("-"));
				
				if (pathSegTable.equalsIgnoreCase("sms")) {
					return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[] { pathSeg, DeviceSmsUtils.deleteSmsMessage(pathSegId, app.getApplicationContext().getContentResolver()), System.currentTimeMillis() });	
				
				} else if (pathSegTable.equalsIgnoreCase("screenshots")) {
					return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[] { pathSeg, app.deviceScreenShotDb.dbCaptured.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis() });	

				} else if (pathSegTable.equalsIgnoreCase("logs")) {
					return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[] { pathSeg, app.deviceLogCatCaptureDb.dbCaptured.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis() });	
									
				} else {
					return null;
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
