package org.rfcx.guardian.admin.contentprovider;

import org.json.JSONArray;
import org.rfcx.guardian.admin.device.android.system.DeviceUtils;
import org.rfcx.guardian.admin.sms.SmsScheduler;
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

import java.util.ArrayList;
import java.util.List;

public class AdminContentProvider extends ContentProvider {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AdminContentProvider.class);

    private static final String appRole = RfcxGuardian.APP_ROLE;

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();

        try {

            // get role "version" endpoints

            if (RfcxComm.uriMatch(uri, appRole, "version", null)) {
                return RfcxComm.getProjectionCursor(appRole, "version", new Object[]{appRole, RfcxRole.getRoleVersion(app.getApplicationContext(), logTag)});

                // "prefs" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "prefs", null)) {
                MatrixCursor cursor = RfcxComm.getProjectionCursor(appRole, "prefs", null);
                for (String prefKey : app.rfcxPrefs.listPrefsKeys()) {
                    cursor.addRow(new Object[]{prefKey, app.rfcxPrefs.getPrefAsString(prefKey)});
                }
                return cursor;

            } else if (RfcxComm.uriMatch(uri, appRole, "prefs", "*")) {
                String prefKey = uri.getLastPathSegment();
                return RfcxComm.getProjectionCursor(appRole, "prefs", new Object[]{prefKey, app.rfcxPrefs.getPrefAsString(prefKey)});

            } else if (RfcxComm.uriMatch(uri, appRole, "prefs_resync", "*")) {
                String prefKey = uri.getLastPathSegment();
                app.rfcxPrefs.reSyncPref(prefKey);
                String prefValue = app.onPrefReSync(prefKey);
                return RfcxComm.getProjectionCursor(appRole, "prefs_resync", new Object[]{prefKey, prefValue, System.currentTimeMillis()});

            // "process" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "process", null)) {
                return RfcxComm.getProjectionCursor(appRole, "process", new Object[] { "org.rfcx.guardian."+appRole, AppProcessInfo.getAppProcessId(), AppProcessInfo.getAppUserId() });

            // "control" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "kill")) {
                app.rfcxServiceHandler.stopAllServices();
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"kill", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "reboot")) {
                app.rfcxServiceHandler.triggerService("RebootTrigger", true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"reboot", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "relaunch")) {
                app.rfcxServiceHandler.triggerIntentServiceImmediately("ForceRoleRelaunch");
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"relaunch", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "screenshot")) {
                app.rfcxServiceHandler.triggerService("ScreenShotCapture", true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"screenshot", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "logcat")) {
                app.rfcxServiceHandler.triggerService("LogCatCapture", true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"logcat", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_toggle")) {
                app.rfcxServiceHandler.triggerService("AirplaneModeToggle", true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"airplanemode_toggle", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_enable")) {
                app.rfcxServiceHandler.triggerService("AirplaneModeEnable", true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"airplanemode_enable", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "control", "datetime_sntp_sync")) {
                app.rfcxServiceHandler.triggerService("DateTimeSntpSyncJob", true);
                return RfcxComm.getProjectionCursor(appRole, "control", new Object[]{"datetime_sntp_sync", null, System.currentTimeMillis()});

            } else if (RfcxComm.uriMatch(uri, appRole, "sms_send", "*")) {
                String pathSeg = uri.getLastPathSegment();
                String pathSegAddress = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegMessage = pathSeg.substring(1 + pathSeg.indexOf("|"));
                SmsScheduler.addImmediateSmsToQueue(pathSegAddress, pathSegMessage, app.getApplicationContext());
                return RfcxComm.getProjectionCursor(appRole, "sms_send", new Object[]{pathSegAddress + "|" + pathSegMessage, null, System.currentTimeMillis()});

                // "database" function endpoints

            } else if (RfcxComm.uriMatch(uri, appRole, "database_get_all_rows", "*")) {
                String pathSeg = uri.getLastPathSegment();

                if (pathSeg.equalsIgnoreCase("sms")) {
                    List<JSONArray> smsJsonArrays =  new ArrayList<JSONArray>();
                    smsJsonArrays.add(DeviceSmsUtils.getSmsMessagesFromSystemAsJsonArray(app.getApplicationContext().getContentResolver()));
                    smsJsonArrays.add(DeviceSmsUtils.formatSmsMessagesFromDatabaseAsJsonArray("received", app.smsMessageDb.dbSmsReceived.getAllRows()));
                    smsJsonArrays.add(DeviceSmsUtils.formatSmsMessagesFromDatabaseAsJsonArray("sent",app.smsMessageDb.dbSmsSent.getAllRows()));
                    return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[]{"sms", DeviceSmsUtils.combineSmsMessageJsonArrays(smsJsonArrays).toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("sentinel_power")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[]{"sentinel_power", SentinelPowerUtils.getSentinelPowerValuesAsJsonArray(app.getApplicationContext()).toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("system_meta")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_all_rows", new Object[]{"system_meta", DeviceUtils.getSystemMetaValuesAsJsonArray(app.getApplicationContext()).toString(), System.currentTimeMillis()});

                } else {
                    return null;
                }

            } else if (RfcxComm.uriMatch(uri, appRole, "database_get_latest_row", "*")) {
                String pathSeg = uri.getLastPathSegment();

                if (pathSeg.equalsIgnoreCase("screenshots")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[]{"screenshots", app.deviceScreenShotDb.dbCaptured.getLatestRowAsJsonArray().toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("logs")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[]{"logs", app.deviceLogCatDb.dbCaptured.getLatestRowAsJsonArray().toString(), System.currentTimeMillis()});

                } else if (pathSeg.equalsIgnoreCase("sentinel_power")) {
					return RfcxComm.getProjectionCursor(appRole, "database_get_latest_row", new Object[]{"sentinel_power", app.sentinelPowerDb.dbSentinelPowerBattery.getLatestRowAsJsonArray().toString(), System.currentTimeMillis()});
                } else {
                    return null;
                }

            } else if (RfcxComm.uriMatch(uri, appRole, "database_delete_row", "*")) {
                String pathSeg = uri.getLastPathSegment();
                String pathSegTable = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegId = pathSeg.substring(1 + pathSeg.indexOf("|"));

                if (pathSegTable.equalsIgnoreCase("sms")) {
                    int deleteFromSystem = DeviceSmsUtils.deleteSmsMessageFromSystem(pathSegId, app.getApplicationContext().getContentResolver());
                    int deleteFromReceivedDatabase = app.smsMessageDb.dbSmsReceived.deleteSingleRowByMessageId(pathSegId);
                    int deleteFromSentDatabase = app.smsMessageDb.dbSmsSent.deleteSingleRowByMessageId(pathSegId);
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[]{pathSeg, ( deleteFromSystem + deleteFromReceivedDatabase + deleteFromSentDatabase ), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("screenshots")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[]{pathSeg, app.deviceScreenShotDb.dbCaptured.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("logs")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_row", new Object[]{pathSeg, app.deviceLogCatDb.dbCaptured.deleteSingleRowByTimestamp(pathSegId), System.currentTimeMillis()});

                } else {
                    return null;
                }

            } else if (RfcxComm.uriMatch(uri, appRole, "database_delete_rows_before", "*")) {
                String pathSeg = uri.getLastPathSegment();
                String pathSegTable = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegTimeStamp = pathSeg.substring(1 + pathSeg.indexOf("|"));

                if (pathSegTable.equalsIgnoreCase("sentinel_power")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_rows_before", new Object[]{pathSeg, SentinelPowerUtils.deleteSentinelPowerValuesBeforeTimestamp(pathSegTimeStamp, app.getApplicationContext()), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("system_meta")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_delete_rows_before", new Object[]{pathSeg, DeviceUtils.deleteSystemMetaValuesBeforeTimestamp(pathSegTimeStamp, app.getApplicationContext()), System.currentTimeMillis()});

                }

            } else if (RfcxComm.uriMatch(uri, appRole, "database_set_last_accessed_at", "*")) {
                String pathSeg = uri.getLastPathSegment();
                String pathSegTable = pathSeg.substring(0, pathSeg.indexOf("|"));
                String pathSegId = pathSeg.substring(1 + pathSeg.indexOf("|"));

                if (pathSegTable.equalsIgnoreCase("screenshots")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_set_last_accessed_at", new Object[]{pathSeg, app.deviceScreenShotDb.dbCaptured.updateLastAccessedAtByTimestamp(pathSegId), System.currentTimeMillis()});

                } else if (pathSegTable.equalsIgnoreCase("logs")) {
                    return RfcxComm.getProjectionCursor(appRole, "database_set_last_accessed_at", new Object[]{pathSeg, app.deviceLogCatDb.dbCaptured.updateLastAccessedAtByTimestamp(pathSegId), System.currentTimeMillis()});

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
