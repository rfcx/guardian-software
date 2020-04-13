package org.rfcx.guardian.guardian.contentprovider;

import org.rfcx.guardian.utility.device.AppProcessInfo;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class GuardianContentProvider extends ContentProvider {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ContentProvider");

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

			} else if (RfcxComm.uriMatch(uri, appRole, "prefs_set", "*")) {
				String pathSeg = uri.getLastPathSegment();
				String pathSegPrefKey = pathSeg.substring(0, pathSeg.indexOf("|"));
				String pathSegPrefValue = pathSeg.substring(1 + pathSeg.indexOf("|"));
				app.setSharedPref(pathSegPrefKey, pathSegPrefValue);
				return RfcxComm.getProjectionCursor(appRole, "prefs_set", new Object[]{ pathSegPrefKey, pathSegPrefValue, System.currentTimeMillis()});

			// "process" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "process", null)) {
				return RfcxComm.getProjectionCursor(appRole, "process", new Object[] { "org.rfcx.guardian."+appRole, AppProcessInfo.getAppProcessId(), AppProcessInfo.getAppUserId() });

			// "control" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "control", "kill")) {
				app.rfcxServiceHandler.stopAllServices();
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"kill", null, System.currentTimeMillis()});
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}


	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}
	
}
