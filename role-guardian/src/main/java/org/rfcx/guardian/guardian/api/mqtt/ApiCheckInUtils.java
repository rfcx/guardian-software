package org.rfcx.guardian.guardian.api.mqtt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.rfcx.guardian.guardian.socket.SocketManager;
import org.rfcx.guardian.utility.camera.RfcxCameraUtils;
import org.rfcx.guardian.utility.device.capture.DeviceDiskUsage;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceLogCat;
import org.rfcx.guardian.utility.device.capture.DeviceScreenShot;
import org.rfcx.guardian.utility.device.hardware.DeviceHardwareUtils;
import org.rfcx.guardian.utility.network.MqttUtils;
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

		this.mqttCheckInClient = new MqttUtils(context, RfcxGuardian.APP_ROLE, this.app.rfcxGuardianIdentity.getGuid());
		this.mqttCheckInClient.addSubscribeTopic(this.app.rfcxGuardianIdentity.getGuid()+"/cmd");

		setOrResetBrokerConfig();
		this.mqttCheckInClient.setCallback(this);
		this.mqttCheckInClient.setActionTimeout(this.requestTimeOutLength);

		confirmOrCreateConnectionToBroker(true);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInUtils");

	private RfcxGuardian app;
	private MqttUtils mqttCheckInClient = null;

	private long requestTimeOutLength = 0;

	private long requestSendReturned = System.currentTimeMillis();

	private String inFlightCheckInAudioId = null;
	private String latestCheckInAudioId = null;
	private Map<String, String[]> inFlightCheckInEntries = new HashMap<String, String[]>();
	private Map<String, long[]> inFlightCheckInStats = new HashMap<String, long[]>();

	private int inFlightCheckInAttemptCounter = 0;
	private int inFlightCheckInAttemptCounterLimit = 5;

	private int[] failedCheckInThresholds = new int[0];
	private boolean[] failedCheckInThresholdsReached = new boolean[0];

	private ApiCheckInHealthUtils apiCheckInHealthUtils = new ApiCheckInHealthUtils();

	public String prefsSha1FullApiSync = null;
	public long prefsTimestampLastFullApiSync = 0;

	public static long latestFileSize = 0;
	public static long totalFileSize = 0;
	public static String latestCheckinTimestamp = "";
	public static int totalSyncedAudio = 0;

	public void setOrResetBrokerConfig() {
		String[] authUserPswd = app.rfcxPrefs.getPrefAsString("api_checkin_auth_creds").split(",");
		String authUser = !app.rfcxPrefs.getPrefAsBoolean("enable_checkin_auth") ? null : authUserPswd[0];
		String authPswd = !app.rfcxPrefs.getPrefAsBoolean("enable_checkin_auth") ? null : authUserPswd[1];
		this.mqttCheckInClient.setOrResetBroker(
			this.app.rfcxPrefs.getPrefAsString("api_checkin_protocol"),
			this.app.rfcxPrefs.getPrefAsInt("api_checkin_port"),
			this.app.rfcxPrefs.getPrefAsString("api_checkin_host"),
			this.app.rfcxGuardianIdentity.getKeystorePassphrase(),
			!authUser.equalsIgnoreCase("[guid]") ? authUser : app.rfcxGuardianIdentity.getGuid(),
			!authPswd.equalsIgnoreCase("[token]") ? authPswd : app.rfcxGuardianIdentity.getAuthToken()
		);
	}

	boolean addCheckInToQueue(String[] audioInfo, String filePath) {

		// serialize audio info into JSON for checkin queue insertion
		String queueJson = generateCheckInQueueJson(audioInfo);

		long audioFileSize = FileUtils.getFileSizeInBytes(filePath);

		// add audio info to checkin queue
		int queuedCount = app.apiCheckInDb.dbQueued.insert(audioInfo[1] + "." + audioInfo[2], queueJson, "0", filePath, audioInfo[10], audioFileSize+"");

		long queuedLimitMb = app.rfcxPrefs.getPrefAsLong("checkin_queue_filesize_limit");
		long queuedLimitPct = Math.round(Math.floor(100*(Double.parseDouble(app.apiCheckInDb.dbQueued.getCumulativeFileSizeForAllRows()+"")/(queuedLimitMb*1024*1024))));

		Log.d(logTag, "Queued " + audioInfo[1] + ", " + FileUtils.bytesAsReadableString(audioFileSize)
						+  " (" + queuedCount + " in queue, "+queuedLimitPct+"% of "+queuedLimitMb+" MB limit) " + filePath);

		// once queued, remove database reference from encode role
		app.audioEncodeDb.dbEncoded.deleteSingleRow(audioInfo[1]);

		return true;
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

	void stashOrArchiveOldestCheckIns() {

		long queueFileSizeLimitInBytes = app.rfcxPrefs.getPrefAsLong("checkin_queue_filesize_limit")*1024*1024;
		long stashFileSizeBufferInBytes = app.rfcxPrefs.getPrefAsLong("checkin_stash_filesize_buffer")*1024*1024;
		long archiveFileSizeTargetInBytes = app.rfcxPrefs.getPrefAsLong("checkin_archive_filesize_target")*1024*1024;

		if (app.apiCheckInDb.dbQueued.getCumulativeFileSizeForAllRows() >= queueFileSizeLimitInBytes) {

			long queuedFileSizeSumBeforeLimit = 0;
			int queuedCountBeforeLimit = 0;

			for (String[] checkInCycle : app.apiCheckInDb.dbQueued.getRowsWithOffset(0,5000)) {
				queuedFileSizeSumBeforeLimit += Long.parseLong(checkInCycle[6]);
				if (queuedFileSizeSumBeforeLimit >= queueFileSizeLimitInBytes) { break; }
				queuedCountBeforeLimit++;
			}

			// string list for reporting stashed checkins to the log
			List<String> stashSuccessList = new ArrayList<String>();
			List<String> stashFailureList = new ArrayList<String>();
			int stashCount = 0;

			// cycle through stashable checkins and move them to the new table/database
			for (String[] checkInsToStash : app.apiCheckInDb.dbQueued.getRowsWithOffset(queuedCountBeforeLimit, 16)) {

				if (!DeviceDiskUsage.isExternalStorageWritable()) {
					stashFailureList.add(checkInsToStash[1]);

				} else {
					String stashFilePath = RfcxAudioUtils.getAudioFileLocation_ExternalStorage(
							app.rfcxGuardianIdentity.getGuid(),
							Long.parseLong(checkInsToStash[1].substring(0, checkInsToStash[1].lastIndexOf("."))),
							checkInsToStash[1].substring(checkInsToStash[1].lastIndexOf(".") + 1));
					try {
						FileUtils.copy(checkInsToStash[4], stashFilePath);
					} catch (IOException e) {
						RfcxLog.logExc(logTag, e);
					}

					if (FileUtils.exists(stashFilePath) && (FileUtils.getFileSizeInBytes(stashFilePath) == Long.parseLong(checkInsToStash[6]))) {
						stashCount = app.apiCheckInDb.dbStashed.insert(checkInsToStash[1], checkInsToStash[2], checkInsToStash[3], stashFilePath, checkInsToStash[5], checkInsToStash[6]);
						stashSuccessList.add(checkInsToStash[1]);
					} else {
						stashFailureList.add(checkInsToStash[1]);
					}
				}

				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInsToStash[1]);
				FileUtils.delete(checkInsToStash[4]);
			}

			if (stashFailureList.size() > 0) {
				Log.e(logTag, stashFailureList.size() + " CheckIn(s) failed to be Stashed (" + TextUtils.join(" ", stashFailureList) + ").");
			}

			if (stashSuccessList.size() > 0) {
				Log.i(logTag, stashSuccessList.size() + " CheckIn(s) moved to Stash (" + TextUtils.join(" ", stashSuccessList) + "). Total in Stash: " + stashCount + " CheckIns, " + Math.round(app.apiCheckInDb.dbStashed.getCumulativeFileSizeForAllRows() / 1024) + " kB.");
			}
		}

		if (app.apiCheckInDb.dbStashed.getCumulativeFileSizeForAllRows() >= (stashFileSizeBufferInBytes + archiveFileSizeTargetInBytes)) {
			app.rfcxServiceHandler.triggerService("ApiCheckInArchive", false);
		}
	}

	void skipSingleCheckIn(String[] checkInToSkip) {

		String skipFilePath = RfcxAudioUtils.getAudioFileLocation_ExternalStorage(
				app.rfcxGuardianIdentity.getGuid(),
				Long.parseLong(checkInToSkip[1].substring(0, checkInToSkip[1].lastIndexOf("."))),
				checkInToSkip[1].substring(checkInToSkip[1].lastIndexOf(".") + 1));

		try {
			FileUtils.copy(checkInToSkip[4], skipFilePath);
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}

		if (FileUtils.exists(skipFilePath) && (FileUtils.getFileSizeInBytes(skipFilePath) == Long.parseLong(checkInToSkip[6]))) {
			app.apiCheckInDb.dbSkipped.insert(checkInToSkip[0], checkInToSkip[1], checkInToSkip[2], checkInToSkip[3], skipFilePath, checkInToSkip[5], checkInToSkip[6]);
		}

		app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInToSkip[1]);
		FileUtils.delete(checkInToSkip[4]);

	}

	private void reQueueAudioAssetForCheckIn(String checkInStatus, String audioId) {

		boolean isReQueued = false;
		String[] checkInToReQueue = new String[] {};

		// fetch check-in entry from relevant table, if it exists...
		if (checkInStatus.equalsIgnoreCase("sent")) {
			checkInToReQueue = app.apiCheckInDb.dbSent.getSingleRowByAudioAttachmentId(audioId);
		} else if (checkInStatus.equalsIgnoreCase("stashed")) {
			checkInToReQueue = app.apiCheckInDb.dbStashed.getSingleRowByAudioAttachmentId(audioId);
		} else if (checkInStatus.equalsIgnoreCase("skipped")) {
			checkInToReQueue = app.apiCheckInDb.dbSkipped.getSingleRowByAudioAttachmentId(audioId);
		}

		// if this array has been populated, indicating that the source row exists, then add entry to checkin table
		if ((checkInToReQueue.length > 0) && (checkInToReQueue[0] != null)) {


			int queuedCount = app.apiCheckInDb.dbQueued.insert(checkInToReQueue[1], checkInToReQueue[2], checkInToReQueue[3], checkInToReQueue[4], checkInToReQueue[5], checkInToReQueue[6]);
			String[] reQueuedCheckIn = app.apiCheckInDb.dbQueued.getSingleRowByAudioAttachmentId(audioId);

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
			Log.i(logTag, "CheckIn Successfully ReQueued: "+checkInStatus+", "+audioId);
		} else {
			Log.e(logTag, "CheckIn Failed to ReQueue: "+checkInStatus+", "+audioId);
		}

	}

	private void reQueueStashedCheckInIfAllowedByHealthCheck(long[] currentCheckInStats) {

		if (	apiCheckInHealthUtils.validateRecentCheckInHealthCheck(
					app.rfcxPrefs.getPrefAsLong("audio_cycle_duration"),
					app.rfcxPrefs.getPrefAsString("checkin_requeue_bounds_hours"),
					currentCheckInStats
				)
			&& 	(app.apiCheckInDb.dbStashed.getCount() > 0)
		) {
			reQueueAudioAssetForCheckIn("stashed", app.apiCheckInDb.dbStashed.getLatestRow()[1]);
		}
	}

	void createSystemMetaDataJsonSnapshot() throws JSONException {

		JSONObject metaDataJsonObj = new JSONObject();
		Date metaQueryTimestampObj = new Date();
		long metaQueryTimestamp = metaQueryTimestampObj.getTime();

		JSONArray metaIds = new JSONArray();
		metaIds.put(metaQueryTimestamp);
		metaDataJsonObj.put("meta_ids", metaIds);
		metaDataJsonObj.put("measured_at", metaQueryTimestamp);

		metaDataJsonObj.put("broker_connections", app.deviceSystemDb.dbMqttBrokerConnections.getConcatRows());
		metaDataJsonObj.put("datetime_offsets", app.deviceSystemDb.dbDateTimeOffsets.getConcatRows());

		// Adding connection data from previous checkins
		metaDataJsonObj.put("previous_checkins", app.apiCheckInStatsDb.dbStats.getConcatRows());

		// Adding system metadata, if they can be retrieved from admin role via content provider
		JSONArray systemMetaJsonArray = RfcxComm.getQueryContentProvider("admin", "database_get_all_rows",
				"system_meta", app.getApplicationContext().getContentResolver());
		metaDataJsonObj = addConcatSystemMetaParams(metaDataJsonObj, systemMetaJsonArray);

		// Adding sentinel power data, if they can be retrieved from admin role via content provider
		JSONArray sentinelPowerJsonArray = RfcxComm.getQueryContentProvider("admin", "database_get_all_rows",
				"sentinel_power", app.getApplicationContext().getContentResolver());
		metaDataJsonObj.put("sentinel_power", getConcatSentinelMeta(sentinelPowerJsonArray));

		// Adding sentinel sensor data, if they can be retrieved from admin role via content provider
		JSONArray sentinelSensorJsonArray = RfcxComm.getQueryContentProvider("admin", "database_get_all_rows",
				"sentinel_sensor", app.getApplicationContext().getContentResolver());
		metaDataJsonObj.put("sentinel_sensor", getConcatSentinelMeta(sentinelSensorJsonArray));

		// Saves JSON snapshot blob to database
		app.apiCheckInMetaDb.dbMeta.insert(metaQueryTimestamp, metaDataJsonObj.toString());

		clearPrePackageMetaData(metaQueryTimestampObj);

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

	private String getAssetExchangeLogList(String assetStatus, int rowLimit) {

		List<String[]> assetRows = new ArrayList<String[]>();
		if (assetStatus.equalsIgnoreCase("purged")) {
			assetRows = app.apiAssetExchangeLogDb.dbPurged.getLatestRowsWithLimitExcludeCreatedAt(rowLimit);
		}
		return DbUtils.getConcatRows(assetRows);
	}

	private String getCheckInStatusInfoForJson(boolean includeAssetIdLists) {

		StringBuilder sentIdList = new StringBuilder();
		if (includeAssetIdLists) {
			long reportAssetIdIfOlderThan = 4 * this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
			for (String[] _checkIn : app.apiCheckInDb.dbSent.getLatestRowsWithLimit(15)) {
				String assetId = _checkIn[1].substring(0, _checkIn[1].lastIndexOf("."));
				if (reportAssetIdIfOlderThan < Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(Long.parseLong(assetId)))) {
					sentIdList.append("*").append(assetId);
				}
			}
		}

		return TextUtils.join("|",
				new String[] {
					"sent*" + app.apiCheckInDb.dbSent.getCount() + sentIdList.toString(),
					"queued*" + app.apiCheckInDb.dbQueued.getCount(),
					"meta*" + app.apiCheckInMetaDb.dbMeta.getCount(),
					"skipped*" + app.apiCheckInDb.dbSkipped.getCount(),
					"stashed*" + app.apiCheckInDb.dbStashed.getCount(),
					"archived*" + app.archiveDb.dbCheckInArchive.getInnerRecordCumulativeCount()
				});
	}


	private JSONObject buildCheckInPrefsJsonObj() {

		JSONObject prefsObj = new JSONObject();
		try {

			long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(this.prefsTimestampLastFullApiSync));
			String prefsSha1 = app.rfcxPrefs.getPrefsChecksum();
			prefsObj.put("sha1", prefsSha1);

			if (	(this.prefsSha1FullApiSync != null)
				&&	!this.prefsSha1FullApiSync.equalsIgnoreCase(prefsSha1)
				&& 	(milliSecondsSinceAccessed > this.requestTimeOutLength)
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

		int maxMetaRowsToBundle = this.app.rfcxPrefs.getPrefAsInt("checkin_meta_bundle_limit");

		JSONObject metaJsonBundledSnapshotsObj = null;
		JSONArray metaJsonBundledSnapshotsIds = new JSONArray();
		long metaMeasuredAtValue = 0;

		for (String[] metaRow : app.apiCheckInMetaDb.dbMeta.getLatestRowsWithLimit(2 * maxMetaRowsToBundle)) {

			long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(Long.parseLong(metaRow[3])));

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
				app.apiCheckInMetaDb.dbMeta.updateLastAccessedAtByTimestamp(metaRow[1]);

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

	private String buildCheckInJson(String checkInJsonString, String[] screenShotMeta, String[] logFileMeta, String[] photoFileMeta, String[] videoFileMeta) throws JSONException, IOException {

		JSONObject checkInMetaJson = retrieveAndBundleMetaJson();

		// Adding Audio JSON fields from checkin table
		JSONObject checkInJsonObj = new JSONObject(checkInJsonString);
		checkInMetaJson.put("queued_at", checkInJsonObj.getLong("queued_at"));
		checkInMetaJson.put("audio", checkInJsonObj.getString("audio"));

		// Recording number of currently queued/skipped/stashed checkins
		checkInMetaJson.put("checkins", getCheckInStatusInfoForJson(true));

		checkInMetaJson.put("assets_purged", getAssetExchangeLogList("purged", 12));

		// Device: Phone & Android info
		JSONObject deviceJsonObj = new JSONObject();
		deviceJsonObj.put("phone", app.deviceMobilePhone.getMobilePhoneInfoJson());
		deviceJsonObj.put("android", DeviceHardwareUtils.getInfoAsJson());
		checkInMetaJson.put("device", deviceJsonObj);

		// Adding software role versions
		checkInMetaJson.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));

		// Adding checksum of current prefs values
		checkInMetaJson.put("prefs", buildCheckInPrefsJsonObj());

		// Adding device location timezone offset
		checkInMetaJson.put("datetime", TextUtils.join("|", new String[] { "system*"+System.currentTimeMillis(), "timezone*"+DateTimeUtils.getTimeZoneOffset() }));

		// Adding instructions, if there are any
		if (app.instructionsUtils.getInstructionsCount() > 0) {
			checkInMetaJson.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
		}

		// Adding messages to JSON blob
		checkInMetaJson.put("messages", RfcxComm.getQueryContentProvider("admin", "database_get_all_rows", "sms", app.getApplicationContext().getContentResolver()));

		// Adding screenshot meta to JSON blob
		checkInMetaJson.put("screenshots",  (screenShotMeta[0] == null) ? "" :
				TextUtils.join("*", new String[]{screenShotMeta[1], screenShotMeta[2], screenShotMeta[3], screenShotMeta[4], screenShotMeta[5], screenShotMeta[6] })
		);

		// Adding logs meta to JSON blob
		checkInMetaJson.put("logs", (logFileMeta[0] == null) ? "" :
				TextUtils.join("*", new String[] { logFileMeta[1], logFileMeta[2], logFileMeta[3], logFileMeta[4] })
		);

		// Adding photos meta to JSON blob
		checkInMetaJson.put("photos",(photoFileMeta[0] == null) ? "" :
                TextUtils.join("*", new String[] { photoFileMeta[1], photoFileMeta[2], photoFileMeta[3], photoFileMeta[4], photoFileMeta[5], photoFileMeta[6] })
        );

		// Adding videos meta to JSON blob
		checkInMetaJson.put("videos",(videoFileMeta[0] == null) ? "" :
				TextUtils.join("*", new String[] { videoFileMeta[1], videoFileMeta[2], videoFileMeta[3], videoFileMeta[4], videoFileMeta[5], videoFileMeta[6] })
		);

		Log.d(logTag,checkInMetaJson.toString());

        // Adding Guardian GUID and Auth Token
        JSONObject guardianObj = new JSONObject();
        guardianObj.put("guid", this.app.rfcxGuardianIdentity.getGuid());
        guardianObj.put("token", this.app.rfcxGuardianIdentity.getAuthToken());
        checkInMetaJson.put("guardian", guardianObj);

		return checkInMetaJson.toString();

	}

	private byte[] packageMqttCheckInPayload(String checkInJsonString, String checkInAudioFilePath)
			throws UnsupportedEncodingException, IOException, JSONException {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		String[] screenShotMeta = getLatestExternalAssetMeta("screenshots");
		if ((screenShotMeta[0] != null) && !FileUtils.exists(screenShotMeta[0])) {
			purgeSingleAsset("screenshot", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), screenShotMeta[2]);
		}

		String[] logFileMeta = getLatestExternalAssetMeta("logs");
		if ((logFileMeta[0] != null) && !FileUtils.exists(logFileMeta[0])) {
			purgeSingleAsset("log", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), logFileMeta[2]);
		}

        String[] photoFileMeta = getLatestExternalAssetMeta("photos");
        if ((photoFileMeta[0] != null) && !FileUtils.exists(photoFileMeta[0])) {
            purgeSingleAsset("photo", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), photoFileMeta[2]);
        }

		String[] videoFileMeta = getLatestExternalAssetMeta("videos");
		if ((videoFileMeta[0] != null) && !FileUtils.exists(videoFileMeta[0])) {
			purgeSingleAsset("video", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), videoFileMeta[2]);
		}

        // Build JSON blob from included assets
		byte[] jsonBlobAsBytes = StringUtils.gZipStringToByteArray(buildCheckInJson(checkInJsonString, screenShotMeta, logFileMeta, photoFileMeta, videoFileMeta));
		String jsonBlobMetaSection = String.format(Locale.US, "%012d", jsonBlobAsBytes.length);
		byteArrayOutputStream.write(jsonBlobMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(jsonBlobAsBytes);

		byte[] audioFileAsBytes = new byte[0];
		if (FileUtils.exists(checkInAudioFilePath)) {
			audioFileAsBytes = FileUtils.fileAsByteArray(checkInAudioFilePath);
		}
		String audioFileMetaSection = String.format(Locale.US, "%012d", audioFileAsBytes.length);
		byteArrayOutputStream.write(audioFileMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(audioFileAsBytes);

		byte[] screenShotFileAsBytes = new byte[0];
		if (FileUtils.exists(screenShotMeta[0])) {
			screenShotFileAsBytes = FileUtils.fileAsByteArray(screenShotMeta[0]);
		}
		String screenShotFileMetaSection = String.format(Locale.US, "%012d", screenShotFileAsBytes.length);
		byteArrayOutputStream.write(screenShotFileMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(screenShotFileAsBytes);

		byte[] logFileAsBytes = new byte[0];
		if (FileUtils.exists(logFileMeta[0])) {
			logFileAsBytes = FileUtils.fileAsByteArray(logFileMeta[0]);
		}
		String logFileMetaSection = String.format(Locale.US, "%012d", logFileAsBytes.length);
		byteArrayOutputStream.write(logFileMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(logFileAsBytes);

        byte[] photoFileAsBytes = new byte[0];
        if (FileUtils.exists(photoFileMeta[0])) {
            photoFileAsBytes = FileUtils.fileAsByteArray(photoFileMeta[0]);
        }
        String photoFileMetaSection = String.format(Locale.US, "%012d", photoFileAsBytes.length);
        byteArrayOutputStream.write(photoFileMetaSection.getBytes(StandardCharsets.UTF_8));
        byteArrayOutputStream.write(photoFileAsBytes);

		byte[] videoFileAsBytes = new byte[0];
		if (FileUtils.exists(videoFileMeta[0])) {
			videoFileAsBytes = FileUtils.fileAsByteArray(videoFileMeta[0]);
		}
		String videoFileMetaSection = String.format(Locale.US, "%012d", videoFileAsBytes.length);
		byteArrayOutputStream.write(videoFileMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(videoFileAsBytes);

		byteArrayOutputStream.close();

		return byteArrayOutputStream.toByteArray();
	}

	void sendMqttCheckIn(String[] checkInDbEntry) {

		String audioId = checkInDbEntry[1].substring(0, checkInDbEntry[1].lastIndexOf("."));
		String audioPath = checkInDbEntry[4];
		String audioJson = checkInDbEntry[2];

		try {

			if (FileUtils.exists(audioPath)) {

				byte[] checkInPayload = packageMqttCheckInPayload(audioJson, audioPath);

				this.inFlightCheckInAudioId = audioId;
				this.inFlightCheckInEntries.remove(audioId);
				this.inFlightCheckInEntries.put(audioId, checkInDbEntry);
				this.inFlightCheckInAttemptCounter++;

				app.apiCheckInDb.dbQueued.incrementSingleRowAttempts(audioId);
				long msgSendStart = publishMessageOnConfirmedConnection("guardians/checkins", checkInPayload);

				setInFlightCheckInStats(audioId, msgSendStart, 0, checkInPayload.length);
				this.inFlightCheckInAttemptCounter = 0;

			} else {
				purgeSingleAsset("audio", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), audioId);
			}

		} catch (Exception e) {

			RfcxLog.logExc(logTag, e);
			handleCheckInPublicationExceptions(e, audioId);
		}
	}

	private void handleCheckInPublicationExceptions(Exception inputExc, String audioId) {

		try {
			String excStr = RfcxLog.getExceptionContentAsString(inputExc);

			boolean isTimedOut = excStr.contains("Timed out waiting for a response from the server");
			boolean tooManyPublishes = excStr.contains("Too many publishes in progress");

			if (	excStr.contains("UnknownHostException")
				||	excStr.contains("Broken pipe")
				||	excStr.contains("No route to host")
				||	excStr.contains("Host is unresolved")
				||	excStr.contains("Unable to connect to server")
				||	tooManyPublishes
				||	isTimedOut
			) {

				if (!isTimedOut) {
					Log.v(logTag, "Connection has failed " + this.inFlightCheckInAttemptCounter + " times (max: " + this.inFlightCheckInAttemptCounterLimit + ")");
					app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
				}

				if (this.inFlightCheckInAttemptCounter >= this.inFlightCheckInAttemptCounterLimit){
					Log.v(logTag, "Max Connection Failure Loop Reached: Airplane Mode will be toggled.");
					app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle", app.getApplicationContext().getContentResolver());
					this.inFlightCheckInAttemptCounter = 0;
				}

				if (isTimedOut || (tooManyPublishes && (this.inFlightCheckInAttemptCounter > 1))) {
					Log.v(logTag, "Kill ApiCheckInJob Service, Close MQTT Connection & Re-Connect");
					app.rfcxServiceHandler.stopService("ApiCheckInQueue");
					this.mqttCheckInClient.closeConnection();
					confirmOrCreateConnectionToBroker(true);
				}

			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	private String[] getLatestExternalAssetMeta(String assetType) {

		String[] assetMeta = new String[] { null };
		try {
			JSONArray latestAssetMetaArray = RfcxComm.getQueryContentProvider("admin", "database_get_latest_row",
					assetType, app.getApplicationContext().getContentResolver());
			for (int i = 0; i < latestAssetMetaArray.length(); i++) {
				JSONObject latestAssetMeta = latestAssetMetaArray.getJSONObject(i);
				long milliSecondsSinceAccessed = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(Long.parseLong(latestAssetMeta.getString("last_accessed_at"))));
				if (milliSecondsSinceAccessed > this.requestTimeOutLength) {
					assetMeta = new String[] { latestAssetMeta.getString("filepath"),
							latestAssetMeta.getString("created_at"), latestAssetMeta.getString("timestamp"),
							latestAssetMeta.getString("format"), latestAssetMeta.getString("digest"),
							latestAssetMeta.getString("width"), latestAssetMeta.getString("height") };
					RfcxComm.updateQueryContentProvider("admin", "database_set_last_accessed_at", assetType + "|" + latestAssetMeta.getString("timestamp"),
							app.getApplicationContext().getContentResolver());
					break;
				} else {
					Log.e(logTag,"Skipping asset attachment: "+assetType+", "+latestAssetMeta.getString("timestamp")+" was last sent only "+Math.round(milliSecondsSinceAccessed / 1000)+" seconds ago.");
				}
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return assetMeta;
	}

	public void initializeFailedCheckInThresholds() {

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

	void updateFailedCheckInThresholds() {

		if (this.failedCheckInThresholds.length > 0) {

			int minsSinceSuccess = (int) Math.floor(((System.currentTimeMillis() - this.requestSendReturned) / 1000) / 60);
			int minsSinceConnected = (int) Math.floor(((System.currentTimeMillis() - app.deviceConnectivity.lastConnectedAt()) / 1000) / 60);

			if (	// ...we haven't yet reached the first threshold for bad connectivity
					(minsSinceSuccess < this.failedCheckInThresholds[0])
					// OR... we are explicitly in offline mode
					|| !app.rfcxPrefs.getPrefAsBoolean("enable_checkin_publish")
					// OR... checkins are explicitly paused due to low battery level
					|| !isBatteryChargeSufficientForCheckIn()
					// OR... this is likely the first checkin after a period of disconnection
				//	|| (app.deviceConnectivity.isConnected() && (minsSinceConnected < this.failedCheckInThresholds[0]))
			) {
				for (int i = 0; i < this.failedCheckInThresholdsReached.length; i++) {
					this.failedCheckInThresholdsReached[i] = false;
				}
			} else {
				int j = 0;
				for (int toggleThreshold : this.failedCheckInThresholds) {
                    if ((minsSinceSuccess >= toggleThreshold) && !this.failedCheckInThresholdsReached[j]) {
                        this.failedCheckInThresholdsReached[j] = true;
                        if (toggleThreshold == this.failedCheckInThresholds[this.failedCheckInThresholds.length - 1]) {
                            // last threshold
                            if (!app.deviceConnectivity.isConnected() && !app.deviceMobilePhone.hasSim()) {
                                Log.d(logTag, "Failure Threshold Reached: Forced reboot due to missing SIM card (" + toggleThreshold
                                        + " minutes since last successful CheckIn)");
                                app.deviceControlUtils.runOrTriggerDeviceControl("reboot",
                                        app.getApplicationContext().getContentResolver());
                            } else {
                                Log.d(logTag, "Failure Threshold Reached: Forced Relaunch (" + toggleThreshold
                                        + " minutes since last successful CheckIn)");
                                app.deviceControlUtils.runOrTriggerDeviceControl("relaunch",
                                        app.getApplicationContext().getContentResolver());

								for (int i = 0; i < this.failedCheckInThresholdsReached.length; i++) {
									this.failedCheckInThresholdsReached[i] = false;
								}
								this.inFlightCheckInAttemptCounter = 0;
                            }
                        } else { //} else if (!app.deviceConnectivity.isConnected()) {
                            // any threshold // and not connected
                            Log.d(logTag, "Failure Threshold Reached: Airplane Mode (" + toggleThreshold
                                    + " minutes since last successful CheckIn)");
                            app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle",
                                    app.getApplicationContext().getContentResolver());
                            this.inFlightCheckInAttemptCounter = 0;
                        }
                        break;
                    }
					j++;
				}
			}
		}
	}

	boolean isBatteryChargeSufficientForCheckIn() {
		int batteryCharge = app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= app.rfcxPrefs.getPrefAsInt("checkin_battery_cutoff"));
	}

	boolean isBatteryChargedButBelowCheckInThreshold() {
		return (app.deviceBattery.isBatteryCharged(app.getApplicationContext(), null)
				&& !isBatteryChargeSufficientForCheckIn());
	}

	private void purgeSingleAsset(String assetType, String rfcxDeviceId, Context context, String assetId) {

		try {
			List<String> filePaths =  new ArrayList<String>();

			if (assetType.equals("audio")) {
				app.audioEncodeDb.dbEncoded.deleteSingleRow(assetId);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbSkipped.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(assetId);
				app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(assetId);
				for (String fileExtension : new String[] { "opus", "flac" }) {
					filePaths.add(RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(rfcxDeviceId, context, Long.parseLong(assetId), fileExtension));
					filePaths.add(RfcxAudioUtils.getAudioFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId), fileExtension));
				}

			} else if (assetType.equals("screenshot")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "screenshots|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePaths.add(DeviceScreenShot.getScreenShotFileLocation_Complete(rfcxDeviceId, context, Long.parseLong(assetId)));
				filePaths.add(DeviceScreenShot.getScreenShotFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId)));

			} else if (assetType.equals("photo")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "photos|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePaths.add(RfcxCameraUtils.getPhotoFileLocation_Complete_PostGZip(rfcxDeviceId, context, Long.parseLong(assetId)));
				filePaths.add(RfcxCameraUtils.getPhotoFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId)));

			} else if (assetType.equals("video")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "videos|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePaths.add(RfcxCameraUtils.getVideoFileLocation_Complete_PostGZip(rfcxDeviceId, context, Long.parseLong(assetId)));
				filePaths.add(RfcxCameraUtils.getVideoFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId)));

			} else if (assetType.equals("log")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "logs|" + assetId,
						app.getApplicationContext().getContentResolver());
				filePaths.add(DeviceLogCat.getLogFileLocation_Complete_PostZip(rfcxDeviceId, context, Long.parseLong(assetId)));
				filePaths.add(DeviceLogCat.getLogFileLocation_ExternalStorage(rfcxDeviceId, Long.parseLong(assetId)));

			} else if (assetType.equals("sms")) {
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "sms|" + assetId,
						app.getApplicationContext().getContentResolver());

			} else if (assetType.equals("meta")) {
				app.apiCheckInMetaDb.dbMeta.deleteSingleRowByTimestamp(assetId);
				app.apiAssetExchangeLogDb.dbPurged.insert(assetType, assetId);

			} else if (assetType.equals("instruction")) {
				app.instructionsDb.dbExecutedInstructions.deleteSingleRowById(assetId);
				app.instructionsDb.dbQueuedInstructions.deleteSingleRowById(assetId);

			}

			boolean isPurgeReported = false;
			// delete asset file after it has been purged from records
			for (String filePath : filePaths) {
				if ((filePath != null) && (new File(filePath)).exists()) {
					latestFileSize = (new File(filePath)).length();
					totalFileSize += latestFileSize;
//					latestCheckinTimestamp = DateTimeUtils.getDateTime();
					FileUtils.delete(filePath);
					app.apiAssetExchangeLogDb.dbPurged.insert(assetType, assetId);
					Log.d(logTag, "Purging asset: " + assetType + ", " + assetId + ", " + filePath.substring(1 + filePath.lastIndexOf("/")));
					isPurgeReported = true;
				}
			}
			if (!isPurgeReported) { Log.d(logTag, "Purging asset: " + assetType + ", " + assetId); }

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	public void purgeAllCheckIns() {

		String deviceGuid = app.rfcxGuardianIdentity.getGuid();
		String defaultAudioCodec = app.rfcxPrefs.getPrefAsString("audio_encode_codec");

		List<String[]> allQueued = app.apiCheckInDb.dbQueued.getAllRows();
		app.apiCheckInDb.dbQueued.deleteAllRows();
		List<String> queuedDeletedList = new ArrayList<String>();

		for (String[] queuedRow : allQueued) {
			String fileTimestamp = queuedRow[1].substring(0, queuedRow[1].indexOf("."));
			String filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(deviceGuid, app.getApplicationContext(),
					Long.parseLong(fileTimestamp), defaultAudioCodec);
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
			String filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(deviceGuid, app.getApplicationContext(),
					Long.parseLong(fileTimestamp), defaultAudioCodec);
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
			String filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(deviceGuid, app.getApplicationContext(),
					Long.parseLong(fileTimestamp), defaultAudioCodec);
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
			String filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(deviceGuid, app.getApplicationContext(),
					Long.parseLong(fileTimestamp), defaultAudioCodec);
			if ((new File(filePath)).exists()) {
				(new File(filePath)).delete();
				sentDeletedList.add(fileTimestamp);
			}
		}

		Log.v(logTag, "Deleted from Sent: " + TextUtils.join(" ", sentDeletedList));

	}


	private void processCheckInResponseMessage(String jsonStr) {

		Log.i(logTag, "Received: " + jsonStr);
		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			// reset/record request latency
			this.requestSendReturned = System.currentTimeMillis();

			// parse audio info and use it to purge the data locally
			// this assumes that the audio array has only one item in it
			// multiple audio items returned in this array would cause an error
			if (jsonObj.has("audio")) {
				JSONArray audioJson = jsonObj.getJSONArray("audio");
				String audioId = audioJson.getJSONObject(0).getString("id");
				purgeSingleAsset("audio", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), audioId);
				this.latestCheckInAudioId = audioId;

				if (jsonObj.has("checkin_id")) {
					String checkInId = jsonObj.getString("checkin_id");
					if (checkInId.length() > 0) {
						long[] checkInStats = this.inFlightCheckInStats.get(audioId);
						if (checkInStats != null) {
							app.apiCheckInStatsDb.dbStats.insert(checkInId, checkInStats[1], checkInStats[2]);
							Calendar rightNow = GregorianCalendar.getInstance();
							rightNow.setTime(new Date());

							reQueueStashedCheckInIfAllowedByHealthCheck(new long[]{
									/* latency */    	checkInStats[1],
									/* queued */       	(long) app.apiCheckInDb.dbQueued.getCount(),
									/* recent */        checkInStats[0],
									/* time-of-day */   (long) rightNow.get(Calendar.HOUR_OF_DAY)
							});
							app.apiDiagnosticsDb.dbBandwidth.insert(checkInId, Math.round(1000*checkInStats[2]/checkInStats[1]) );
						}
					}
				}
				this.inFlightCheckInEntries.remove(audioId);
				this.inFlightCheckInStats.remove(audioId);
			}

			// parse screenshot info and use it to purge the data locally
			if (jsonObj.has("screenshots")) {
				JSONArray screenShotJson = jsonObj.getJSONArray("screenshots");
				for (int i = 0; i < screenShotJson.length(); i++) {
					String screenShotId = screenShotJson.getJSONObject(i).getString("id");
					purgeSingleAsset("screenshot", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), screenShotId);
				}
			}

			// parse log info and use it to purge the data locally
			if (jsonObj.has("logs")) {
				JSONArray logsJson = jsonObj.getJSONArray("logs");
				for (int i = 0; i < logsJson.length(); i++) {
					String logId = logsJson.getJSONObject(i).getString("id");
					purgeSingleAsset("log", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), logId);
				}
			}

			// parse sms info and use it to purge the data locally
			if (jsonObj.has("messages")) {
				JSONArray messagesJson = jsonObj.getJSONArray("messages");
				for (int i = 0; i < messagesJson.length(); i++) {
					String smsId = messagesJson.getJSONObject(i).getString("id");
					purgeSingleAsset("sms", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), smsId);
				}
			}

			// parse 'meta' info and use it to purge the data locally
			if (jsonObj.has("meta")) {
				JSONArray metaJson = jsonObj.getJSONArray("meta");
				for (int i = 0; i < metaJson.length(); i++) {
					String metaId = metaJson.getJSONObject(i).getString("id");
					purgeSingleAsset("meta", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), metaId);
				}
			}

			// parse photo info and use it to purge the data locally
			if (jsonObj.has("photos")) {
				JSONArray photosJson = jsonObj.getJSONArray("photos");
				for (int i = 0; i < photosJson.length(); i++) {
					String photoId = photosJson.getJSONObject(i).getString("id");
					purgeSingleAsset("photo", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), photoId);
				}
			}

			// parse video info and use it to purge the data locally
			if (jsonObj.has("videos")) {
				JSONArray videosJson = jsonObj.getJSONArray("videos");
				for (int i = 0; i < videosJson.length(); i++) {
					String videoId = videosJson.getJSONObject(i).getString("id");
					purgeSingleAsset("video", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), videoId);
				}
			}

			// parse 'purged' confirmation array and delete entries from asset exchange log
			if (jsonObj.has("purged")) {
				JSONArray purgedJson = jsonObj.getJSONArray("purged");
				for (int i = 0; i < purgedJson.length(); i++) {
					JSONObject purgedObj = purgedJson.getJSONObject(i);
					if (purgedObj.has("type") && purgedObj.has("id")) {
						String assetId = purgedObj.getString("id");
						String assetType = purgedObj.getString("type");
						if (	assetType.equalsIgnoreCase("meta")
								|| assetType.equalsIgnoreCase("audio")
								|| assetType.equalsIgnoreCase("screenshot")
								|| assetType.equalsIgnoreCase("log")
								|| assetType.equalsIgnoreCase("photo")
								|| assetType.equalsIgnoreCase("video")
						) {
							app.apiAssetExchangeLogDb.dbPurged.deleteSingleRowByTimestamp(assetId);
						}
					}
				}
			}

			// parse generic 'received' info and use it to purge the data locally
			if (jsonObj.has("received")) {
				JSONArray receivedJson = jsonObj.getJSONArray("received");
				for (int i = 0; i < receivedJson.length(); i++) {
					JSONObject receivedObj = receivedJson.getJSONObject(i);
					if (receivedObj.has("type") && receivedObj.has("id")) {
						String assetId = receivedObj.getString("id");
						String assetType = receivedObj.getString("type");
						purgeSingleAsset(assetType, app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), assetId);
					}
				}
			}

			// parse 'unconfirmed' array
			if (jsonObj.has("unconfirmed")) {
				JSONArray unconfirmedJson = jsonObj.getJSONArray("unconfirmed");
				for (int i = 0; i < unconfirmedJson.length(); i++) {
					String assetId = unconfirmedJson.getJSONObject(i).getString("id");
					String assetType = unconfirmedJson.getJSONObject(i).getString("type");
					if (assetType.equalsIgnoreCase("audio")) {
						reQueueAudioAssetForCheckIn("sent", assetId);
					}
				}
			}

			// parse 'prefs' array
			if (jsonObj.has("prefs")) {
				JSONArray prefsJson = jsonObj.getJSONArray("prefs");
				for (int i = 0; i < prefsJson.length(); i++) {
					JSONObject prefsObj = prefsJson.getJSONObject(i);
					if (prefsObj.has("sha1")) {
						this.prefsSha1FullApiSync = prefsObj.getString("sha1").toLowerCase();
					}
				}
			}

			// parse 'instructions' array
			if (jsonObj.has("instructions")) {
				app.instructionsUtils.processReceivedInstructionJson( (new JSONObject()).put("instructions",jsonObj.getJSONArray("instructions")) );
				app.rfcxServiceHandler.triggerService("InstructionsExecution", false);
			}

			// increase total of synced audio when get the response from mqtt sending
