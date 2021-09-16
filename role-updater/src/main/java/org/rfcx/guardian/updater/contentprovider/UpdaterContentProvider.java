package org.rfcx.guardian.updater.contentprovider;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.device.AppProcessInfo;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.util.Objects;

public class UpdaterContentProvider extends ContentProvider {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "UpdaterContentProvider");

	private static final String appRole = RfcxGuardian.APP_ROLE;

	@Override
	public Cursor query(@NotNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		String logFuncVal = "";

		try {

			RfcxGuardian app = (RfcxGuardian) Objects.requireNonNull(getContext()).getApplicationContext();

			// get role "version" endpoints

			if (RfcxComm.uriMatch(uri, appRole, "version", null)) { logFuncVal = "version";
				return RfcxComm.getProjectionCursor(appRole, "version", new Object[]{appRole, RfcxRole.getRoleVersion(app.getApplicationContext(), logTag)});

				// "prefs" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", null)) { logFuncVal = "prefs";
				MatrixCursor cursor = RfcxComm.getProjectionCursor(appRole, "prefs", null);
				for (String prefKey : RfcxPrefs.listPrefsKeys()) {
					cursor.addRow(new Object[]{prefKey, app.rfcxPrefs.getPrefAsString(prefKey)});
				}
				return cursor;

			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", "*")) { logFuncVal = "prefs-*";
				String prefKey = uri.getLastPathSegment();
				return RfcxComm.getProjectionCursor(appRole, "prefs", new Object[]{prefKey, app.rfcxPrefs.getPrefAsString(prefKey)});

			} else if (RfcxComm.uriMatch(uri, appRole, "prefs_resync", "*")) { logFuncVal = "prefs_resync-*";
				String prefKey = uri.getLastPathSegment();
				app.rfcxPrefs.reSyncPrefs(prefKey);
                app.onPrefReSync(prefKey);
				return RfcxComm.getProjectionCursor(appRole, "prefs_resync", new Object[]{ prefKey, System.currentTimeMillis() });

				// guardian identity endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "identity_resync", "*")) { logFuncVal = "identity_resync-*";
				String idKey = uri.getLastPathSegment();
				app.rfcxGuardianIdentity.reSyncGuardianIdentity();
				return RfcxComm.getProjectionCursor(appRole, "identity_resync", new Object[]{ idKey, System.currentTimeMillis() });

				// get status of services

			} else if (RfcxComm.uriMatch(uri, appRole, "status", "*")) { logFuncVal = "status-*";
				String statusTarget = uri.getLastPathSegment();
				JSONArray statusArr = app.rfcxStatus.getCompositeLocalStatusAsJsonArr();
				return RfcxComm.getProjectionCursor(appRole, "status", new Object[] { statusTarget, statusArr.toString(), System.currentTimeMillis()});

				// "process" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "process", null)) { logFuncVal = "process";
				return RfcxComm.getProjectionCursor(appRole, "process", new Object[] { "org.rfcx.guardian."+appRole.toLowerCase(), AppProcessInfo.getAppProcessId(), AppProcessInfo.getAppUserId() });

				// "control" function endpoints

			} else if (RfcxComm.uriMatch(uri, appRole, "control", "kill")) { logFuncVal = "control-kill";
				app.rfcxSvc.stopAllServices();
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{ "kill", null, System.currentTimeMillis()});

			} else if (RfcxComm.uriMatch(uri, appRole, "control", "initialize")) { logFuncVal = "control-initialize";
				app.initializeRoleServices();
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"initialize", null, System.currentTimeMillis()});

			} else if (RfcxComm.uriMatch(uri, appRole, "control", "software_update")) {  logFuncVal = "control-software_update";
				app.apiUpdateRequestUtils.attemptToTriggerUpdateRequest(true, true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{ "software_update", null, System.currentTimeMillis()});

			} else if (RfcxComm.uriMatch(uri, appRole, "software_install_companion", "*")) {  logFuncVal = "software_install_companion";
				String installJsonString = uri.getLastPathSegment();
				app.installUtils.installFromContentProvider(installJsonString);
				return RfcxComm.getProjectionCursor(appRole, "software_install_companion", new Object[]{ null });

			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "UpdaterContentProvider - "+logFuncVal);
		}
		return null;
	}

	public ParcelFileDescriptor openFile(@NotNull Uri uri, @NotNull String mode) {
		return RfcxComm.serveAssetFileRequest(uri, mode, getContext(), RfcxGuardian.APP_ROLE, logTag);
	}

	@Override
	public int delete(@NotNull Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int update(@NotNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(@NotNull Uri uri) {
		return null;
	}

	@Override
	public Uri insert(@NotNull Uri uri, ContentValues values) {
		return null;
	}

}
