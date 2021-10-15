package org.rfcx.guardian.classify.contentprovider;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.classify.service.AudioClassifyJobService;
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
import android.text.TextUtils;
import android.util.Log;

import java.util.Objects;

public class ClassifyContentProvider extends ContentProvider {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ClassifyContentProvider");

    private static final String appRole = RfcxGuardian.APP_ROLE;

    @Override
    public Cursor query(@NotNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String logFuncVal = "";

        try {

            RfcxGuardian app = (RfcxGuardian) Objects.requireNonNull(getContext()).getApplicationContext();

            // get role "version" endpoints

            if (RfcxComm.uriMatch(uri, appRole, "version", null)) {
                logFuncVal = "version";
                return RfcxComm.getProjectionCursor(appRole, "version", new Object[]{appRole, RfcxRole.getRoleVersion(app.getApplicationContext(), logTag)});

                // "prefs" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "prefs", null)) {
                logFuncVal = "prefs";
                MatrixCursor cursor = RfcxComm.getProjectionCursor(appRole, "prefs", null);
                for (String prefKey : RfcxPrefs.listPrefsKeys()) {
                    cursor.addRow(new Object[]{prefKey, app.rfcxPrefs.getPrefAsString(prefKey)});
                }
                return cursor;

            } else if (RfcxComm.uriMatch(uri, appRole, "prefs", "*")) {
                logFuncVal = "prefs-*";
                String prefKey = uri.getLastPathSegment();
                return RfcxComm.getProjectionCursor(appRole, "prefs", new Object[]{prefKey, app.rfcxPrefs.getPrefAsString(prefKey)});

            } else if (RfcxComm.uriMatch(uri, appRole, "prefs_resync", "*")) {
                logFuncVal = "prefs_resync-*";
                String prefKey = uri.getLastPathSegment();
                app.rfcxPrefs.reSyncPrefs(prefKey);
                app.onPrefReSync(prefKey);
                return RfcxComm.getProjectionCursor(appRole, "prefs_resync", new Object[]{prefKey, System.currentTimeMillis()});

                // guardian identity endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "identity_resync", "*")) {
                logFuncVal = "identity_resync-*";
                String idKey = uri.getLastPathSegment();
                app.rfcxGuardianIdentity.reSyncGuardianIdentity();
                return RfcxComm.getProjectionCursor(appRole, "identity_resync", new Object[]{idKey, System.currentTimeMillis()});

                // get status of services

            } else if (RfcxComm.uriMatch(uri, appRole, "status", "*")) {
                logFuncVal = "status-*";
                String statusTarget = uri.getLastPathSegment();
                JSONArray statusArr = app.rfcxStatus.getCompositeLocalStatusAsJsonArr();
                return RfcxComm.getProjectionCursor(appRole, "status", new Object[]{statusTarget, statusArr.toString(), System.currentTimeMillis()});

                // "process" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "process", null)) {
                logFuncVal = "process";
                return RfcxComm.getProjectionCursor(appRole, "process", new Object[]{"org.rfcx.guardian." + appRole.toLowerCase(), AppProcessInfo.getAppProcessId(), AppProcessInfo.getAppUserId()});

                // "control" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "kill")) {
                logFuncVal = "control-kill";
                app.rfcxSvc.stopAllServices();
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"kill", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "initialize")) {
                logFuncVal = "control-initialize";
                app.initializeRoleServices();
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"initialize", null, System.currentTimeMillis()});


            } else if (RfcxComm.uriMatch(uri, appRole, "classify_queue", "*")) {
                logFuncVal = "classify_queue-*";
                String pathSeg = uri.getLastPathSegment();

                String[] clsfyJob = TextUtils.split(pathSeg, "\\|");

                String audioId = clsfyJob[0];
                String clsfrId = clsfyJob[1];
                String clsfrVer = clsfyJob[2];
                int sampleRate = Integer.parseInt(clsfyJob[3]);
                String audioFile = clsfyJob[4];
                String clsfrFile = clsfyJob[5];
                String windowSize = clsfyJob[6];
                String stepSize = clsfyJob[7];
                String classes = clsfyJob[8];

                app.audioClassifyDb.dbQueued.insert(audioId, clsfrId, clsfrVer, 0, sampleRate, 0, audioFile, clsfrFile, windowSize, stepSize, classes);

                Log.d(logTag, "Audio Classify Job added to Queue"
                        + " - Audio: " + audioId + " - Classifier: " + clsfrId + ", "
                        + "v" + clsfrVer + ", " + classes + ", "
                        + Math.round((double) sampleRate / 1000) + "kHz, "
                        + Float.parseFloat(windowSize) + ", " + Float.parseFloat(stepSize));

                app.rfcxSvc.triggerService(AudioClassifyJobService.SERVICE_NAME, false);

                return RfcxComm.getProjectionCursor(appRole, "classify_queue", new Object[]{audioId + "|" + clsfrId, null, System.currentTimeMillis()});

            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e, "ClassifyContentProvider - " + logFuncVal);
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
