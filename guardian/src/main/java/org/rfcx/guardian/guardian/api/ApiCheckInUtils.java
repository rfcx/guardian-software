package org.rfcx.guardian.guardian.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.rfcx.guardian.guardian.R;
import org.rfcx.guardian.guardian.activity.MainActivity;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.control.DeviceLogCat;
import org.rfcx.guardian.utility.device.control.DeviceScreenShot;
import org.rfcx.guardian.utility.device.hardware.DeviceHardwareUtils;
import org.rfcx.guardian.utility.mqtt.MqttUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class ApiCheckInUtils implements MqttCallback {

	public ApiCheckInUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

		this.requestTimeOutLength = 2 * this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
		initializeFailedCheckInThresholds();

		this.mqttCheckInClient = new MqttUtils(context, RfcxGuardian.APP_ROLE, this.app.rfcxDeviceGuid.getDeviceGuid());

		this.subscribeBaseTopic = (new StringBuilder()).append("guardians/").append(this.app.rfcxDeviceGuid.getDeviceGuid().toLowerCase(Locale.US)).append("/").append(RfcxGuardian.APP_ROLE.toLowerCase(Locale.US)).toString();
		//this.mqttCheckInClient.addSubscribeTopic(this.subscribeBaseTopic + "/instructions");
		this.mqttCheckInClient.addSubscribeTopic(this.subscribeBaseTopic + "/checkins");

		this.mqttCheckInClient.setOrResetBroker(this.app.rfcxPrefs.getPrefAsString("api_checkin_protocol"), this.app.rfcxPrefs.getPrefAsInt("api_checkin_port"), this.app.rfcxPrefs.getPrefAsString("api_checkin_host"));
		this.mqttCheckInClient.setCallback(this);
		this.mqttCheckInClient.setActionTimeout(this.requestTimeOutLength);

		confirmOrCreateConnectionToBroker();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInUtils.class);

	private RfcxGuardian app;
	private MqttUtils mqttCheckInClient = null;

	public long requestTimeOutLength = 0;

	private String subscribeBaseTopic = null;

	private long requestSendReturned = System.currentTimeMillis();

	private String inFlightCheckInAudioId = null;
	private String latestCheckInAudioId = null;
	private Map<String, String[]> inFlightCheckInEntries = new HashMap<String, String[]>();
	private Map<String, long[]> inFlightCheckInStats = new HashMap<String, long[]>();

	private List<String> previousCheckIns = new ArrayList<String>();

