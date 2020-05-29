package org.rfcx.guardian.guardian.contentprovider;

import org.json.JSONArray;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureUtils;
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
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "GuardianContentProvider");

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
				String pathSegPrefVal = pathSeg.substring(1 + pathSeg.indexOf("|"));
				app.setSharedPref(pathSegPrefKey, pathSegPrefVal);
				return RfcxComm.getProjectionCursor(appRole, "prefs_set", new Object[]{pathSegPrefKey, pathSegPrefVal, System.currentTimeMillis()});

			// guardian identity info

			} else if (RfcxComm.uriMatch(uri, appRole, "identity", "*")) {
				String idKey = uri.getLastPathSegment();
				return RfcxComm.getProjectionCursor(appRole, "identity", new Object[] { idKey, app.rfcxGuardianIdentity.getIdentityValue(idKey) });

			} else if (RfcxComm.uriMatch(uri, appRole, "identity_set", "*")) {
				String pathSeg = uri.getLastPathSegment();
				String pathSegIdKey = pathSeg.substring(0, pathSeg.indexOf("|"));
				String pathSegIdVal = pathSeg.substring(1 + pathSeg.indexOf("|"));
				app.rfcxGuardianIdentity.setIdentityValue(pathSegIdKey, pathSegIdVal);
				return RfcxComm.getProjectionCursor(appRole, "identity_set", new Object[]{pathSegIdKey, pathSegIdVal, System.currentTimeMillis()});

			// get status of services

			} else if (RfcxComm.uriMatch(uri, appRole, "status", "*")) {
				String statusTarget = uri.getLastPathSegment();
				JSONArray statusResultJsonArray = new JSONArray();
				if (statusTarget.equalsIgnoreCase("audio_capture")) {
					statusResultJsonArray = app.audioCaptureUtils.getAudioCaptureStatusAsJsonArray();
				}
				return RfcxComm.getProjectionCursor(appRole, "status", new Object[] { statusTarget, statusResultJsonArray.toString(), System.currentTimeMillis()});

			// "process" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "process", null)) {
				return RfcxComm.getProjectionCursor(appRole, "process", new Object[] { "org.rfcx.guardian."+appRole.toLowerCase(), AppProcessInfo.getAppProcessId(), AppProcessInfo.getAppUserId() });

			// "control" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "control", "kill")) {
				app.rfcxServiceHandler.stopAllServices();
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"kill", null, System.currentTimeMillis()});
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "GuardianContentProvider");
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
