package org.rfcx.guardian.guardian.api.mqtt;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.hardware.DeviceHardwareUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ApiJsonUtils {

	public ApiJsonUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInMetaUtils");

	private RfcxGuardian app;

	public String prefsSha1FullApiSync = null;
	public long prefsTimestampLastFullApiSync = 0;

	public boolean hasDeviceInfoBeenSent = false;


	public String buildCheckInJson(String checkInJsonString, String[] screenShotMeta, String[] logFileMeta, String[] photoFileMeta, String[] videoFileMeta) throws JSONException, IOException {

		JSONObject checkInMetaJson = retrieveAndBundleMetaJson();

		// Adding Audio JSON fields from checkin table
		JSONObject checkInJsonObj = new JSONObject(checkInJsonString);
		checkInMetaJson.put("queued_at", checkInJsonObj.getLong("queued_at"));
		checkInMetaJson.put("audio", checkInJsonObj.getString("audio"));

		// Recording number of currently queued/skipped/stashed checkins
		checkInMetaJson.put("checkins", getCheckInStatusInfoForJson( new String[] { "sent" } ));

		checkInMetaJson.put("purged", app.assetUtils.getAssetExchangeLogList("purged", 4 * app.rfcxPrefs.getPrefAsInt("checkin_meta_bundle_limit")));

		// Device: Phone & Android info
		if (!hasDeviceInfoBeenSent || (Math.round(Math.random()*10) == 1)) {
			this.hasDeviceInfoBeenSent = true;
			JSONObject deviceJsonObj = new JSONObject();
			deviceJsonObj.put("phone", app.deviceMobilePhone.getMobilePhoneInfoJson());
			deviceJsonObj.put("android", DeviceHardwareUtils.getInfoAsJson());
			checkInMetaJson.put("device", deviceJsonObj);
		}

		// Adding software role versions
		checkInMetaJson.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));

		// Adding checksum of current prefs values
		checkInMetaJson.put("prefs", app.apiJsonUtils.buildCheckInPrefsJsonObj());

		// Adding instructions, if there are any
		if (app.instructionsUtils.getInstructionsCount() > 0) {
			checkInMetaJson.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
		}

		// Adding messages to JSON blob
		JSONArray smsArr = RfcxComm.getQueryContentProvider("admin", "database_get_all_rows", "sms", app.getApplicationContext().getContentResolver());
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

		Log.d(logTag,checkInMetaJson.toString());

		// Adding Guardian GUID and Auth Token
		JSONObject guardianObj = new JSONObject();
		guardianObj.put("guid", this.app.rfcxGuardianIdentity.getGuid());
		guardianObj.put("token", this.app.rfcxGuardianIdentity.getAuthToken());
		checkInMetaJson.put("guardian", guardianObj);

		return checkInMetaJson.toString();

	}


	public String buildPingJson(boolean includeAllExtraFields, String[] includeExtraFields) throws JSONException, IOException {

		JSONObject pingObj = new JSONObject();

		boolean includeMeasuredAt = false;

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "battery")) {
			pingObj.put("battery", app.deviceBattery.getBatteryStateAsConcatString(app.getApplicationContext(), null) );
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "checkins")) {
			pingObj.put("checkins", getCheckInStatusInfoForJson(new String[] {}));
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "instructions")) {
			pingObj.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "prefs")) {
			pingObj.put("prefs", app.apiJsonUtils.buildCheckInPrefsJsonObj());
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "device")) {
			JSONObject deviceJsonObj = new JSONObject();
			deviceJsonObj.put("phone", app.deviceMobilePhone.getMobilePhoneInfoJson());
			deviceJsonObj.put("android", DeviceHardwareUtils.getInfoAsJson());
			pingObj.put("device", deviceJsonObj);
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "software")) {
			pingObj.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_power")) {
			String sentinelPower = getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "sentinel_power", app.getApplicationContext().getContentResolver()));
			if (sentinelPower.length() > 0) { pingObj.put("sentinel_power", sentinelPower); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_sensor")) {
			String sentinelSensor = getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "sentinel_sensor", app.getApplicationContext().getContentResolver()));
			if (sentinelSensor.length() > 0) { pingObj.put("sentinel_sensor", sentinelSensor); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "storage")) {
			String systemStorage = getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "system_storage", app.getApplicationContext().getContentResolver()));
			if (systemStorage.length() > 0) { pingObj.put("storage", systemStorage); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "memory")) {
			String systemMemory = getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "system_memory", app.getApplicationContext().getContentResolver()));
			if (systemMemory.length() > 0) { pingObj.put("memory", systemMemory); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "cpu")) {
			String systemCPU = getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "system_cpu", app.getApplicationContext().getContentResolver()));
			if (systemCPU.length() > 0) { pingObj.put("cpu", systemCPU); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "network")) {
			String networkCPU = getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "system_network", app.getApplicationContext().getContentResolver()));
			if (networkCPU.length() > 0) { pingObj.put("network", networkCPU); }
			includeMeasuredAt = true;
		}

		if (includeMeasuredAt) { pingObj.put("measured_at", System.currentTimeMillis()); }

		Log.d(logTag, pingObj.toString());

		JSONObject guardianObj = new JSONObject();
		guardianObj.put("guid", app.rfcxGuardianIdentity.getGuid());
		guardianObj.put("token", app.rfcxGuardianIdentity.getAuthToken());
		pingObj.put("guardian", guardianObj);

		return pingObj.toString();
	}




	private String getCheckInStatusInfoForJson(String[] includeAssetIdLists) {

		Map<String, String> assetExtraInfo = new HashMap<String, String>();

		for (String assetType : new String[] { "sent", "queued", "meta", "skipped", "stashed", "archived" }) {

			StringBuilder assetInfo = new StringBuilder();

			if (assetType.equalsIgnoreCase("sent")) {
				assetInfo.append("*").append( app.apiCheckInDb.dbSent.getCumulativeFileSizeForAllRows() );
			} else if (assetType.equalsIgnoreCase("queued")) {
				assetInfo.append("*").append( app.apiCheckInDb.dbQueued.getCumulativeFileSizeForAllRows() );
			} else if (assetType.equalsIgnoreCase("meta")) {
				assetInfo.append("*").append( FileUtils.getFileSizeInBytes(app.metaDb.dbMeta.FILEPATH) );
			} else if (assetType.equalsIgnoreCase("skipped")) {
				assetInfo.append("*").append( app.apiCheckInDb.dbSkipped.getCumulativeFileSizeForAllRows() );
			} else if (assetType.equalsIgnoreCase("stashed")) {
				assetInfo.append("*").append( app.apiCheckInDb.dbStashed.getCumulativeFileSizeForAllRows() );
			} else if (assetType.equalsIgnoreCase("archived")) {
				assetInfo.append("*").append( app.archiveDb.dbCheckInArchive.getCumulativeFileSizeForAllRows() );
			}

			if (ArrayUtils.doesStringArrayContainString(includeAssetIdLists, assetType)) {
				long reportAssetIdIfOlderThan = 4 * this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
				for (String[] _checkIn : app.apiCheckInDb.dbSent.getLatestRowsWithLimit(15)) {
					long assetId = Long.parseLong(_checkIn[1].substring(0, _checkIn[1].lastIndexOf(".")));
					if (reportAssetIdIfOlderThan < Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(assetId))) {
						assetInfo.append("*").append(assetId);
					}
				}
			}

			assetExtraInfo.put(assetType, assetInfo.toString());
		}

		return TextUtils.join("|",
				new String[] {
						"sent*" + app.apiCheckInDb.dbSent.getCount() + assetExtraInfo.get("sent"),
						"queued*" + app.apiCheckInDb.dbQueued.getCount() + assetExtraInfo.get("queued"),
						"meta*" + app.metaDb.dbMeta.getCount() + assetExtraInfo.get("meta"),
						"skipped*" + app.apiCheckInDb.dbSkipped.getCount() + assetExtraInfo.get("skipped"),
						"stashed*" + app.apiCheckInDb.dbStashed.getCount() + assetExtraInfo.get("stashed"),
						"archived*" + app.archiveDb.dbCheckInArchive.getInnerRecordCumulativeCount() + assetExtraInfo.get("archived")
				});
	}


	public void createSystemMetaDataJsonSnapshot() throws JSONException {

		JSONObject metaDataJsonObj = new JSONObject();
		Date metaQueryTimestampObj = new Date();
		long metaQueryTimestamp = metaQueryTimestampObj.getTime();

		JSONArray metaIds = new JSONArray();
		metaIds.put(metaQueryTimestamp);
		metaDataJsonObj.put("meta_ids", metaIds);
		metaDataJsonObj.put("measured_at", metaQueryTimestamp);

		metaDataJsonObj.put("broker_connections", app.deviceSystemDb.dbMqttBrokerConnections.getConcatRows());

		// Adding connection data from previous checkins
		metaDataJsonObj.put("previous_checkins", app.apiCheckInStatsDb.dbStats.getConcatRows());

		// Adding system metadata, if they can be retrieved from admin role via content provider
		JSONArray systemMetaJsonArray = RfcxComm.getQueryContentProvider("admin", "database_get_all_rows",
				"system_meta", app.getApplicationContext().getContentResolver());
		metaDataJsonObj = addConcatSystemMetaParams(metaDataJsonObj, systemMetaJsonArray);

		// Adding sentinel power data, if they can be retrieved from admin role via content provider
		String sentinelPower = getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "database_get_all_rows",
				"sentinel_power", app.getApplicationContext().getContentResolver()));
		if (sentinelPower.length() > 0) { metaDataJsonObj.put("sentinel_power", sentinelPower); }

		// Adding sentinel sensor data, if they can be retrieved from admin role via content provider
		String sentinelSensor = getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "database_get_all_rows",
				"sentinel_sensor", app.getApplicationContext().getContentResolver()));
		if (sentinelSensor.length() > 0) { metaDataJsonObj.put("sentinel_sensor", sentinelSensor); }

		ArrayList<String> dateTimeOffsets = new ArrayList<String>();
		if (metaDataJsonObj.has("datetime_offsets")) { dateTimeOffsets.add(metaDataJsonObj.getString("datetime_offsets")); }
		if (app.deviceSystemDb.dbDateTimeOffsets.getCount() > 0) { dateTimeOffsets.add(app.deviceSystemDb.dbDateTimeOffsets.getConcatRows()); }
		if (dateTimeOffsets.size() > 0) { metaDataJsonObj.put("datetime_offsets", TextUtils.join("|", dateTimeOffsets)); }

		// Saves JSON snapshot blob to database
		app.metaDb.dbMeta.insert(metaQueryTimestamp, metaDataJsonObj.toString());

		clearPrePackageMetaData(metaQueryTimestampObj);

	}

	public void clearPrePackageMetaData() {
		clearPrePackageMetaData(new Date(System.currentTimeMillis()));
	}

	private void clearPrePackageMetaData(Date deleteBefore) {
		try {

			app.deviceSystemDb.dbDateTimeOffsets.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbMqttBrokerConnections.clearRowsBefore(deleteBefore);
			app.apiCheckInStatsDb.dbStats.clearRowsBefore(deleteBefore);

			RfcxComm.deleteQueryContentProvider("admin", "database_delete_rows_before",
					"system_meta|" + deleteBefore.getTime(), app.getApplicationContext().getContentResolver());

			RfcxComm.deleteQueryContentProvider("admin", "database_delete_rows_before",
					"sentinel_power|" + deleteBefore.getTime(), app.getApplicationContext().getContentResolver());

			RfcxComm.deleteQueryContentProvider("admin", "database_delete_rows_before",
					"sentinel_sensor|" + deleteBefore.getTime(), app.getApplicationContext().getContentResolver());

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
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

	private String getConcatMetaField(JSONArray metaJsonArray) throws JSONException {
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

	private JSONObject buildCheckInPrefsJsonObj() {

		JSONObject prefsObj = new JSONObject();
		try {

			long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(this.prefsTimestampLastFullApiSync));
			String prefsSha1 = app.rfcxPrefs.getPrefsChecksum();
			prefsObj.put("sha1", prefsSha1);

			if (	(this.prefsSha1FullApiSync != null)
					&&	!this.prefsSha1FullApiSync.equalsIgnoreCase(prefsSha1)
					&& 	(milliSecondsSinceAccessed > app.apiCheckInUtils.getSetCheckInPublishTimeOutLength())
			) {
				Log.v(logTag, "Prefs local checksum mismatch with API. Local Prefs snapshot will be sent.");
				prefsObj.put("vals", app.rfcxPrefs.getPrefsAsJsonObj());
				this.prefsTimestampLastFullApiSync = System.currentTimeMillis();
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return prefsObj;
	}


	private JSONObject retrieveAndBundleMetaJson() throws JSONException {

		int maxMetaRowsToBundle = app.rfcxPrefs.getPrefAsInt("checkin_meta_bundle_limit");
		if (app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_sampling_ratio")) {
			maxMetaRowsToBundle = maxMetaRowsToBundle + app.audioCaptureUtils.samplingRatioArr[0] + app.audioCaptureUtils.samplingRatioArr[1];
		}

		JSONObject metaJsonBundledSnapshotsObj = null;
		JSONArray metaJsonBundledSnapshotsIds = new JSONArray();
		long metaMeasuredAtValue = 0;

		for (String[] metaRow : app.metaDb.dbMeta.getLatestRowsWithLimit(2 * maxMetaRowsToBundle)) {

			long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(Long.parseLong(metaRow[3])));

			if (milliSecondsSinceAccessed > app.apiCheckInUtils.getSetCheckInPublishTimeOutLength()) {

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
								&&	(metaJsonObjToAppend.get(jsonKey) != null)
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
		}

		// if no meta data was available to bundle, then we create an empty object
		if (metaJsonBundledSnapshotsObj == null) { metaJsonBundledSnapshotsObj = new JSONObject(); }

		// use highest measured_at value, or if empty, set to current time
		metaJsonBundledSnapshotsObj.put("measured_at", ((metaMeasuredAtValue == 0) ? System.currentTimeMillis() : metaMeasuredAtValue) );

		return metaJsonBundledSnapshotsObj;
	}


}
