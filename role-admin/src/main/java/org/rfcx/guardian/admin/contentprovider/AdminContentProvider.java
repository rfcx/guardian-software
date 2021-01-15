package org.rfcx.guardian.admin.contentprovider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.device.android.capture.LogcatCaptureService;
import org.rfcx.guardian.admin.device.android.capture.ScreenShotCaptureService;
import org.rfcx.guardian.admin.device.android.control.AirplaneModeEnableService;
import org.rfcx.guardian.admin.device.android.control.AirplaneModeToggleService;
import org.rfcx.guardian.admin.device.android.control.ClockSyncJobService;
import org.rfcx.guardian.admin.device.android.control.ForceRoleRelaunchService;
import org.rfcx.guardian.admin.device.android.control.RebootTriggerService;
import org.rfcx.guardian.admin.device.android.system.DeviceUtils;
import org.rfcx.guardian.admin.device.sentinel.SentinelUtils;
import org.rfcx.guardian.admin.sms.SmsUtils;
import org.rfcx.guardian.utility.device.AppProcessInfo;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.device.sentinel.SentinelPowerUtils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminContentProvider extends ContentProvider {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AdminContentProvider");

    private static final String appRole = RfcxGuardian.APP_ROLE;

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
        String logFuncVal = "";

        try {

            // get role "version" endpoints

            if (RfcxComm.uriMatch(uri, appRole, "version", null)) { logFuncVal = "version";
                return RfcxComm.getProjectionCursor(appRole, "version", new Object[]{appRole, RfcxRole.getRoleVersion(app.getApplicationContext(), logTag)});

                // "prefs" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "prefs", null)) { logFuncVal = "prefs";
                MatrixCursor cursor = RfcxComm.getProjectionCursor(appRole, "prefs", null);
                for (String prefKey : app.rfcxPrefs.listPrefsKeys()) {
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

            // "process" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "process", null)) { logFuncVal = "process";
                return RfcxComm.getProjectionCursor(appRole, "process", new Object[] { "org.rfcx.guardian."+appRole.toLowerCase(), AppProcessInfo.getAppProcessId(), AppProcessInfo.getAppUserId() });

            // get status of services

            } else if (RfcxComm.uriMatch(uri, appRole, "status", "*")) { logFuncVal = "status-*";
                String statusTarget = uri.getLastPathSegment();

                JSONArray statusArr = new JSONArray();
                try {
                    JSONObject statusObj = new JSONObject();
                    statusObj.put("audio_capture", new JSONObject( app.sentinelPowerUtils.sentinelPowerStatusAsJsonObjStr("audio_capture") ) );
                    statusObj.put("api_checkin", new JSONObject( app.sentinelPowerUtils.sentinelPowerStatusAsJsonObjStr("api_checkin") ) );
                    statusArr.put(statusObj);
                } catch (Exception e) {
                    RfcxLog.logExc(logTag, e, "AdminContentProvider - "+logFuncVal);
                }
                return RfcxComm.getProjectionCursor(appRole, "status", new Object[] { statusTarget, statusArr.toString(), System.currentTimeMillis()});

            // "control" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "kill")) { logFuncVal = "control-kill";
                app.rfcxServiceHandler.stopAllServices();
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"kill", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "initialize")) { logFuncVal = "control-initialize";
                app.initializeRoleServices();
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"initialize", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "reboot")) { logFuncVal = "control-reboot";
                app.rfcxServiceHandler.triggerService( RebootTriggerService.SERVICE_NAME, true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"reboot", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "relaunch")) { logFuncVal = "control-relaunch";
                app.rfcxServiceHandler.triggerIntentServiceImmediately( ForceRoleRelaunchService.SERVICE_NAME);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"relaunch", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "screenshot")) { logFuncVal = "control-screenshot";
                app.rfcxServiceHandler.triggerService( ScreenShotCaptureService.SERVICE_NAME, true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"screenshot", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "logcat")) { logFuncVal = "control-logcat";
                app.rfcxServiceHandler.triggerService( LogcatCaptureService.SERVICE_NAME, true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"logcat", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_toggle")) { logFuncVal = "control-airplanemode_toggle";
                app.rfcxServiceHandler.triggerService( AirplaneModeToggleService.SERVICE_NAME, true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"airplanemode_toggle", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_enable")) { logFuncVal = "control-airplanemode_enable";
                app.rfcxServiceHandler.triggerService( AirplaneModeEnableService.SERVICE_NAME, true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"airplanemode_enable", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "clock_sync")) { logFuncVal = "control-clock_sync";
                app.rfcxServiceHandler.triggerService( ClockSyncJobService.SERVICE_NAME, true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"clock_sync", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "asset_cleanup")) { logFuncVal = "control-asset_cleanup";
                app.assetUtils.runFileSystemAssetCleanup();
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"asset_cleanup", null, System.currentTimeMillis()});


            } else if (RfcxComm.uriMatch(uri, appRole, "gpio_set", "*")) { logFuncVal = "gpio_set-*";
                String pathSeg = uri.getLastPathSegment();
                String pathSegAddr = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegHighOrLow = pathSeg.substring(1+pathSeg.indexOf("|")).toLowerCase(Locale.US);
                app.deviceGPIO.runGPIOCommand("DOUT", pathSegAddr, pathSegHighOrLow.equalsIgnoreCase("high") );
                return RfcxComm.getProjectionCursor(appRole, "gpio_set", new Object[] { pathSegAddr, pathSegHighOrLow, System.currentTimeMillis() });


            } else if (RfcxComm.uriMatch(uri, appRole, "sms_queue", "*")) { logFuncVal = "sms_queue-*";
                String pathSeg = uri.getLastPathSegment();
                String pathSegSendAt = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegAfterSendAt = pathSeg.substring(pathSegSendAt.length()+1);
                String pathSegAddress = pathSegAfterSendAt.substring(0, pathSegAfterSendAt.indexOf("|"));
                String pathSegMessage = pathSegAfterSendAt.substring(1 + pathSegAfterSendAt.indexOf("|"));
                SmsUtils.addScheduledSmsToQueue(Long.parseLong(pathSegSendAt), pathSegAddress, pathSegMessage, app.getApplicationContext(), false);
                return RfcxComm.getProjectionCursor(appRole, "sms_queue", new Object[]{ pathSegSendAt+"|"+pathSegAddress+"|"+pathSegMessage, null, System.currentTimeMillis()});


            // get momentary values endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "get_momentary_values", "*")) { logFuncVal = "get_momentary_values-*";
                String pathSeg = uri.getLastPathSegment();

                if (pathSeg.equalsIgnoreCase("sentinel_power")) {
                    return RfcxComm.getProjectionCursor(appRole, "get_momentary_values", new Object[]{"sentinel_power", app.sentinelPowerUtils.getMomentarySentinelPowerValuesAsJsonArray().toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("sentinel_sensor")) {
                     return RfcxComm.getProjectionCursor(appRole, "get_momentary_values", new Object[]{"sentinel_sensor", SentinelUtils.getMomentarySentinelSensorValuesAsJsonArray(true, app.getApplicationContext()).toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("system_storage")) {
                    return RfcxComm.getProjectionCursor(appRole, "get_momentary_values", new Object[]{"system_storage", app.deviceUtils.getMomentaryConcatSystemMetaValuesAsJsonArray("storage").toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("system_memory")) {
                    return RfcxComm.getProjectionCursor(appRole, "get_momentary_values", new Object[]{"system_memory", app.deviceUtils.getMomentaryConcatSystemMetaValuesAsJsonArray("memory").toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("system_cpu")) {
                    return RfcxComm.getProjectionCursor(appRole, "get_momentary_values", new Object[]{"system_cpu", app.deviceUtils.getMomentaryConcatSystemMetaValuesAsJsonArray("cpu").toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("system_network")) {
                    return RfcxComm.getProjectionCursor(appRole, "get_momentary_values", new Object[]{"system_network", app.deviceUtils.getMomentaryConcatSystemMetaValuesAsJsonArray("network").toString(), System.currentTimeMillis()});


                } else {
                    return null;
                }


            // "database" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "database_get_all_rows", "*")) { logFuncVal = "database_get_all_rows-*";
                String pathSeg = uri.getLastPathSegment();

                if (pathSeg.equalsIgnoreCase("sms")) {
                    List<JSONArray> smsJsonArrays =  new ArrayList<JSONArray>();
                    smsJsonArrays.add(DeviceSmsUtils.getSmsMessagesFromSystemAsJsonArray(app.getResolver()));
                    smsJsonArrays.add(DeviceSmsUtils.formatSmsMessagesFromDatabaseAsJsonArray("received", app.smsMessageDb.dbSmsReceived.getAllRows()));
                    smsJsonArrays.add(DeviceSmsUtils.formatSmsMessagesFromDatabaseAsJsonArray("sent",app.smsMessageDb.dbSmsSent.getAllRows()));
                    return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[]{"sms", DeviceSmsUtils.combineSmsMessageJsonArrays(smsJsonArrays).toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("sentinel_power")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[]{"sentinel_power", SentinelPowerUtils.getSentinelPowerValuesAsJsonArray(app.getApplicationContext()).toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("sentinel_sensor")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[]{"sentinel_sensor", SentinelUtils.getSentinelSensorValuesAsJsonArray(app.getApplicationContext()).toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("system_meta")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[]{"system_meta", DeviceUtils.getSystemMetaValuesAsJsonArray(app.getApplicationContext()).toString(), System.currentTimeMillis()});

                } else {
                    return null;
                }

            } else if (RfcxComm.uriMatch(uri, appRole, "database_get_latest_row", "*")) { logFuncVal = "database_get_latest_row-*";
                String pathSeg = uri.getLastPathSegment();

                if (pathSeg.equalsIgnoreCase("screenshots")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[]{"screenshots", app.screenShotDb.dbCaptured.getLatestRowAsJsonArray().toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("logs")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[]{"logs", app.logcatDb.dbCaptured.getLatestRowAsJsonArray().toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("photos")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[]{"photos", app.cameraCaptureDb.dbPhotos.getLatestRowAsJsonArray().toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("videos")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[]{"videos", app.cameraCaptureDb.dbVideos.getLatestRowAsJsonArray().toString(), System.currentTimeMillis()});

                } else {
                    return null;
                }

            } else if (RfcxComm.uriMatch(uri, appRole, "database_delete_row", "*")) { logFuncVal = "database_delete_row-*";
                String pathSeg = uri.getLastPathSegment();
                String pathSegTable = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegId = pathSeg.substring(1 + pathSeg.indexOf("|"));

                if (pathSegTable.equalsIgnoreCase("sms")) {
                    int deleteFromSystem = DeviceSmsUtils.deleteSmsMessageFromSystem(pathSegId, app.getResolver());
                    int deleteFromReceivedDatabase = app.smsMessageDb.dbSmsReceived.deleteSingleRowByMessageId(pathSegId);
                    int deleteFromSentDatabase = app.smsMessageDb.dbSmsSent.deleteSingleRowByMessageId(pathSegId);
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[]{pathSeg, ( deleteFromSystem + deleteFromReceivedDatabase + deleteFromSentDatabase ), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("screenshots")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[]{pathSeg, app.screenShotDb.dbCaptured.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("logs")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[]{pathSeg, app.logcatDb.dbCaptured.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("photos")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[]{pathSeg, app.cameraCaptureDb.dbPhotos.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("videos")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[]{pathSeg, app.cameraCaptureDb.dbVideos.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis()});

                } else {
                    return null;
                }

            } else if (RfcxComm.uriMatch(uri, appRole, "database_delete_rows_before", "*")) { logFuncVal = "database_delete_rows_before-*";
                String pathSeg = uri.getLastPathSegment();
                String pathSegTable = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegTimeStamp = pathSeg.substring(1 + pathSeg.indexOf("|"));

                if (pathSegTable.equalsIgnoreCase("sentinel_power")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_rows_before", new Object[]{pathSeg, SentinelPowerUtils.deleteSentinelPowerValuesBeforeTimestamp(pathSegTimeStamp, app.getApplicationContext()), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("sentinel_sensor")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_rows_before", new Object[]{pathSeg, SentinelUtils.deleteSentinelSensorValuesBeforeTimestamp(pathSegTimeStamp, app.getApplicationContext()), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("system_meta")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_rows_before", new Object[]{pathSeg, DeviceUtils.deleteSystemMetaValuesBeforeTimestamp(pathSegTimeStamp, app.getApplicationContext()), System.currentTimeMillis()});

                }

            } else if (RfcxComm.uriMatch(uri, appRole, "database_set_last_accessed_at", "*")) { logFuncVal = "database_set_last_accessed_at-*";
                String pathSeg = uri.getLastPathSegment();
                String pathSegTable = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegId = pathSeg.substring(1 + pathSeg.indexOf("|"));

                if (pathSegTable.equalsIgnoreCase("screenshots")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_set_last_accessed_at", new Object[]{pathSeg, app.screenShotDb.dbCaptured.updateLastAccessedAtByTimestamp(pathSegId), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("logs")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_set_last_accessed_at", new Object[]{pathSeg, app.logcatDb.dbCaptured.updateLastAccessedAtByTimestamp(pathSegId), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("photos")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_set_last_accessed_at", new Object[]{pathSeg, app.cameraCaptureDb.dbPhotos.updateLastAccessedAtByTimestamp(pathSegId), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("videos")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_set_last_accessed_at", new Object[]{pathSeg, app.cameraCaptureDb.dbVideos.updateLastAccessedAtByTimestamp(pathSegId), System.currentTimeMillis()});

                } else {
                    return null;
                }

            } else if (RfcxComm.uriMatch(uri, appRole, "signal", "*")) { logFuncVal = "signal-*";
                return RfcxComm.getProjectionCursor(appRole, "signal", new Object[]{ app.deviceMobileNetwork.getSignalStrengthAsJsonArray() });
            } else if (RfcxComm.uriMatch(uri, appRole, "sentinel_values", "*")) { logFuncVal = "sentinel_values-*";
                return RfcxComm.getProjectionCursor(appRole, "sentinel_values", new Object[]{ new JSONArray().put(app.sentinelPowerUtils.getMomentarySentinelPowerValuesAsJson())});
            }


            return null;

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e, "AdminContentProvider - "+logFuncVal);
        }
        return null;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        return RfcxComm.serveAssetFileRequest(uri, mode, getContext(), RfcxGuardian.APP_ROLE, logTag);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
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
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

}
