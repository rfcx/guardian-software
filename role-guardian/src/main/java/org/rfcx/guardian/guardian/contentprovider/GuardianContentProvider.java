package org.rfcx.guardian.guardian.contentprovider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.utility.device.AppProcessInfo;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;

public class GuardianContentProvider extends ContentProvider {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "GuardianContentProvider");

	private static final String appRole = RfcxGuardian.APP_ROLE;

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		String logFuncVal = "";
		
		try {

			// get role "version" endpoints

			if (RfcxComm.uriMatch(uri, appRole, "version", null)) { logFuncVal = "version";
				return RfcxComm.getProjectionCursor(appRole, "version", new Object[] { appRole, RfcxRole.getRoleVersion(app.getApplicationContext(), logTag) });

			// "prefs" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", null)) { logFuncVal = "prefs";
				MatrixCursor cursor = RfcxComm.getProjectionCursor(appRole, "prefs", null);
				for (String prefKey : app.rfcxPrefs.listPrefsKeys()) {
					cursor.addRow(new Object[] { prefKey, app.rfcxPrefs.getPrefAsString(prefKey) });
				}
				return cursor;

			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", "*")) { logFuncVal = "prefs-*";
				String prefKey = uri.getLastPathSegment();
				return RfcxComm.getProjectionCursor(appRole, "prefs", new Object[] { prefKey, app.rfcxPrefs.getPrefAsString(prefKey) });

			} else if (RfcxComm.uriMatch(uri, appRole, "prefs_set", "*")) { logFuncVal = "prefs_set-*";
				String pathSeg = uri.getLastPathSegment();
				String pathSegPrefKey = pathSeg.substring(0, pathSeg.indexOf("|"));
				String pathSegPrefVal = pathSeg.substring(1 + pathSeg.indexOf("|"));
				app.setSharedPref(pathSegPrefKey, pathSegPrefVal);
				return RfcxComm.getProjectionCursor(appRole, "prefs_set", new Object[]{pathSegPrefKey, pathSegPrefVal, app.wifiCommunicationUtils.getPrefsChangesAsJson(), System.currentTimeMillis()});

			// guardian identity info

			} else if (RfcxComm.uriMatch(uri, appRole, "identity", "*")) { logFuncVal = "identity-*";
				String idKey = uri.getLastPathSegment();
				return RfcxComm.getProjectionCursor(appRole, "identity", new Object[] { idKey, app.rfcxGuardianIdentity.getIdentityValue(idKey) });

			} else if (RfcxComm.uriMatch(uri, appRole, "identity_set", "*")) { logFuncVal = "identity_set-*";
				String pathSeg = uri.getLastPathSegment();
				String pathSegIdKey = pathSeg.substring(0, pathSeg.indexOf("|"));
				String pathSegIdVal = pathSeg.substring(1 + pathSeg.indexOf("|"));
				app.rfcxGuardianIdentity.setIdentityValue(pathSegIdKey, pathSegIdVal);
				return RfcxComm.getProjectionCursor(appRole, "identity_set", new Object[]{pathSegIdKey, pathSegIdVal, System.currentTimeMillis()});

			// get status of services

			} else if (RfcxComm.uriMatch(uri, appRole, "status", "*")) { logFuncVal = "status-*";
				String statusTarget = uri.getLastPathSegment();

				JSONArray statusArr = new JSONArray();
				try {
					JSONObject statusObj = new JSONObject();
					JSONObject statusAudioCapture = app.audioCaptureUtils.audioCaptureStatusAsJsonObj();
					if (statusAudioCapture != null) { statusObj.put("audio_capture", statusAudioCapture); }
					JSONObject statusApiCheckIn = app.apiCheckInHealthUtils.apiCheckInStatusAsJsonObj();
					if (statusApiCheckIn != null) { statusObj.put("api_checkin", statusApiCheckIn); }
					statusArr.put(statusObj);
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e, "GuardianContentProvider - "+logFuncVal);
				}

				return RfcxComm.getProjectionCursor(appRole, "status", new Object[] { statusTarget, statusArr.toString(), System.currentTimeMillis()});

			// "process" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "process", null)) { logFuncVal = "process";
				return RfcxComm.getProjectionCursor(appRole, "process", new Object[] { "org.rfcx.guardian."+appRole.toLowerCase(), AppProcessInfo.getAppProcessId(), AppProcessInfo.getAppUserId() });

			// "send ping" function

			} else if (RfcxComm.uriMatch(uri, appRole, "ping", "*")) { logFuncVal = "ping-*";
				String pingField = uri.getLastPathSegment();
				app.apiCheckInUtils.sendMqttPing(pingField.equalsIgnoreCase("all"), new String[]{ pingField } );
				return RfcxComm.getProjectionCursor(appRole, "ping", new Object[] { System.currentTimeMillis() });

			// "control" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "control", "kill")) { logFuncVal = "control-kill";
				app.rfcxServiceHandler.stopAllServices();
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"kill", null, System.currentTimeMillis()});

			} else if (RfcxComm.uriMatch(uri, appRole, "control", "initialize")) { logFuncVal = "control-initialize";
				app.initializeRoleServices();
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"initialize", null, System.currentTimeMillis()});

			// "instructions" endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "instructions", "*")) { logFuncVal = "instructions-*";
				JSONObject instrObj = new JSONObject(uri.getLastPathSegment());
				app.instructionsUtils.processReceivedInstructionJson(instrObj);
				return RfcxComm.getProjectionCursor(appRole, "instructions", new Object[]{ instrObj.toString(), System.currentTimeMillis() });

			// "get configuration" function

			} else if (RfcxComm.uriMatch(uri, appRole, "configuration", "*")) {
				logFuncVal = "configuration-*";
				String configurationTarget = uri.getLastPathSegment();
				JSONArray configurationResultJsonArray = new JSONArray();
				if (configurationTarget.equalsIgnoreCase("configuration")) {
					configurationResultJsonArray = app.wifiCommunicationUtils.getCurrentConfigurationAsJson();
				}
				return RfcxComm.getProjectionCursor(appRole, "configuration", new Object[]{configurationTarget, configurationResultJsonArray.toString(), System.currentTimeMillis() });

			// "get audio buffer" function

			} else if (RfcxComm.uriMatch(uri, appRole, "microphone_test", "*")) {
				logFuncVal = "microphone_test-*";
				String microphoneTarget = uri.getLastPathSegment();
				JSONArray microphoneResultJsonArray = new JSONArray();
				if (microphoneTarget.equalsIgnoreCase("microphone_test")) {
					microphoneResultJsonArray = app.wifiCommunicationUtils.getAudioBufferAsJson();
				}
				return RfcxComm.getProjectionCursor(appRole, "microphone_test", new Object[]{microphoneTarget, microphoneResultJsonArray.toString(), System.currentTimeMillis() });

			// "get diagnostic" function

			} else if (RfcxComm.uriMatch(uri, appRole, "diagnostic", "*")) {
				logFuncVal = "diagnostic-*";
				String diagnosticTarget = uri.getLastPathSegment();
				JSONArray diagnosticResultJsonArray = new JSONArray();
				if (diagnosticTarget.equalsIgnoreCase("diagnostic")) {
					diagnosticResultJsonArray = app.wifiCommunicationUtils.getDiagnosticAsJson();
				}
				return RfcxComm.getProjectionCursor(appRole, "diagnostic", new Object[]{diagnosticTarget, diagnosticResultJsonArray.toString(), System.currentTimeMillis()});
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "GuardianContentProvider - "+logFuncVal);
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