//	private Date preFlightStatsQueryTimestamp = new Date();

	private int[] failedCheckInThresholds = new int[0];
	private boolean[] failedCheckInThresholdsReached = new boolean[0];

	private Map<String, long[]> healthCheckMonitors = new HashMap<String, long[]>();
	private static final String[] healthCheckCategories = new String[] { "latency", "queued", "recent", "time-of-day" };
	private long[] healthCheckTargetLowerBounds = new long[healthCheckCategories.length];
	private long[] healthCheckTargetUpperBounds = new long[healthCheckCategories.length];
	private static final int healthCheckMeasurementCount = 6;
	private long[] healthCheckInitValues = new long[healthCheckMeasurementCount];

	public boolean addCheckInToQueue(String[] audioInfo, String filepath) {

		// serialize audio info into JSON for checkin queue insertion
		String queueJson = generateCheckInQueueJson(audioInfo);

		// add audio info to checkin queue
		int queuedCount = app.apiCheckInDb.dbQueued.insert(audioInfo[1] + "." + audioInfo[2], queueJson, "0", filepath);

		Log.d(logTag, "Queued (1/" + queuedCount + "): " + queueJson + " | " + filepath);

		// once queued, remove database reference from encode role
		app.audioEncodeDb.dbEncoded.deleteSingleRow(audioInfo[1]);

		return true;
	}

	public void stashOldestCheckIns() {

		int stashThreshold = app.rfcxPrefs.getPrefAsInt("checkin_stash_threshold");
		int archiveThreshold = app.rfcxPrefs.getPrefAsInt("checkin_archive_threshold");

		if (app.apiCheckInDb.dbQueued.getCount() > stashThreshold) {

			List<String[]> checkInsBeyondStashThreshold = app.apiCheckInDb.dbQueued.getRowsWithOffset(stashThreshold, archiveThreshold);

			// string list for reporting stashed checkins to the log
			List<String> stashList = new ArrayList<String>();
			int stashCount = 0;

			// cycle through stashable checkins and move them to the new table/database
			for (String[] checkInsToStash : checkInsBeyondStashThreshold) {
				stashCount = app.apiCheckInDb.dbStashed.insert(checkInsToStash[1], checkInsToStash[2], checkInsToStash[3], checkInsToStash[4]);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInsToStash[1]);
				stashList.add(checkInsToStash[1]);
			}

			// report in the logs
			Log.i(logTag, "Stashed CheckIns (" + stashCount + " total in database): " + TextUtils.join(" ", stashList));
		}

		if (app.apiCheckInDb.dbStashed.getCount() >= archiveThreshold) {
			app.rfcxServiceHandler.triggerService("ApiCheckInArchive", false);
		}
	}

	public void reQueueCheckIn(String checkInStatus, String audioId) {

		boolean isReQueued = false;
		String[] checkInToReQueue = new String[] {};
		String[] reQueuedCheckIn = new String[] {};

		// fetch check-in entry from relevant table, if it exists...
		if (checkInStatus.equalsIgnoreCase("sent")) {
			checkInToReQueue = app.apiCheckInDb.dbSent.getSingleRowByAudioAttachmentId(audioId);
		} else if (checkInStatus.equalsIgnoreCase("stashed")) {
			checkInToReQueue = app.apiCheckInDb.dbStashed.getSingleRowByAudioAttachmentId(audioId);
		} else if (checkInStatus.equalsIgnoreCase("skipped")) {
			checkInToReQueue = app.apiCheckInDb.dbSkipped.getSingleRowByAudioAttachmentId(audioId);
		}

		// if it exists, add entry to checkin table
		if (checkInToReQueue[0] != null) {

			int queuedCount = app.apiCheckInDb.dbQueued.insert(checkInToReQueue[1], checkInToReQueue[2], checkInToReQueue[3], checkInToReQueue[4]);
			reQueuedCheckIn = app.apiCheckInDb.dbQueued.getSingleRowByAudioAttachmentId(audioId);

			// if successfully inserted into queue table (and verified), delete from original table
			if (reQueuedCheckIn[0] != null) {

				if (checkInStatus.equalsIgnoreCase("sent")) {
					app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(audioId);
					checkInToReQueue = app.apiCheckInDb.dbSent.getSingleRowByAudioAttachmentId(audioId);
				} else if (checkInStatus.equalsIgnoreCase("stashed")) {
					app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(audioId);
					checkInToReQueue = app.apiCheckInDb.dbStashed.getSingleRowByAudioAttachmentId(audioId);
				} else if (checkInStatus.equalsIgnoreCase("skipped")) {
					app.apiCheckInDb.dbSkipped.deleteSingleRowByAudioAttachmentId(audioId);
					checkInToReQueue = app.apiCheckInDb.dbSkipped.getSingleRowByAudioAttachmentId(audioId);
				}

				isReQueued = (checkInToReQueue[0] == null);
			}
		}

		if (isReQueued) {
			Log.i(logTag, "CheckIn Successfully ReQueued: "+audioId);
		} else {
			Log.e(logTag, "CheckIn Failed to ReQueue: "+audioId);
		}

	}

	private void setupRecentActivityHealthCheck() {

		// fill initial array with garbage (very high) values to ensure that checks will fail until we have the required number of checkins to compare
		Arrays.fill(healthCheckInitValues, Math.round(Long.MAX_VALUE / healthCheckMeasurementCount));

		// initialize categories with initial arrays (to be filled incrementally with real data)
		for (int j = 0; j < healthCheckCategories.length; j++) {
			if (!healthCheckMonitors.containsKey(healthCheckCategories[j])) { healthCheckMonitors.put(healthCheckCategories[j], healthCheckInitValues); }
		}

		// set parameters (bounds) for health check pass or fail

		/* latency */		healthCheckTargetLowerBounds[0] = 0;
							healthCheckTargetUpperBounds[0] = Math.round( 0.4 * app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000);

		/* queued */			healthCheckTargetLowerBounds[1] = 0;
							healthCheckTargetUpperBounds[1] = 1;

		/* recent */			healthCheckTargetLowerBounds[2] = 0;
							healthCheckTargetUpperBounds[2] = ( healthCheckMeasurementCount / 2 ) * (app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000);

		/* time-of-day */	healthCheckTargetLowerBounds[3] = 9;
							healthCheckTargetUpperBounds[3] = 15;
	}

	private void runRecentActivityHealthCheck(long[] inputValues) {

		if (!healthCheckMonitors.containsKey(healthCheckCategories[0])) { setupRecentActivityHealthCheck(); }

		long[] currAvgVals = new long[healthCheckCategories.length]; Arrays.fill(currAvgVals, 0);

		for (int j = 0; j < healthCheckCategories.length; j++) {
			String categ = healthCheckCategories[j];
			long[] arraySnapshot = new long[healthCheckMeasurementCount];
			arraySnapshot[0] = inputValues[j];
			for (int i = (healthCheckMeasurementCount-1); i > 0; i--) { arraySnapshot[i] = healthCheckMonitors.get(categ)[i-1]; }
			healthCheckMonitors.remove(healthCheckCategories[j]);
			healthCheckMonitors.put(healthCheckCategories[j], arraySnapshot);
			currAvgVals[j] = ArrayUtils.getAverageAsLong(healthCheckMonitors.get(categ));
		}

		boolean isExceptionallyHealthy = true;
		StringBuilder healthCheckLogging = new StringBuilder();

		for (int j = 0; j < healthCheckCategories.length; j++) {

			long currAvgVal = currAvgVals[j];
			// some average values require modification before comparison to upper/lower bounds...
			if (healthCheckCategories[j].equalsIgnoreCase("recent")) { currAvgVal = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(currAvgVals[j])); }

			// compare to upper lower bounds, check for pass/fail
			if ((currAvgVal > healthCheckTargetUpperBounds[j]) || (currAvgVal < healthCheckTargetLowerBounds[j])) { isExceptionallyHealthy = false; }

			// generat some verbose logging feedback
			healthCheckLogging.append(", ").append(healthCheckCategories[j]).append(": ").append(currAvgVal).append("/")
							.append((healthCheckTargetLowerBounds[j] > 1) ? healthCheckTargetLowerBounds[j]+"-" : "")
							.append(healthCheckTargetUpperBounds[j]);
		}

		healthCheckLogging.insert(0,"ExceptionalHealthCheck (last "+healthCheckMeasurementCount+" checkins): "+( isExceptionallyHealthy ? "PASS" : "FAIL" ));

		if (!isExceptionallyHealthy) {
			Log.w(logTag,healthCheckLogging.toString());
		} else {
			Log.i(logTag,healthCheckLogging.toString());
		}

	}

	private String generateCheckInQueueJson(String[] audioFileInfo) {

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

	private void createSystemMetaDataJsonSnapshot() throws JSONException {

		JSONObject metaDataJsonObj = new JSONObject();

		try {

			Date metaQueryTimestampObj = new Date();
			long metaQueryTimestamp = metaQueryTimestampObj.getTime();

			JSONArray metaIds = new JSONArray();
			metaIds.put(metaQueryTimestamp);
			metaDataJsonObj.put("meta_ids", metaIds);
			metaDataJsonObj.put("measured_at", metaQueryTimestamp);
			metaDataJsonObj.put("cpu", app.deviceSystemDb.dbCPU.getConcatRows());
			metaDataJsonObj.put("power", app.deviceSystemDb.dbPower.getConcatRows());
			metaDataJsonObj.put("network", app.deviceSystemDb.dbTelephony.getConcatRows());
			metaDataJsonObj.put("offline", app.deviceSystemDb.dbOffline.getConcatRows());
			metaDataJsonObj.put("broker_connections", app.deviceSystemDb.dbMqttBrokerConnections.getConcatRows());
			metaDataJsonObj.put("datetime_offsets", app.deviceSystemDb.dbDateTimeOffsets.getConcatRows());
			metaDataJsonObj.put("lightmeter", app.deviceSensorDb.dbLightMeter.getConcatRows());
			metaDataJsonObj.put("data_transfer", app.deviceDataTransferDb.dbTransferred.getConcatRows());
			metaDataJsonObj.put("accelerometer", app.deviceSensorDb.dbAccelerometer.getConcatRows());
			metaDataJsonObj.put("reboots", app.rebootDb.dbRebootComplete.getConcatRows());
			metaDataJsonObj.put("geoposition", app.deviceSensorDb.dbGeoPosition.getConcatRows());
			metaDataJsonObj.put("disk_usage", app.deviceDiskDb.dbDiskUsage.getConcatRows());

			// Adding sentinel data, if they can be retrieved
			JSONArray sentinelPower = RfcxComm.getQueryContentProvider("admin", "database_get_latest_row",
					"sentinel_power", app.getApplicationContext().getContentResolver());
			metaDataJsonObj.put("sentinel_power", getConcatSentinelMeta(sentinelPower));
			if(app.sharedPrefs.getString("checkin_with_i2c_battery", "false").equals("true")){
				metaDataJsonObj.put("battery", getConcatSentinelMetaForBattery(sentinelPower));
			}else{
				metaDataJsonObj.put("battery", app.deviceSystemDb.dbBattery.getConcatRows());
			}
			// Saves JSON snapshot blob to database
			app.apiCheckInMetaDb.dbMeta.insert(metaQueryTimestamp, metaDataJsonObj.toString());

			clearPreFlightSystemMetaData(metaQueryTimestampObj);

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	private void clearPreFlightSystemMetaData(Date deleteBefore) {
		try {
			app.deviceSystemDb.dbBattery.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbCPU.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbPower.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbTelephony.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbOffline.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbMqttBrokerConnections.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbDateTimeOffsets.clearRowsBefore(deleteBefore);
			app.deviceSensorDb.dbLightMeter.clearRowsBefore(deleteBefore);
			app.deviceSensorDb.dbAccelerometer.clearRowsBefore(deleteBefore);
			app.deviceDataTransferDb.dbTransferred.clearRowsBefore(deleteBefore);
			app.rebootDb.dbRebootComplete.clearRowsBefore(deleteBefore);
			app.deviceSensorDb.dbGeoPosition.clearRowsBefore(deleteBefore);
			app.deviceDiskDb.dbDiskUsage.clearRowsBefore(deleteBefore);

			RfcxComm.deleteQueryContentProvider("admin", "database_delete_rows_before",
					"sentinel_power|" + deleteBefore.getTime(), app.getApplicationContext().getContentResolver());

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	private String getConcatSentinelMeta(JSONArray sentinelJsonArray) throws JSONException {
		ArrayList<String> sentinelMetaBlobs = new ArrayList<String>();
		for (int i = 0; i < sentinelJsonArray.length(); i++) {
			JSONObject sentinelJsonRow = sentinelJsonArray.getJSONObject(i);
			Iterator<String> paramLabels = sentinelJsonRow.keys();
			while (paramLabels.hasNext()) {
				String paramLabel = paramLabels.next();
				if ( (sentinelJsonRow.get(paramLabel) instanceof String) && (sentinelJsonRow.getString(paramLabel).length() > 0) ) {
					sentinelMetaBlobs.add(sentinelJsonRow.getString(paramLabel));
				}
			}
		}

		return (sentinelMetaBlobs.size() > 0) ? TextUtils.join("|", sentinelMetaBlobs) : "";
	}
	//todo: comment the example result
	private String getConcatSentinelMetaForBattery(JSONArray sentinelJsonArray) throws JSONException {
		ArrayList<String> sentinelMetaBlobs = new ArrayList<String>();
		for (int i = 0; i < sentinelJsonArray.length(); i++) {
			JSONObject sentinelJsonRow = sentinelJsonArray.getJSONObject(i);
			Iterator<String> paramLabels = sentinelJsonRow.keys();
			int count = 0;
			ArrayList<String> tempArray = new ArrayList<String>();
			while (paramLabels.hasNext()) {
				String paramLabel = paramLabels.next();
				if ( (sentinelJsonRow.get(paramLabel) instanceof String) && (sentinelJsonRow.getString(paramLabel).length() > 0) && count > 1) {
					tempArray.add(sentinelJsonRow.getString(paramLabel));
				}
				count++;
			}
			sentinelMetaBlobs.add(TextUtils.join("*", tempArray));
		}

		return (sentinelMetaBlobs.size() > 0) ? TextUtils.join("|", sentinelMetaBlobs) : "";
	}

	private String getAssetExchangeLogList(String assetStatus, int rowLimit) {

		List<String[]> assetRows = new ArrayList<String[]>();

		if (assetStatus.equalsIgnoreCase("purged")) {

			assetRows = app.apiAssetExchangeLogDb.dbPurged.getLatestRowsWithLimitExcludeCreatedAt(rowLimit);
			for (String[] assetRow : assetRows) { app.apiAssetExchangeLogDb.dbPurged.deleteSingleRowByTimestamp(assetRow[2]); }

		}/* else if (assetStatus.equalsIgnoreCase("sent")) {
			
			
		}*/

		return DbUtils.getConcatRows(assetRows);
	}

	private String getCheckInStatusInfoForJson() {

		StringBuilder _queued = (new StringBuilder()).append("queued").append("*").append(app.apiCheckInDb.dbQueued.getCount());
		StringBuilder _sent = (new StringBuilder()).append("sent").append("*").append(app.apiCheckInDb.dbSent.getCount());
		StringBuilder _skipped = (new StringBuilder()).append("skipped").append("*").append(app.apiCheckInDb.dbSkipped.getCount());
		StringBuilder _stashed = (new StringBuilder()).append("stashed").append("*").append(app.apiCheckInDb.dbStashed.getCount());

//		long sendIfOlderThan = 4 * this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;

		for (String[] _checkIn : app.apiCheckInDb.dbSent.getLatestRowsWithLimit(10)) {
//			long diff = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(Long.parseLong(checkInEntry[0])));
//			Log.e(logTag, "Difference ("+checkInEntry[1]+") "+ Math.round((diff / 1000) / 60) );
			_sent.append("*").append(_checkIn[1].substring(0, _checkIn[1].lastIndexOf(".")));
		}

		return TextUtils.join("|", new String[] { _queued.toString(), _sent.toString(), _skipped.toString(), _stashed.toString() });
	}

	private JSONObject retrieveAndBundleMetaJson() throws JSONException {

		int maxRowsToQuery = 10;
		int maxRowsToBundle = 2;

		JSONObject metaJsonBundledSnapshotsObj = null;
		JSONArray metaJsonBundledSnapshotsIds = new JSONArray();

		for (String[] metaRow : app.apiCheckInMetaDb.dbMeta.getLatestRowsWithLimit(maxRowsToQuery)) {

			long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds((long) Long.parseLong(metaRow[3])));

			if (milliSecondsSinceAccessed > this.requestTimeOutLength) {

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

						if (		(metaJsonBundledSnapshotsObj.get(jsonKey) instanceof String)
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

						}
					}
				}

				// Overwrite meta_ids attribute with updated array of snapshot IDs
				metaJsonBundledSnapshotsObj.put("meta_ids", metaJsonBundledSnapshotsIds);

				// mark this row as accessed in the database
				app.apiCheckInMetaDb.dbMeta.updateLastAccessedAtByTimestamp(metaRow[1]);

				// if the bundle is already contains max number of snapshots, stop here
				if (metaJsonBundledSnapshotsIds.length() >= maxRowsToBundle) { break; }
			}
		}

		return metaJsonBundledSnapshotsObj;
	}

	public String buildCheckInJson(String checkInJsonString, String[] screenShotMeta, String[] logFileMeta) throws JSONException, IOException {

		createSystemMetaDataJsonSnapshot();

		JSONObject checkInMetaJson = retrieveAndBundleMetaJson();

		// Adding Guardian GUID
		checkInMetaJson.put("guardian_guid", this.app.rfcxDeviceGuid.getDeviceGuid());

		// Adding Audio JSON fields from checkin table
		JSONObject checkInJsonObj = new JSONObject(checkInJsonString);
		checkInMetaJson.put("queued_at", checkInJsonObj.getLong("queued_at"));
		checkInMetaJson.put("audio", checkInJsonObj.getString("audio"));

		// Adding latency data from previous checkins
		checkInMetaJson.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));

		// Recording number of currently queued/skipped/stashed checkins
		checkInMetaJson.put("checkins", getCheckInStatusInfoForJson());

//		checkInMetaJson.put("assets_purged", getAssetExchangeLogList("purged", 10));

		// Telephony and SIM card info
		checkInMetaJson.put("phone", app.deviceMobilePhone.getMobilePhoneInfoJson());

		// Hardware info
		checkInMetaJson.put("hardware", DeviceHardwareUtils.getDeviceHardwareInfoJson());

		// Adding software role versions
		checkInMetaJson.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));

		// Adding checksum of current prefs values
		checkInMetaJson.put("prefs", app.rfcxPrefs.getPrefsChecksum());

		// Adding device location timezone offset
		checkInMetaJson.put("datetime", TextUtils.join("|", new String[] { "system*"+System.currentTimeMillis(), "timezone*"+DateTimeUtils.getTimeZoneOffset() }));

		// Adding messages to JSON blob
		checkInMetaJson.put("messages", RfcxComm.getQueryContentProvider("admin", "database_get_all_rows", "sms", app.getApplicationContext().getContentResolver()));

		// Adding screenshot meta to JSON blob
		checkInMetaJson.put("screenshots", (screenShotMeta[0] == null) ? "" :
				TextUtils.join("*", new String[] { screenShotMeta[1], screenShotMeta[2], screenShotMeta[3], screenShotMeta[4] })
				);

		// Adding logs meta to JSON blob
		checkInMetaJson.put("logs", (logFileMeta[0] == null) ? "" :
				TextUtils.join("*", new String[] { logFileMeta[1], logFileMeta[2], logFileMeta[3], logFileMeta[4] })
				);

		if (app.rfcxPrefs.getPrefAsBoolean("verbose_logging")) { Log.d(logTag,checkInMetaJson.toString()); }

		return checkInMetaJson.toString();

	}

	public byte[] packageMqttPayload(String checkInJsonString, String checkInAudioFilePath)
			throws UnsupportedEncodingException, IOException, JSONException {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		String[] screenShotMeta = getLatestExternalAssetMeta("screenshots");
		if ((screenShotMeta[0] != null) && !(new File(screenShotMeta[0])).exists()) {
			purgeSingleAsset("screenshot", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(),
					screenShotMeta[2], screenShotMeta[3]);
		}

		String[] logFileMeta = getLatestExternalAssetMeta("logs");
		if ((logFileMeta[0] != null) && !(new File(logFileMeta[0])).exists()) {
			purgeSingleAsset("log", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), logFileMeta[2],
					logFileMeta[3]);
		}

		byte[] jsonBlobAsBytes = StringUtils.gZipStringToByteArray(buildCheckInJson(checkInJsonString, screenShotMeta, logFileMeta));
		String jsonBlobMetaSection = String.format("%012d", jsonBlobAsBytes.length);
		byteArrayOutputStream.write(jsonBlobMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(jsonBlobAsBytes);

		byte[] audioFileAsBytes = new byte[0];
		if ((new File(checkInAudioFilePath)).exists()) {
			audioFileAsBytes = FileUtils.fileAsByteArray(checkInAudioFilePath);
		}
		String audioFileMetaSection = String.format("%012d", audioFileAsBytes.length);
		byteArrayOutputStream.write(audioFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(audioFileAsBytes);

		byte[] screenShotFileAsBytes = new byte[0];
		if ((screenShotMeta[0] != null) && (new File(screenShotMeta[0])).exists()) {
			screenShotFileAsBytes = FileUtils.fileAsByteArray(screenShotMeta[0]);
		}
		String screenShotFileMetaSection = String.format("%012d", screenShotFileAsBytes.length);
		byteArrayOutputStream.write(screenShotFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(screenShotFileAsBytes);

		byte[] logFileAsBytes = new byte[0];
		if ((logFileMeta[0] != null) && (new File(logFileMeta[0])).exists()) {
			logFileAsBytes = FileUtils.fileAsByteArray(logFileMeta[0]);
		}
		String logFileMetaSection = String.format("%012d", logFileAsBytes.length);
		byteArrayOutputStream.write(logFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(logFileAsBytes);

		byteArrayOutputStream.close();

		return byteArrayOutputStream.toByteArray();
	}

	public void sendMqttCheckIn(String[] checkInDatabaseEntry) {

		String audioId = checkInDatabaseEntry[1].substring(0, checkInDatabaseEntry[1].lastIndexOf("."));
		String audioExt = checkInDatabaseEntry[1].substring(1 + checkInDatabaseEntry[1].lastIndexOf("."));
		String audioPath = checkInDatabaseEntry[4];
		String audioJson = checkInDatabaseEntry[2];

		try {

			byte[] checkInPayload = packageMqttPayload(audioJson, audioPath);

			if ((new File(audioPath)).exists()) {

				this.inFlightCheckInAudioId = audioId;
				this.inFlightCheckInEntries.remove(audioId);
				this.inFlightCheckInEntries.put(audioId, checkInDatabaseEntry);

				app.apiCheckInDb.dbQueued.incrementSingleRowAttempts(audioId);
				long msgSendStart = this.mqttCheckInClient.publishMessage("guardians/checkins", checkInPayload);

				setInFlightCheckInStats(audioId, msgSendStart, 0, checkInPayload.length);

			} else {
				purgeSingleAsset("audio", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), audioId,
						audioExt);
			}

		} catch (Exception e) {

			RfcxLog.logExc(logTag, e);

			String excStr = RfcxLog.getExceptionContentAsString(e);

			if (excStr.contains("Too many publishes in progress")) {
				app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
				app.rfcxServiceHandler.triggerService("ApiCheckInJob", true);

			} else if (excStr.contains("UnknownHostException")) {
				app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);

			} else if (excStr.contains("Broken pipe")) {
				app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);

			} else if (excStr.contains("Timed out waiting for a response from the server")) {
				app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);

			} else if (excStr.contains("No route to host")) {
				app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);

			} else if (excStr.contains("Host is unresolved")) {
				app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);

			}

		}
	}

	public String[] getLatestExternalAssetMeta(String assetType) {

		String[] assetMeta = new String[] { null };
		try {
			JSONArray latestAssetMetaArray = RfcxComm.getQueryContentProvider("admin", "database_get_latest_row",
					assetType, app.getApplicationContext().getContentResolver());
			for (int i = 0; i < latestAssetMetaArray.length(); i++) {
				JSONObject latestAssetMeta = latestAssetMetaArray.getJSONObject(i);
				long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds((long) Long.parseLong(latestAssetMeta.getString("last_accessed_at"))));
				if (milliSecondsSinceAccessed > this.requestTimeOutLength) {
					assetMeta = new String[] { latestAssetMeta.getString("filepath"),
							latestAssetMeta.getString("created_at"), latestAssetMeta.getString("timestamp"),
							latestAssetMeta.getString("format"), latestAssetMeta.getString("digest") };
					RfcxComm.updateQueryContentProvider("admin", "database_set_last_accessed_at", assetType + "|" + latestAssetMeta.getString("timestamp"),
							app.getApplicationContext().getContentResolver());
					break;
				} else {
					Log.e(logTag,"Skipping asset attachent: "+assetType+", "+latestAssetMeta.getString("timestamp")+" was last sent only "+Math.round(milliSecondsSinceAccessed / 1000)+" seconds ago.");
				}
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return assetMeta;
	}

	private void initializeFailedCheckInThresholds() {

		String[] checkInThresholdsStr = TextUtils.split(app.rfcxPrefs.getPrefAsString("checkin_failure_thresholds"), ",");

		int[] checkInThresholds = new int[checkInThresholdsStr.length];
		boolean[] checkInThresholdsReached = new boolean[checkInThresholdsStr.length];

		for (int i = 0; i < checkInThresholdsStr.length; i++) {
			try {
				checkInThresholds[i] = Integer.parseInt(checkInThresholdsStr[i]);
				checkInThresholdsReached[i] = false;
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		this.failedCheckInThresholds = checkInThresholds;
		this.failedCheckInThresholdsReached = checkInThresholdsReached;
	}

	public void updateFailedCheckInThresholds() {

		if (this.failedCheckInThresholds.length > 0) {

			int minsSinceSuccess = (int) Math.floor(((System.currentTimeMillis() - this.requestSendReturned) / 1000) / 60);
			int minsSinceConnected = (int) Math.floor(((System.currentTimeMillis() - app.deviceConnectivity.lastConnectedAt()) / 1000) / 60);

			if (		// ...we haven't yet reached the first threshold for bad connectivity
					(minsSinceSuccess < this.failedCheckInThresholds[0])
					// OR... we are explicitly in offline mode
					|| !app.rfcxPrefs.getPrefAsBoolean("enable_checkin_publish")
					// OR... checkins are explicitly paused due to low battery level
					|| !isBatteryChargeSufficientForCheckIn()
					// OR... this is likely the first checkin after a period of disconnection
					|| (app.deviceConnectivity.isConnected() && (minsSinceConnected < this.failedCheckInThresholds[0]))
			) {
				for (int i = 0; i < this.failedCheckInThresholdsReached.length; i++) {
					this.failedCheckInThresholdsReached[i] = false;
				}
			} else {
				int i = 0;
				for (int toggleThreshold : this.failedCheckInThresholds) {
					if ((minsSinceSuccess >= toggleThreshold) && !this.failedCheckInThresholdsReached[i]) {
						this.failedCheckInThresholdsReached[i] = true;
						if (toggleThreshold == this.failedCheckInThresholds[this.failedCheckInThresholds.length - 1]) {
							// last index, force role(s) relaunch
							Log.d(logTag, "ToggleCheck: Forced Relaunch (" + toggleThreshold
									+ " minutes since last successful CheckIn)");
							app.deviceControlUtils.runOrTriggerDeviceControl("relaunch",
									app.getApplicationContext().getContentResolver());
						} else if (!app.deviceConnectivity.isConnected()) {
							Log.d(logTag, "ToggleCheck: Airplane Mode (" + toggleThreshold
									+ " minutes since last successful CheckIn)");
							app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle",
									app.getApplicationContext().getContentResolver());
						}
						break;
					}
					i++;
				}
			}
		}
	}

	public boolean isBatteryChargeSufficientForCheckIn() {
		int batteryCharge = app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= app.rfcxPrefs.getPrefAsInt("checkin_battery_cutoff"));
	}

	public boolean isBatteryChargedButBelowCheckInThreshold() {
		return (app.deviceBattery.isBatteryCharged(app.getApplicationContext(), null)
				&& !isBatteryChargeSufficientForCheckIn());
	}

	private void purgeSingleAsset(String assetType, String rfcxDeviceId, Context context, String assetId, String fileExtension) {

		try {
			String filePath = null;

			if (assetType.equals("audio")) {
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbSkipped.deleteSingleRowByAudioAttachmentId(assetId);
				app.audioEncodeDb.dbEncoded.deleteSingleRow(assetId);
				if(latestCheckInAudioId != null){
					filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(rfcxDeviceId, context,
							(long) Long.parseLong(this.latestCheckInAudioId), fileExtension);
				}
			} else if (assetType.equals("screenshot")) {
				RfcxComm.deleteQueryContentProvider("org.rfcx.org.rfcx.guardian.guardian.admin", "database_delete_row", "screenshots|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePath = DeviceScreenShot.getScreenShotFileLocation_Complete(rfcxDeviceId, context,
						(long) Long.parseLong(assetId));

			} else if (assetType.equals("log")) {
				RfcxComm.deleteQueryContentProvider("org.rfcx.org.rfcx.guardian.guardian.admin", "database_delete_row", "logs|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePath = DeviceLogCat.getLogFileLocation_Complete_PostZip(rfcxDeviceId, context,
						(long) Long.parseLong(assetId));

			} else if (assetType.equals("sms")) {
				RfcxComm.deleteQueryContentProvider("org.rfcx.org.rfcx.guardian.guardian.admin", "database_delete_row", "sms|" + assetId,
						app.getApplicationContext().getContentResolver());

			} else if (assetType.equals("meta")) {
				app.apiCheckInMetaDb.dbMeta.deleteSingleRowByTimestamp(assetId);

				// ONLY TESTING THE EXCHANGE LOG WITH META FOR THE MOMENT

				app.apiAssetExchangeLogDb.dbPurged.insert(assetType, assetId);

			}
			//delete audio file after checkin
			if ((filePath != null) && (new File(filePath)).exists()) {
				(new File(filePath)).delete();
				Log.d(logTag, "Purging asset: " + assetType + ", " + assetId + ( (filePath != null) ? ", "+filePath.substring(1+filePath.lastIndexOf("/")) : "") );
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	public void purgeAllCheckIns() {

		String deviceGuid = app.rfcxDeviceGuid.getDeviceGuid();
		Context context = app.getApplicationContext();
		String defaultAudioCodec = app.rfcxPrefs.getPrefAsString("audio_encode_codec");

		List<String[]> allQueued = app.apiCheckInDb.dbQueued.getAllRows();
		app.apiCheckInDb.dbQueued.deleteAllRows();
		List<String> queuedDeletedList = new ArrayList<String>();

		for (String[] queuedRow : allQueued) {
			String fileTimestamp = queuedRow[1].substring(0, queuedRow[1].indexOf("."));
			String filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(deviceGuid, context,
					(long) Long.parseLong(fileTimestamp), defaultAudioCodec);
			if ((new File(filePath)).exists()) {
				(new File(filePath)).delete();
				queuedDeletedList.add(fileTimestamp);
			}
		}

		Log.v(logTag, "Deleted from Queued: " + TextUtils.join(" ", queuedDeletedList));

		List<String[]> allStashed = app.apiCheckInDb.dbStashed.getAllRows();
		app.apiCheckInDb.dbStashed.deleteAllRows();
		List<String> stashedDeletedList = new ArrayList<String>();

		for (String[] stashedRow : allStashed) {
			String fileTimestamp = stashedRow[1].substring(0, stashedRow[1].indexOf("."));
			String filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(deviceGuid, context,
					(long) Long.parseLong(fileTimestamp), defaultAudioCodec);
			if ((new File(filePath)).exists()) {
				(new File(filePath)).delete();
				stashedDeletedList.add(fileTimestamp);
			}
		}

		Log.v(logTag, "Deleted from Stashed: " + TextUtils.join(" ", stashedDeletedList));

		List<String[]> allSkipped = app.apiCheckInDb.dbSkipped.getAllRows();
		app.apiCheckInDb.dbSkipped.deleteAllRows();
		List<String> skippedDeletedList = new ArrayList<String>();

		for (String[] skippedRow : allSkipped) {
			String fileTimestamp = skippedRow[1].substring(0, skippedRow[1].indexOf("."));
			String filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(deviceGuid, context,
					(long) Long.parseLong(fileTimestamp), defaultAudioCodec);
			if ((new File(filePath)).exists()) {
				(new File(filePath)).delete();
				skippedDeletedList.add(fileTimestamp);
			}
		}

		Log.v(logTag, "Deleted from Skipped: " + TextUtils.join(" ", skippedDeletedList));

		List<String[]> allSent = app.apiCheckInDb.dbSent.getAllRows();
		app.apiCheckInDb.dbSent.deleteAllRows();
		List<String> sentDeletedList = new ArrayList<String>();

		for (String[] sentRow : allSent) {
			String fileTimestamp = sentRow[1].substring(0, sentRow[1].indexOf("."));
			String filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(deviceGuid, context,
					(long) Long.parseLong(fileTimestamp), defaultAudioCodec);
			if ((new File(filePath)).exists()) {
				(new File(filePath)).delete();
				sentDeletedList.add(fileTimestamp);
			}
		}

		Log.v(logTag, "Deleted from Sent: " + TextUtils.join(" ", sentDeletedList));

	}


	private void processAndExecuteInstruction(JSONObject inputInstrObj) {

		try {

			String instrId = inputInstrObj.has("id") ? inputInstrObj.getString("id") : null;
			String instrType = inputInstrObj.has("type") ? inputInstrObj.getString("type") : null;
			JSONObject instrMeta = inputInstrObj.has("meta") ? inputInstrObj.getJSONObject("meta") : null;

			String logMsg = "Instruction: " +instrId + ", " + instrType + ", ";

			// instruction: send message
			if (instrType.equalsIgnoreCase("message_send")) {
				String msgAddress = instrMeta.getString("address");
				String msgBody = instrMeta.getString("body");
				RfcxComm.getQueryContentProvider("org.rfcx.org.rfcx.guardian.guardian.admin", "sms_send", msgAddress + "|" + msgBody, app.getApplicationContext().getContentResolver());
				Log.i(logTag, logMsg + msgAddress + " | " + msgBody);
			}

//			// instructions: prefs
//			if (jsonObj.has("prefs")) {
//				JSONArray instructionPrefsJson = jsonObj.getJSONArray("prefs");
//				for (int i = 0; i < instructionPrefsJson.length(); i++) {
//					JSONObject instructionPrefJson = instructionPrefsJson.getJSONObject(i);
//					// Here we would set preferences...
//				}
//			}

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}

	}

	private void processCheckInResponseMessage(byte[] messagePayload) {

		String jsonStr = StringUtils.UnGZipByteArrayToString(messagePayload);
		Log.i(logTag, "CheckIn: " + jsonStr);
		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			Log.d(logTag,jsonStr);
			// reset/record request latency
			this.requestSendReturned = System.currentTimeMillis();

			// parse audio info and use it to purge the data locally
			if (jsonObj.has("audio")) {
				JSONArray audioJson = jsonObj.getJSONArray("audio");

				for (int i = 0; i < audioJson.length(); i++) {

					String audioId = audioJson.getJSONObject(i).getString("id");
					purgeSingleAsset("audio", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), audioId,
							this.app.rfcxPrefs.getPrefAsString("audio_encode_codec"));
					this.latestCheckInAudioId = audioId;

					if (jsonObj.has("checkin_id")) {
						String checkInId = jsonObj.getString("checkin_id");
						if (checkInId.length() > 0) {
							long[] checkInStats = this.inFlightCheckInStats.get(audioId);

							this.previousCheckIns = new ArrayList<String>();
							this.previousCheckIns.add( TextUtils.join("*", new String[] { checkInId+"", checkInStats[1]+"", checkInStats[2]+"" } ) );
							Calendar rightNow = GregorianCalendar.getInstance();
							rightNow.setTime(new Date());

							runRecentActivityHealthCheck(new long[] {
									/* latency */	checkInStats[1],
									/* queued */		(long) app.apiCheckInDb.dbQueued.getCount(),
									/* recent */		checkInStats[0],
									/* time-of-day */	(long) rightNow.get(Calendar.HOUR_OF_DAY)
								});

						}
					}

					this.inFlightCheckInEntries.remove(audioId);
					this.inFlightCheckInStats.remove(audioId);

				}
			}

			// parse screenshot info and use it to purge the data locally
			if (jsonObj.has("screenshots")) {
				JSONArray screenShotJson = jsonObj.getJSONArray("screenshots");
				for (int i = 0; i < screenShotJson.length(); i++) {
					String screenShotId = screenShotJson.getJSONObject(i).getString("id");
					purgeSingleAsset("screenshot", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), screenShotId, "png");
				}
			}

			// parse log info and use it to purge the data locally
			if (jsonObj.has("logs")) {
				JSONArray logsJson = jsonObj.getJSONArray("logs");
				for (int i = 0; i < logsJson.length(); i++) {
					String logId = logsJson.getJSONObject(i).getString("id");
					purgeSingleAsset("log", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), logId, "log");
				}
			}

			// parse sms info and use it to purge the data locally
			if (jsonObj.has("messages")) {
				JSONArray messagesJson = jsonObj.getJSONArray("messages");
				for (int i = 0; i < messagesJson.length(); i++) {
					String smsId = messagesJson.getJSONObject(i).getString("id");
					purgeSingleAsset("sms", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), smsId, null);
				}
			}

			// parse meta info and use it to purge the data locally
			if (jsonObj.has("meta")) {
				JSONArray metaJson = jsonObj.getJSONArray("meta");
				for (int i = 0; i < metaJson.length(); i++) {
					String metaId = metaJson.getJSONObject(i).getString("id");
					purgeSingleAsset("meta", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), metaId, null);
				}
			}

			// parse instruction info and execute
			if (jsonObj.has("instructions")) {
				JSONArray instructionsJson = jsonObj.getJSONArray("instructions");
				for (int i = 0; i < instructionsJson.length(); i++) {
					processAndExecuteInstruction(instructionsJson.getJSONObject(i));
				}
			}

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}

	}

	private void moveCheckInEntryToSentDatabase(String inFlightCheckInAudioId) {

		if ((this.inFlightCheckInEntries.get(inFlightCheckInAudioId) != null) && (this.inFlightCheckInEntries.get(inFlightCheckInAudioId)[0] != null)) {
			String[] checkInEntry = this.inFlightCheckInEntries.get(inFlightCheckInAudioId);
			//delete latest instead to keep present info
			if(latestCheckInAudioId != null){
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(this.latestCheckInAudioId);
			}
			app.apiCheckInDb.dbSent.insert(checkInEntry[1], checkInEntry[2], checkInEntry[3], checkInEntry[4]);
			app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInEntry[1]);
			Log.d(logTag, "sent checkin to db :" + Arrays.toString(checkInEntry));
		}
	}

	private void setInFlightCheckInStats(String keyId, long msgSendStart, long msgSendDuration, long msgPayloadSize) {
		long[] stats = this.inFlightCheckInStats.get(keyId);
		if (stats == null) { stats = new long[] { 0, 0, 0 }; }
		if (msgSendStart != 0) { stats[0] = msgSendStart; }
		if (msgSendDuration != 0) { stats[1] = msgSendDuration; }
		if (msgPayloadSize != 0) { stats[2] = msgPayloadSize; }
		this.inFlightCheckInStats.remove(keyId);
		this.inFlightCheckInStats.put(keyId, stats);
	}

	@Override
	public void messageArrived(String messageTopic, MqttMessage mqttMessage) throws Exception {

		// this is a checkin response message
		if (messageTopic.equalsIgnoreCase(this.subscribeBaseTopic + "/checkins")) {
			processCheckInResponseMessage(mqttMessage.getPayload());

			// this is an instruction message
		}/* else if (messageTopic.equalsIgnoreCase(this.subscribeBaseTopic + "/instructions")) {
			processInstructionMessage(mqttMessage.getPayload());

		}*/
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryToken) {

		try {
			moveCheckInEntryToSentDatabase(this.inFlightCheckInAudioId);

			long msgSendDuration = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(this.inFlightCheckInStats.get(this.inFlightCheckInAudioId)[0]));

			setInFlightCheckInStats(this.inFlightCheckInAudioId, 0, msgSendDuration, 0);

			Log.i(logTag, (new StringBuilder())
							.append("CheckIn delivery time: ")
							.append(DateTimeUtils.milliSecondDurationAsReadableString(msgSendDuration, true))
							.toString());

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

	}

	@Override
	public void connectionLost(Throwable cause) {
		try {
			Log.e(logTag, (new StringBuilder()).append("Connection lost. ")
						.append(DateTimeUtils.timeStampDifferenceFromNowAsReadableString(this.inFlightCheckInStats.get(this.inFlightCheckInAudioId)[0]))
						.append(" since last CheckIn publication was launched").toString());
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

		RfcxLog.logThrowable(logTag, cause);

		confirmOrCreateConnectionToBroker();

	}

	public void confirmOrCreateConnectionToBroker() {

		long minDelayBetweenConnectionAttempts = 10000;

		if (mqttCheckInClient.mqttBrokerConnectionLastAttemptedAt < (app.deviceConnectivity.lastConnectedAt() - minDelayBetweenConnectionAttempts)) {
			try {
				mqttCheckInClient.confirmOrCreateConnectionToBroker(this.app.deviceConnectivity.isConnected());
				if (mqttCheckInClient.mqttBrokerConnectionLatency > 0) {
					Log.v(logTag, "MQTT Connection Latency: "+mqttCheckInClient.mqttBrokerConnectionLatency+" ms");
					app.deviceSystemDb.dbMqttBrokerConnections.insert(new Date(),
													mqttCheckInClient.mqttBrokerConnectionLatency,
													app.rfcxPrefs.getPrefAsString("api_checkin_protocol"),
													app.rfcxPrefs.getPrefAsString("api_checkin_host"),
													app.rfcxPrefs.getPrefAsInt("api_checkin_port"));
				}
			} catch (MqttException e) {
				RfcxLog.logExc(logTag, e);
			}
		} else {
//			Log.e(logTag, "Last connection attempt was less than " + minDelayBetweenConnectionAttempts + "ms ago");
		}
	}

}
