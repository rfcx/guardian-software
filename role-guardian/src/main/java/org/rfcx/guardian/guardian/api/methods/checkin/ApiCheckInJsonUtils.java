package org.rfcx.guardian.guardian.api.methods.checkin;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ApiCheckInJsonUtils {

	public ApiCheckInJsonUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInJsonUtils");

	private RfcxGuardian app;


	public String buildCheckInJson(String checkInJsonString, String[] screenShotMeta, String[] logFileMeta, String[] photoFileMeta, String[] videoFileMeta) throws JSONException, IOException {

		JSONObject checkInMetaJson = retrieveAndBundleMetaJson(app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT), false);

		// Adding Audio JSON fields from checkin table
		JSONObject checkInJsonObj = new JSONObject(checkInJsonString);
		checkInMetaJson.put("queued_at", checkInJsonObj.getLong("queued_at"));
		checkInMetaJson.put("audio", checkInJsonObj.getString("audio"));

		// Recording number of currently queued/skipped/stashed checkins
		checkInMetaJson.put("checkins", getCheckInStatusInfoForJson( new String[] { "sent" } ));

		checkInMetaJson.put("purged", app.assetUtils.getAssetExchangeLogList("purged", 4 * app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT)));

		// Adding software role versions
		checkInMetaJson.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));

		// Adding checksum of current prefs values
		checkInMetaJson.put("prefs", app.apiCheckInJsonUtils.buildCheckInPrefsJsonObj(false));

		// Adding instructions, if there are any
		if (app.instructionsUtils.getInstructionsCount() > 0) {
			checkInMetaJson.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
		}

		// Adding messages to JSON blob
		JSONArray smsArr = RfcxComm.getQuery("admin", "database_get_all_rows", "sms", app.getResolver());
		if (smsArr.length() > 0) { checkInMetaJson.put("messages", smsArr); }

		// Adding screenshot meta to JSON blob
		if (screenShotMeta[0] != null) {
			checkInMetaJson.put("screenshots", TextUtils.join("*", new String[]{screenShotMeta[1], screenShotMeta[2], screenShotMeta[3], screenShotMeta[4], screenShotMeta[5], screenShotMeta[6]}));
		}

		// Adding logs meta to JSON blob
		if (logFileMeta[0] != null) {
			checkInMetaJson.put("logs", TextUtils.join("*", new String[]{logFileMeta[1], logFileMeta[2], logFileMeta[3], logFileMeta[4]}));
		}

		// Adding photos meta to JSON blob
		if (photoFileMeta[0] != null) {
			checkInMetaJson.put("photos", TextUtils.join("*", new String[]{photoFileMeta[1], photoFileMeta[2], photoFileMeta[3], photoFileMeta[4], photoFileMeta[5], photoFileMeta[6]}));
		}

		// Adding videos meta to JSON blob
		if (videoFileMeta[0] != null) {
			checkInMetaJson.put("videos", TextUtils.join("*", new String[]{videoFileMeta[1], videoFileMeta[2], videoFileMeta[3], videoFileMeta[4], videoFileMeta[5], videoFileMeta[6]}));
		}

		int limitLogsTo = 1500;
		String strLogs = checkInMetaJson.toString();
		Log.d(logTag, (strLogs.length() <= limitLogsTo) ? strLogs : strLogs.substring(0, limitLogsTo) + "...");

		// Adding Guardian GUID and Auth Token
		JSONObject guardianObj = new JSONObject();
		guardianObj.put("guid", this.app.rfcxGuardianIdentity.getGuid());
		guardianObj.put("token", this.app.rfcxGuardianIdentity.getAuthToken());
		checkInMetaJson.put("guardian", guardianObj);

		return checkInMetaJson.toString();

	}



	public String getCheckInStatusInfoForJson(String[] includeAssetIdLists) {

		String[] types = new String[] { "sent", "queued", "meta", "skipped", "stashed", "archived", "vault" };

		int idBundleLimit = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT);
		long includeAssetIdIfOlderThan = 4 * this.app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) * 1000;

		List<String> typeStatuses = new ArrayList<>();

		for (String type : types) {

			StringBuilder statusStr = (new StringBuilder()).append(type);
			List<String[]> idRows = new ArrayList<>();
			boolean includeIdList = ArrayUtils.doesStringArrayContainString(includeAssetIdLists, type);

			if (type.equalsIgnoreCase("sent")) {
				statusStr.append("*").append(app.apiCheckInDb.dbSent.getCount());
				statusStr.append("*").append(app.apiCheckInDb.dbSent.getCumulativeFileSizeForAllRows());
				if (includeIdList) { idRows = app.apiCheckInDb.dbSent.getLatestRowsWithLimit(idBundleLimit); }

			} else if (type.equalsIgnoreCase("queued")) {
				statusStr.append("*").append(app.apiCheckInDb.dbQueued.getCount());
				statusStr.append("*").append(app.apiCheckInDb.dbQueued.getCumulativeFileSizeForAllRows());
				if (includeIdList) { idRows = app.apiCheckInDb.dbQueued.getLatestRowsWithLimit(idBundleLimit); }

			} else if (type.equalsIgnoreCase("meta")) {
				statusStr.append("*").append(app.metaDb.dbMeta.getCount());
				statusStr.append("*").append(app.metaDb.dbMeta.getCumulativeJsonBlobLengthForAllRows());
				if (includeIdList) { idRows = app.metaDb.dbMeta.getLatestRowsWithLimit(idBundleLimit); }

			} else if (type.equalsIgnoreCase("skipped")) {
				statusStr.append("*").append(app.apiCheckInDb.dbSkipped.getCount());
				statusStr.append("*").append(app.apiCheckInDb.dbSkipped.getCumulativeFileSizeForAllRows());
				if (includeIdList) { idRows = app.apiCheckInDb.dbSkipped.getLatestRowsWithLimit(idBundleLimit); }

			} else if (type.equalsIgnoreCase("stashed")) {
				statusStr.append("*").append(app.apiCheckInDb.dbStashed.getCount());
				statusStr.append("*").append(app.apiCheckInDb.dbStashed.getCumulativeFileSizeForAllRows());
				if (includeIdList) { idRows = app.apiCheckInDb.dbStashed.getLatestRowsWithLimit(idBundleLimit); }

			} else if (type.equalsIgnoreCase("archived")) {
				statusStr.append("*").append(app.apiCheckInArchiveDb.dbArchive.getInnerRecordCumulativeCount());
				statusStr.append("*").append(app.apiCheckInArchiveDb.dbArchive.getCumulativeFileSizeForAllRows());

			} else if (type.equalsIgnoreCase("vault")) {
				statusStr.append("*").append(app.audioVaultDb.dbVault.getCumulativeRecordCountForAllRows());
				statusStr.append("*").append(app.audioVaultDb.dbVault.getCumulativeFileSizeForAllRows());

			}

			for (String[] idRow : idRows) {
				long assetId = Long.parseLong(idRow[1].substring(0, idRow[1].lastIndexOf(".")));
				if (includeAssetIdIfOlderThan < Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(assetId))) {
					statusStr.append("*").append(assetId);
				}
			}

			typeStatuses.add(statusStr.toString());
		}

		return TextUtils.join("|", typeStatuses );
	}


	public void createSystemMetaDataJsonSnapshot() throws JSONException {

		JSONObject metaDataJsonObj = new JSONObject();
		Date metaQueryTimestampObj = new Date();
		long metaQueryTimestamp = metaQueryTimestampObj.getTime();

		JSONArray metaIds = new JSONArray();
		metaIds.put(metaQueryTimestamp);
		metaDataJsonObj.put("meta_ids", metaIds);
		metaDataJsonObj.put("measured_at", metaQueryTimestamp);

		metaDataJsonObj.put("broker_connections", app.deviceSystemDb.dbMqttBroker.getConcatRows());

		// Adding connection data from previous checkins
		metaDataJsonObj.put("previous_checkins", app.latencyStatsDb.dbCheckInLatency.getConcatRows());

		// Adding latency data from previous classify jobs
		metaDataJsonObj.put("previous_classify", app.latencyStatsDb.dbClassifyLatency.getConcatRows());

		metaDataJsonObj.put("detections", app.audioDetectionDb.dbFiltered.getSimplifiedConcatRows());

		// Adding system metadata, if they can be retrieved from admin role via content provider
		JSONArray systemMetaJsonArray = RfcxComm.getQuery("admin", "database_get_all_rows",
				"system_meta", app.getResolver());
		metaDataJsonObj = addConcatSystemMetaParams(metaDataJsonObj, systemMetaJsonArray);

		// Adding sentinel power data, if they can be retrieved from admin role via content provider
		String sentinelPower = getConcatMetaField(RfcxComm.getQuery("admin", "database_get_all_rows",
				"sentinel_power", app.getResolver()));
		if (sentinelPower.length() > 0) { metaDataJsonObj.put("sentinel_power", sentinelPower); }

		// Adding sentinel sensor data, if they can be retrieved from admin role via content provider
		String sentinelSensor = getConcatMetaField(RfcxComm.getQuery("admin", "database_get_all_rows",
				"sentinel_sensor", app.getResolver()));
		if (sentinelSensor.length() > 0) { metaDataJsonObj.put("sentinel_sensor", sentinelSensor); }

		ArrayList<String> dateTimeOffsets = new ArrayList<String>();
		if (metaDataJsonObj.has("datetime_offsets")) { dateTimeOffsets.add(metaDataJsonObj.getString("datetime_offsets")); }
		if (app.deviceSystemDb.dbDateTimeOffsets.getCount() > 0) { dateTimeOffsets.add(app.deviceSystemDb.dbDateTimeOffsets.getConcatRows()); }
		if (dateTimeOffsets.size() > 0) { metaDataJsonObj.put("datetime_offsets", TextUtils.join("|", dateTimeOffsets)); }

		String metaDataJsonStr = metaDataJsonObj.toString();

		// Saves JSON snapshot blob to database
		app.metaDb.dbMeta.insert(metaQueryTimestamp, metaDataJsonStr);

		clearPrePackageMetaData(metaQueryTimestampObj);

		int metaQueueFullRecordCount = app.metaDb.dbMeta.getCount();
		long metaQueueFullByteLength = app.metaDb.dbMeta.getCumulativeJsonBlobLengthForAllRows();
		long metaFileSizeLimit = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_META_QUEUE_FILESIZE_LIMIT);

		Log.d(logTag, "Meta JSON Snapshot added to Queue: " + metaQueryTimestamp + ", "
							+ FileUtils.bytesAsReadableString(metaDataJsonStr.length())
							+ " (" + metaQueueFullRecordCount + " snapshots, "
							+ Math.round( 100 * metaQueueFullByteLength / ( metaFileSizeLimit * 1024 * 1024 ) ) + "%"
							+ " of " + metaFileSizeLimit + " MB limit)"
		);

		archiveOldestMetaSnapshots( metaQueueFullByteLength, metaQueueFullRecordCount );

	}

	public void clearPrePackageMetaData() {
		clearPrePackageMetaData(new Date(System.currentTimeMillis()));
	}

	private void clearPrePackageMetaData(Date deleteBefore) {
		try {

			app.deviceSystemDb.dbDateTimeOffsets.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbMqttBroker.clearRowsBefore(deleteBefore);
			app.latencyStatsDb.dbCheckInLatency.clearRowsBefore(deleteBefore);
			app.latencyStatsDb.dbClassifyLatency.clearRowsBefore(deleteBefore);

			app.audioDetectionDb.dbFiltered.clearRowsBefore(deleteBefore);

			RfcxComm.deleteQuery("admin", "database_delete_rows_before", "system_meta|" + deleteBefore.getTime(), app.getResolver());

			RfcxComm.deleteQuery("admin", "database_delete_rows_before", "sentinel_power|" + deleteBefore.getTime(), app.getResolver());

			RfcxComm.deleteQuery("admin", "database_delete_rows_before", "sentinel_sensor|" + deleteBefore.getTime(), app.getResolver());

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	private void archiveOldestMetaSnapshots(long metaQueueFullByteLength, int metaQueueFullRecordCount) {

//		int metaQueueFullRecordCount = app.metaDb.dbMeta.getCount();
//		long metaQueueFullByteLength = app.metaDb.dbMeta.getCumulativeJsonBlobLengthForAllRows();
		long metaFileSizeLimit = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_META_QUEUE_FILESIZE_LIMIT);
		long metaFileSizeLimitInBytes = metaFileSizeLimit * 1024 * 1024;

		if (metaQueueFullByteLength >= metaFileSizeLimitInBytes) {

			int metaArchiveRecordCount = Math.round( metaQueueFullRecordCount / 3 ); // We archive 1/3 of the full queue size. This could be larger/smaller.
			long metaArchiveTimestamp = 0;
			long metaArchiveBlobSize = 0;
			List<String> archiveSuccessList = new ArrayList<String>();
			JSONArray metaJsonBlobsToArchive = new JSONArray();

			for (String[] metaRowsToArchive : app.metaDb.dbMeta.getRowsWithOffset(metaQueueFullRecordCount - metaArchiveRecordCount, metaArchiveRecordCount)) {
				try {
					metaJsonBlobsToArchive.put(new JSONObject(metaRowsToArchive[2]));
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}
				archiveSuccessList.add(metaRowsToArchive[1]);
				app.metaDb.dbMeta.deleteSingleRowByTimestamp(metaRowsToArchive[1]);
				if (metaArchiveTimestamp == 0) { metaArchiveTimestamp = Long.parseLong(metaRowsToArchive[1]); }
				metaArchiveBlobSize += metaRowsToArchive[2].length();
			}

			if (archiveSuccessList.size() > 0) {
				try {
					String metaJsobBlobDir = Environment.getExternalStorageDirectory().toString() + "/rfcx/archive/meta/" + (new SimpleDateFormat("yyyy", Locale.US)).format(new Date(metaArchiveTimestamp));
					FileUtils.initializeDirectoryRecursively(metaJsobBlobDir, true);
					String metaJsobBlobFilePath = metaJsobBlobDir+"/"+(new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US)).format(new Date(metaArchiveTimestamp))+".json";
					StringUtils.saveStringToFile(metaJsonBlobsToArchive.toString(), metaJsobBlobFilePath);
					FileUtils.gZipFile(metaJsobBlobFilePath, metaJsobBlobFilePath+".gz");
					FileUtils.delete(metaJsobBlobFilePath);
					if (FileUtils.exists(metaJsobBlobFilePath+".gz")) {
						Log.i(logTag, archiveSuccessList.size() + " Meta blobs (" + FileUtils.bytesAsReadableString(metaArchiveBlobSize) + ") have been archived to " + metaJsobBlobFilePath);
					} else {
						Log.e(logTag, "Meta blob archive process failed...");
					}
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}
	}

	private JSONObject addConcatSystemMetaParams(JSONObject metaDataJsonObj, JSONArray systemMetaJsonArray) throws JSONException {
		for (int i = 0; i < systemMetaJsonArray.length(); i++) {
			JSONObject systemJsonRow = systemMetaJsonArray.getJSONObject(i);
			Iterator<String> paramLabels = systemJsonRow.keys();
			while (paramLabels.hasNext()) {
				String paramLabel = paramLabels.next();
				if ( (systemJsonRow.get(paramLabel) instanceof String) && (systemJsonRow.getString(paramLabel).length() > 0) ) {
					metaDataJsonObj.put(paramLabel, systemJsonRow.getString(paramLabel));
				} else {
					metaDataJsonObj.put(paramLabel, "");
				}
			}
		}
		return metaDataJsonObj;
	}

	public static String getConcatMetaField(JSONArray metaJsonArray) throws JSONException {
		ArrayList<String> metaBlobs = new ArrayList<String>();
		for (int i = 0; i < metaJsonArray.length(); i++) {
			JSONObject metaJsonRow = metaJsonArray.getJSONObject(i);
			Iterator<String> paramLabels = metaJsonRow.keys();
			while (paramLabels.hasNext()) {
				String paramLabel = paramLabels.next();
				if ( (metaJsonRow.get(paramLabel) instanceof String) && (metaJsonRow.getString(paramLabel).length() > 0) ) {
					metaBlobs.add(metaJsonRow.getString(paramLabel));
				}
			}
		}
		return (metaBlobs.size() > 0) ? TextUtils.join("|", metaBlobs) : "";
	}



	public String buildCheckInQueueJson(String[] audioFileInfo) {

		try {
			JSONObject queueJson = new JSONObject();

			// Recording the moment the check in was queued
			queueJson.put("queued_at", System.currentTimeMillis());

			// Adding audio file metadata
			List<String> audioFiles = new ArrayList<String>();
			audioFiles.add(TextUtils.join("*", audioFileInfo));
			queueJson.put("audio", TextUtils.join("|", audioFiles));

			return queueJson.toString();

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
			return "{}";
		}
	}

	public JSONObject buildCheckInPrefsJsonObj(boolean overrideLimitByLastAccessedAt) {

		JSONObject prefsObj = new JSONObject();
		try {

			long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(app.rfcxPrefs.prefsTimestampLastFullApiSync));
			String prefsSha1 = app.rfcxPrefs.getPrefsChecksum();
			prefsObj.put("sha1", prefsSha1);

			if (	(app.rfcxPrefs.prefsSha1FullApiSync != null)
					&&	!app.rfcxPrefs.prefsSha1FullApiSync.equalsIgnoreCase(prefsSha1)
					&& 	(overrideLimitByLastAccessedAt || (milliSecondsSinceAccessed > app.apiMqttUtils.getSetCheckInPublishTimeOutLength()))
			) {
				Log.v(logTag, "Prefs local checksum mismatch with API. Local Prefs snapshot will be sent.");
				prefsObj.put("vals", app.rfcxPrefs.getPrefsAsJsonObj());
				app.rfcxPrefs.prefsTimestampLastFullApiSync = System.currentTimeMillis();
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return prefsObj;
	}


	private JSONObject retrieveAndBundleMetaJson(int maxMetaRowsToBundle, boolean overrideLimitByLastAccessedAt) throws JSONException {

		JSONObject metaJsonBundledSnapshotsObj = null;
		JSONArray metaJsonBundledSnapshotsIds = new JSONArray();
		long metaMeasuredAtValue = 0;

		List<String[]> metaRows = (overrideLimitByLastAccessedAt) ? app.metaDb.dbMeta.getLatestRowsWithLimit(maxMetaRowsToBundle) :
				app.metaDb.dbMeta.getLatestRowsNotAccessedSinceWithLimit( (System.currentTimeMillis() - app.apiMqttUtils.getSetCheckInPublishTimeOutLength()), maxMetaRowsToBundle);

		for (String[] metaRow : metaRows) {

			// add meta snapshot ID to array of IDs
			metaJsonBundledSnapshotsIds.put(metaRow[1]);

			// if this is the first row to be examined, initialize the bundled object with this JSON blob
			if (metaJsonBundledSnapshotsObj == null) {
				metaJsonBundledSnapshotsObj = new JSONObject(metaRow[2]);

			} else {
				JSONObject metaJsonObjToAppend = new JSONObject(metaRow[2]);
				Iterator<String> jsonKeys = metaJsonBundledSnapshotsObj.keys();
				while (jsonKeys.hasNext()) {
					String jsonKey = jsonKeys.next();

					if (	(metaJsonBundledSnapshotsObj.get(jsonKey) instanceof String)
							&&	metaJsonObjToAppend.has(jsonKey)
							&&	(metaJsonObjToAppend.get(jsonKey) instanceof String)
					) {
						String origStr = metaJsonBundledSnapshotsObj.getString(jsonKey);
						String newStr = metaJsonObjToAppend.getString(jsonKey);
						if (	 (origStr.length() > 0) && (newStr.length() > 0) ) {
							metaJsonBundledSnapshotsObj.put(jsonKey, origStr+"|"+newStr);
						} else {
							metaJsonBundledSnapshotsObj.put(jsonKey, origStr+newStr);
						}
						if (jsonKey.equalsIgnoreCase("measured_at")) {
							long measuredAt = Long.parseLong(newStr);
							Log.e(logTag, "measured_at: "+DateTimeUtils.getDateTime(measuredAt));
							if (measuredAt > metaMeasuredAtValue) {
								metaMeasuredAtValue = measuredAt;
							}
						}
					}
				}
			}

			// Overwrite meta_ids attribute with updated array of snapshot IDs
			metaJsonBundledSnapshotsObj.put("meta_ids", metaJsonBundledSnapshotsIds);

			// mark this row as accessed in the database
			app.metaDb.dbMeta.updateLastAccessedAtByTimestamp(metaRow[1]);

			// if the bundle already contains max number of snapshots, stop here
			if (metaJsonBundledSnapshotsIds.length() >= maxMetaRowsToBundle) { break; }
		}

		// if no meta data was available to bundle, then we create an empty object
		if (metaJsonBundledSnapshotsObj == null) { metaJsonBundledSnapshotsObj = new JSONObject(); }

		// use highest measured_at value, or if empty, set to current time
		metaJsonBundledSnapshotsObj.put("measured_at", ((metaMeasuredAtValue == 0) ? System.currentTimeMillis() : metaMeasuredAtValue) );

		return metaJsonBundledSnapshotsObj;
	}


}
