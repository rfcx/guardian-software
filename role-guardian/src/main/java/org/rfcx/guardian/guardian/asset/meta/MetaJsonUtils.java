package org.rfcx.guardian.guardian.asset.meta;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MetaJsonUtils {

	public MetaJsonUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "MetaJsonUtils");

	private RfcxGuardian app;



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

		// Adding Swarm diagnostic data, if they can be retrieved from admin role via content provider
		String swmDiagnostic = getConcatMetaField(RfcxComm.getQuery("admin", "database_get_all_rows",
				"swm_diagnostic", app.getResolver()));
		if (swmDiagnostic.length() > 0) { metaDataJsonObj.put("swm", swmDiagnostic); }

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

		Log.d(logTag, "Meta Snapshot Created: " + metaQueryTimestamp + ", "
							+ FileUtils.bytesAsReadableString(metaDataJsonStr.length())
							+ " (" + metaQueueFullRecordCount + " snapshots, "
							+ Math.round( 100 * metaQueueFullByteLength / ( metaFileSizeLimit * 1024 * 1024 ) ) + "%"
							+ " of " + metaFileSizeLimit + " MB limit)"
		);

		archiveOldestMetaSnapshots( metaQueueFullByteLength, metaQueueFullRecordCount );

	}

	private void clearPrePackageMetaData(Date deleteBefore) {
		try {

			app.deviceSystemDb.dbDateTimeOffsets.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbMqttBroker.clearRowsBefore(deleteBefore);
			app.latencyStatsDb.dbCheckInLatency.clearRowsBefore(deleteBefore);
			app.latencyStatsDb.dbClassifyLatency.clearRowsBefore(deleteBefore);

			RfcxComm.deleteQuery("admin", "database_delete_rows_before", "system_meta|" + deleteBefore.getTime(), app.getResolver());

			RfcxComm.deleteQuery("admin", "database_delete_rows_before", "sentinel_power|" + deleteBefore.getTime(), app.getResolver());

			RfcxComm.deleteQuery("admin", "database_delete_rows_before", "sentinel_sensor|" + deleteBefore.getTime(), app.getResolver());

			RfcxComm.deleteQuery("admin", "database_delete_rows_before", "swm_diagnostic|" + deleteBefore.getTime(), app.getResolver());

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



	public JSONObject buildPrefsJsonObj(boolean overrideLimitByLastAccessedAt, boolean forceFullValuesDump) {

		JSONObject prefsObj = new JSONObject();
		try {

			long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(app.rfcxPrefs.prefsSync_TimestampLastSync));
			String prefsSha1 = app.rfcxPrefs.getPrefsChecksum();
			prefsObj.put("sha1", prefsSha1.substring(0, RfcxPrefs.prefsSync_Sha1CharLimit));

			if (	forceFullValuesDump
					||	(	(app.rfcxPrefs.prefsSync_Sha1Value != null)
						&&	!app.rfcxPrefs.prefsSync_Sha1Value.equalsIgnoreCase(prefsSha1.substring(0, RfcxPrefs.prefsSync_Sha1CharLimit))
						&&	!app.rfcxPrefs.prefsSync_Sha1Value.equalsIgnoreCase(prefsSha1)
						&& 	(overrideLimitByLastAccessedAt || (milliSecondsSinceAccessed > app.apiMqttUtils.getSetCheckInPublishTimeOutLength()))
						)
			) {
				if (!forceFullValuesDump) { Log.v(logTag, "Prefs local checksum mismatch with API. Local Prefs snapshot will be sent."); }
				prefsObj.put("vals", app.rfcxPrefs.getPrefsAsJsonObj());
				if (!forceFullValuesDump) { app.rfcxPrefs.prefsSync_TimestampLastSync = System.currentTimeMillis(); }
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return prefsObj;
	}


	public JSONObject retrieveAndBundleMetaJson(JSONObject inputMetaJson, int maxMetaRowsToBundle, boolean overrideFilterByLastAccessedAt) throws JSONException {

		JSONObject metaJsonBundledSnapshotsObj = inputMetaJson;
		JSONArray metaJsonBundledSnapshotsIds = new JSONArray();
		long metaMeasuredAtValue = ((metaJsonBundledSnapshotsObj == null) || (!metaJsonBundledSnapshotsObj.has("measured_at"))) ? 0 : metaJsonBundledSnapshotsObj.getLong("measured_at");

		List<String[]> metaRows = (overrideFilterByLastAccessedAt) ? app.metaDb.dbMeta.getLatestRowsWithLimit(maxMetaRowsToBundle) :
				app.metaDb.dbMeta.getLatestRowsNotAccessedSinceWithLimit( (System.currentTimeMillis() - app.apiMqttUtils.getSetCheckInPublishTimeOutLength()), maxMetaRowsToBundle);

		for (String[] metaRow : metaRows) {

			// add meta snapshot ID to array of IDs
			metaJsonBundledSnapshotsIds.put(metaRow[1]);

			// if this is the first row to be examined, initialize the bundled object with this JSON blob
			if (metaJsonBundledSnapshotsObj == null) {
				metaJsonBundledSnapshotsObj = new JSONObject(metaRow[2]);

			} else {
				JSONObject metaJsonObjToAppend = new JSONObject(metaRow[2]);

				Iterator<String> appendKeys = metaJsonObjToAppend.keys();
				Iterator<String> bundleKeys = metaJsonBundledSnapshotsObj.keys();
				List<String> allKeys = new ArrayList<>();
				while (bundleKeys.hasNext()) { String bndlKey = bundleKeys.next(); if (!ArrayUtils.doesStringListContainString(allKeys, bndlKey)) { allKeys.add(bndlKey); } }
				while (appendKeys.hasNext()) { String apnKey = appendKeys.next(); if (!ArrayUtils.doesStringListContainString(allKeys, apnKey)) { allKeys.add(apnKey); } }

				for (String jsonKey : allKeys) {

					if (	!metaJsonBundledSnapshotsObj.has(jsonKey)
							&&	metaJsonObjToAppend.has(jsonKey)
							&&	(metaJsonObjToAppend.get(jsonKey) instanceof String)
					) {
						String newStr = metaJsonObjToAppend.getString(jsonKey);
						metaJsonBundledSnapshotsObj.put(jsonKey, newStr);

					} else if (	metaJsonBundledSnapshotsObj.has(jsonKey)
							&&	(metaJsonBundledSnapshotsObj.get(jsonKey) instanceof String)
							&&	metaJsonObjToAppend.has(jsonKey)
							&&	(metaJsonObjToAppend.get(jsonKey) instanceof String)
					) {
						String origStr = metaJsonBundledSnapshotsObj.getString(jsonKey);
						String newStr = metaJsonObjToAppend.getString(jsonKey);
						if ( (origStr.length() > 0) && (newStr.length() > 0) ) {
							metaJsonBundledSnapshotsObj.put(jsonKey, origStr+"|"+newStr);
						} else {
							metaJsonBundledSnapshotsObj.put(jsonKey, origStr+newStr);
						}

					} else if (jsonKey.equalsIgnoreCase("measured_at")) {

						long measuredAt = metaJsonObjToAppend.getLong(jsonKey);
						if (measuredAt > metaMeasuredAtValue) {
							metaMeasuredAtValue = measuredAt;
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
		metaJsonBundledSnapshotsObj.put("measured_at", ""+((metaMeasuredAtValue == 0) ? System.currentTimeMillis() : metaMeasuredAtValue) );

		return metaJsonBundledSnapshotsObj;
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

}