//			totalSyncedAudio += 1;

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}

	}

	private void moveCheckInEntryToSentDatabase(String inFlightCheckInAudioId) {

		if ((this.inFlightCheckInEntries.get(inFlightCheckInAudioId) != null) && (this.inFlightCheckInEntries.get(inFlightCheckInAudioId)[0] != null)) {
			String[] checkInEntry = this.inFlightCheckInEntries.get(inFlightCheckInAudioId);
			// delete latest instead to keep present info
			if (this.latestCheckInAudioId != null) {
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(this.latestCheckInAudioId);
			}
			if ((checkInEntry != null) && (checkInEntry[0] != null)) {
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(checkInEntry[1]);
				app.apiCheckInDb.dbSent.insert(checkInEntry[1], checkInEntry[2], checkInEntry[3], checkInEntry[4], checkInEntry[5], checkInEntry[6]);
				app.apiCheckInDb.dbSent.incrementSingleRowAttempts(checkInEntry[1]);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInEntry[1]);
			}
		}


		long sentFileSizeBufferInBytes = app.rfcxPrefs.getPrefAsLong("checkin_sent_filesize_buffer")*1024*1024;

		if (app.apiCheckInDb.dbSent.getCumulativeFileSizeForAllRows() >= sentFileSizeBufferInBytes) {

			long sentFileSizeSumBeforeLimit = 0;
			int sentCountBeforeLimit = 0;

			for (String[] checkInCycle : app.apiCheckInDb.dbSent.getRowsWithOffset(0,5000)) {
				sentFileSizeSumBeforeLimit += Long.parseLong(checkInCycle[6]);
				if (sentFileSizeSumBeforeLimit >= sentFileSizeBufferInBytes) { break; }
				sentCountBeforeLimit++;
			}

			for (String[] sentCheckInsToMove : app.apiCheckInDb.dbSent.getRowsWithOffset(sentCountBeforeLimit, 16)) {

				if (!DeviceDiskUsage.isExternalStorageWritable()) {
					app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(sentCheckInsToMove[1]);

				} else {
					String newFilePath = RfcxAudioUtils.getAudioFileLocation_ExternalStorage(
							app.rfcxGuardianIdentity.getGuid(),
							Long.parseLong(sentCheckInsToMove[1].substring(0, sentCheckInsToMove[1].lastIndexOf("."))),
							sentCheckInsToMove[1].substring(sentCheckInsToMove[1].lastIndexOf(".") + 1));
					try {
						FileUtils.copy(sentCheckInsToMove[4], newFilePath);
					} catch (IOException e) {
						RfcxLog.logExc(logTag, e);
					}

					if (FileUtils.exists(newFilePath) && (FileUtils.getFileSizeInBytes(newFilePath) == Long.parseLong(sentCheckInsToMove[6]))) {
						app.apiCheckInDb.dbSent.updateFilePathByAudioAttachmentId(sentCheckInsToMove[1], newFilePath);
					} else {
						app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(sentCheckInsToMove[1]);
					}
				}

				FileUtils.delete(sentCheckInsToMove[4]);
			}
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

		byte[] messagePayload = mqttMessage.getPayload();
		Log.i(logTag, "Received "+FileUtils.bytesAsReadableString(messagePayload.length)+" on '"+messageTopic+"' at "+DateTimeUtils.getDateTime());

		// this is a checkin response message
		if (messageTopic.equalsIgnoreCase(this.app.rfcxGuardianIdentity.getGuid()+"/cmd")) {
			processCheckInResponseMessage(StringUtils.UnGZipByteArrayToString(messagePayload));

		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryToken) {

		try {

			if (deliveryToken.getTopics().length > 0) {

				String msgTopic = deliveryToken.getTopics()[0];
				Log.i(logTag, "Completed delivery to '"+msgTopic+"' at "+DateTimeUtils.getDateTime());

				if (msgTopic.equalsIgnoreCase("guardians/checkins")) {

					moveCheckInEntryToSentDatabase(this.inFlightCheckInAudioId);
					long msgSendDuration = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(this.inFlightCheckInStats.get(this.inFlightCheckInAudioId)[0]));
					setInFlightCheckInStats(this.inFlightCheckInAudioId, 0, msgSendDuration, 0);
//					this.inFlightCheckInAudioId = null;
					String deliveryTimeReadable = DateTimeUtils.milliSecondDurationAsReadableString(msgSendDuration, true);
					SocketManager.INSTANCE.sendCheckInTestMessage(SocketManager.CheckInState.PUBLISHED, deliveryTimeReadable);
					Log.i(logTag, "CheckIn delivery time: " + deliveryTimeReadable);
				}

			} else {
				Log.e(logTag, "Message was delivered, but the topic could not be determined.");
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "deliveryComplete");
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		try {
			Log.e(logTag, "Connection lost. "
							+ DateTimeUtils.timeStampDifferenceFromNowAsReadableString(this.inFlightCheckInStats.get(this.inFlightCheckInAudioId)[0])
							+ " since last CheckIn publication was launched");
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "connectionLost");
		}

		RfcxLog.logThrowable(logTag, cause);

		confirmOrCreateConnectionToBroker(false);
	}

	public void confirmOrCreateConnectionToBroker(boolean overrideDelayBetweenAttempts) {

		long minDelayBetweenConnectionAttempts = 10000;

		if (overrideDelayBetweenAttempts || (mqttCheckInClient.mqttBrokerConnectionLastAttemptedAt < (app.deviceConnectivity.lastConnectedAt() - minDelayBetweenConnectionAttempts))) {
			try {

				setOrResetBrokerConfig();

				mqttCheckInClient.confirmOrCreateConnectionToBroker(this.app.deviceConnectivity.isConnected());
				if (mqttCheckInClient.mqttBrokerConnectionLatency > 0) {

					Log.v(logTag, "MQTT Broker Latency: Connection: "+mqttCheckInClient.mqttBrokerConnectionLatency+" ms, Subscription: "+mqttCheckInClient.mqttBrokerSubscriptionLatency+" ms");

					app.deviceSystemDb.dbMqttBrokerConnections.insert(new Date(),
													mqttCheckInClient.mqttBrokerConnectionLatency,
													mqttCheckInClient.mqttBrokerSubscriptionLatency,
													app.rfcxPrefs.getPrefAsString("api_checkin_protocol"),
													app.rfcxPrefs.getPrefAsString("api_checkin_host"),
													app.rfcxPrefs.getPrefAsInt("api_checkin_port"));

					app.rfcxServiceHandler.triggerService("ApiCheckInJob", false);
				}
			} catch (MqttException e) {
				RfcxLog.logExc(logTag, e, "confirmOrCreateConnectionToBroker");
			}
		} else {
//			Log.e(logTag, "Last broker connection attempt was less than " + DateTimeUtils.milliSecondDurationAsReadableString(minDelayBetweenConnectionAttempts) + " ago");
		}
	}

	private long publishMessageOnConfirmedConnection(String publishTopic, byte[] messageByteArray) throws MqttException {
		confirmOrCreateConnectionToBroker(true);
		SocketManager.INSTANCE.sendCheckInTestMessage(SocketManager.CheckInState.PUBLISHING, null);
		return this.mqttCheckInClient.publishMessage(publishTopic, messageByteArray);
	}

	public boolean isConnectedToBroker() {
		return mqttCheckInClient.isConnected();
	}

	// Ping Messages

	public void sendMqttPing(boolean includeAllExtraFields, String[] includeExtraFields) {

		try {

			long pingSendStart = publishMessageOnConfirmedConnection("guardians/pings", packageMqttPingPayload(buildPingJson(includeAllExtraFields, includeExtraFields)));

		} catch (Exception e) {

			RfcxLog.logExc(logTag, e, "sendMqttPing");
			handlePingPublicationExceptions(e);
		}
	}

	private byte[] packageMqttPingPayload(String pingJsonString) throws UnsupportedEncodingException, IOException, JSONException {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		// Build JSON blob
		byte[] jsonBlobAsBytes = StringUtils.gZipStringToByteArray(pingJsonString);
		String jsonBlobMetaSection = String.format(Locale.US, "%012d", jsonBlobAsBytes.length);
		byteArrayOutputStream.write(jsonBlobMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(jsonBlobAsBytes);

		byteArrayOutputStream.close();

		return byteArrayOutputStream.toByteArray();
	}

	private String buildPingJson(boolean includeAllExtraFields, String[] includeExtraFields) throws JSONException, IOException {

		JSONObject pingObj = new JSONObject();

		boolean includeMeasuredAt = false;

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "battery")) {
			pingObj.put("battery", app.deviceBattery.getBatteryStateAsConcatString(app.getApplicationContext(), null) );
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "checkins")) {
			pingObj.put("checkins", getCheckInStatusInfoForJson(false));
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "instructions")) {
			pingObj.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "prefs")) {
			pingObj.put("prefs", buildCheckInPrefsJsonObj());
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

		if (includeMeasuredAt) { pingObj.put("measured_at", System.currentTimeMillis()); }

		Log.d(logTag, pingObj.toString());

		JSONObject guardianObj = new JSONObject();
		guardianObj.put("guid", app.rfcxGuardianIdentity.getGuid());
		guardianObj.put("token", app.rfcxGuardianIdentity.getAuthToken());
		pingObj.put("guardian", guardianObj);

		return pingObj.toString();
	}

    private void handlePingPublicationExceptions(Exception inputExc) {

        try {
            String excStr = RfcxLog.getExceptionContentAsString(inputExc);

            if (excStr.contains("Too many publishes in progress")) {
//                app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
//                app.rfcxServiceHandler.triggerService("ApiCheckInJob", true);

            } else if (	excStr.contains("UnknownHostException")
                    ||	excStr.contains("Broken pipe")
                    ||	excStr.contains("Timed out waiting for a response from the server")
                    ||	excStr.contains("No route to host")
                    ||	excStr.contains("Host is unresolved")
            ) {
//                Log.i(logTag, "Connection has failed "+this.inFlightCheckInAttemptCounter +" times (max: "+this.inFlightCheckInAttemptCounterLimit +")");
//                app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
//                if (this.inFlightCheckInAttemptCounter >= this.inFlightCheckInAttemptCounterLimit) {
//                    Log.d(logTag, "Max Connection Failure Loop Reached: Airplane Mode will be toggled.");
//                    app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle", app.getApplicationContext().getContentResolver());
//                    this.inFlightCheckInAttemptCounter = 0;
//                }
            }
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e, "handlePingPublicationExceptions");
        }
    }

}
