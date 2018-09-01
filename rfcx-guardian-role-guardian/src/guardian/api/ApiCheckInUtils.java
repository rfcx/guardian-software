package guardian.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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

import rfcx.utility.misc.ArrayUtils;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.StringUtils;
import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceDiskUsage;
import rfcx.utility.device.DeviceMobilePhone;
import rfcx.utility.device.control.DeviceLogCat;
import rfcx.utility.device.control.DeviceScreenShot;
import rfcx.utility.device.hardware.DeviceHardwareUtils;
import rfcx.utility.mqtt.MqttUtils;
import rfcx.utility.rfcx.RfcxComm;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxRole;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import guardian.RfcxGuardian;

public class ApiCheckInUtils implements MqttCallback {

	public ApiCheckInUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();

		this.requestTimeOutLength = 2 * this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
		initializeFailedCheckInThresholds();

		this.mqttCheckInClient = new MqttUtils(RfcxGuardian.APP_ROLE, this.app.rfcxDeviceGuid.getDeviceGuid());

		this.subscribeBaseTopic = (new StringBuilder()).append("guardians/").append(this.app.rfcxDeviceGuid.getDeviceGuid().toLowerCase(Locale.US)).append("/").append(RfcxGuardian.APP_ROLE.toLowerCase(Locale.US)).toString();
		this.mqttCheckInClient.addSubscribeTopic(this.subscribeBaseTopic + "/instructions");
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
	private Map<String, String[]> inFlightCheckInEntries = new HashMap<String, String[]>();
	private Map<String, long[]> inFlightCheckInStats = new HashMap<String, long[]>();

	private List<String> previousCheckIns = new ArrayList<String>();
	
	private Date preFlightStatsQueryTimestamp = new Date();

	private int[] failedCheckInThresholds = new int[0];
	private boolean[] failedCheckInThresholdsReached = new boolean[0];

	private Map<String, long[]> healthCheckMonitors = new HashMap<String, long[]>();
	private static final String[] healthCheckCategories = new String[] { "latency", "queued", "recent" };
	private long[] healthCheckTargets = new long[healthCheckCategories.length];

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
	
	private void runRecentActivityHealthCheck(long[] inputValues) {

		long[] defaultValues = new long[6]; Arrays.fill(defaultValues, Math.round(Long.MAX_VALUE / defaultValues.length));
		long[] averageValues = new long[healthCheckCategories.length]; Arrays.fill(averageValues, 0);

		 /* latency */	healthCheckTargets[0] = Math.round( 0.4 * app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000);
		 /* queued */	healthCheckTargets[1] = 1; 																	
		 /* recent */	healthCheckTargets[2] = ( defaultValues.length / 2 ) * (app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000);
		
		for (int j = 0; j < healthCheckCategories.length; j++) {
			if (!healthCheckMonitors.containsKey(healthCheckCategories[j])) { healthCheckMonitors.put(healthCheckCategories[j], defaultValues); }
			String categ = healthCheckCategories[j];
			long[] arraySnapshot = new long[defaultValues.length];
			arraySnapshot[0] = inputValues[j];
			for (int i = (defaultValues.length-1); i > 0; i--) { arraySnapshot[i] = healthCheckMonitors.get(categ)[i-1]; }
			healthCheckMonitors.remove(healthCheckCategories[j]);
			healthCheckMonitors.put(healthCheckCategories[j], arraySnapshot);
			averageValues[j] = ArrayUtils.getAverageAsLong(healthCheckMonitors.get(categ));
		}
		
		Log.e(logTag, "Health Check: "
							+"latency: "+averageValues[0]+"/"+healthCheckTargets[0]+", "
							+"queue: "+averageValues[1]+"/"+healthCheckTargets[1]+", "
							+"returns: "+Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(averageValues[2]))+"/"+healthCheckTargets[2]);
		
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

	private JSONObject getSystemMetaDataAsJson(JSONObject metaDataJsonObj) throws JSONException {

		try {

			metaDataJsonObj.put("battery", app.deviceSystemDb.dbBattery.getConcatRows());
			metaDataJsonObj.put("cpu", app.deviceSystemDb.dbCPU.getConcatRows());
			metaDataJsonObj.put("power", app.deviceSystemDb.dbPower.getConcatRows());
			metaDataJsonObj.put("network", app.deviceSystemDb.dbTelephony.getConcatRows());
			metaDataJsonObj.put("offline", app.deviceSystemDb.dbOffline.getConcatRows());
			metaDataJsonObj.put("lightmeter", app.deviceSensorDb.dbLightMeter.getConcatRows());
			metaDataJsonObj.put("data_transfer", app.deviceDataTransferDb.dbTransferred.getConcatRows());
			metaDataJsonObj.put("accelerometer", app.deviceSensorDb.dbAccelerometer.getConcatRows());
			metaDataJsonObj.put("location", app.deviceSensorDb.dbGeoLocation.getConcatRows());
			metaDataJsonObj.put("reboots", app.rebootDb.dbRebootComplete.getConcatRows());
			
			metaDataJsonObj.put("disk_usage", DeviceDiskUsage.concatDiskStats());
			
			// this timestamp should be generated and added following the previous queries
			this.preFlightStatsQueryTimestamp = new Date();
			metaDataJsonObj.put("measured_at", this.preFlightStatsQueryTimestamp.getTime());

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

		return metaDataJsonObj;
	}

	private void clearPreFlightSystemMetaData(Date deleteBefore) {
		try {
			app.deviceSystemDb.dbBattery.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbCPU.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbPower.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbTelephony.clearRowsBefore(deleteBefore);
			app.deviceSystemDb.dbOffline.clearRowsBefore(deleteBefore);
			app.deviceSensorDb.dbLightMeter.clearRowsBefore(deleteBefore);
			app.deviceSensorDb.dbAccelerometer.clearRowsBefore(deleteBefore);
			app.deviceSensorDb.dbGeoLocation.clearRowsBefore(deleteBefore);
			app.deviceDataTransferDb.dbTransferred.clearRowsBefore(deleteBefore);
			app.rebootDb.dbRebootComplete.clearRowsBefore(deleteBefore);

			RfcxComm.deleteQueryContentProvider("admin", "database_delete_rows_before",
					"sentinel_power|" + deleteBefore.getTime(), app.getApplicationContext().getContentResolver());

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	public String buildCheckInJson(String checkInJsonString, String[] screenShotMeta, String[] logFileMeta) throws JSONException, IOException {

		JSONObject checkInMetaJson = getSystemMetaDataAsJson(new JSONObject(checkInJsonString));

		// Adding Guardian GUID
		checkInMetaJson.put("guardian_guid", this.app.rfcxDeviceGuid.getDeviceGuid());

		// Adding latency data from previous checkins
		checkInMetaJson.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));

		// Recording number of currently queued/skipped/stashed checkins
		checkInMetaJson.put("checkins", TextUtils.join("|", new String[] { 	
										"queued*"+app.apiCheckInDb.dbQueued.getCount() ,
										"sent*"+app.apiCheckInDb.dbSent.getCount(),
										"skipped*"+app.apiCheckInDb.dbSkipped.getCount(),
										"stashed*"+app.apiCheckInDb.dbStashed.getCount()
									}));

		// Telephony and SIM card info
		checkInMetaJson.put("phone", DeviceMobilePhone.getConcatMobilePhoneInfo(app.getApplicationContext()));
		
		// Hardware info
		checkInMetaJson.put("hardware", DeviceHardwareUtils.getDeviceHardwareInfoJson());

		// Adding software role versions
		checkInMetaJson.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));

		// Adding checksum of current prefs values
		checkInMetaJson.put("prefs", app.rfcxPrefs.getPrefsChecksum());

		// Adding device location timezone offset
		checkInMetaJson.put("timezone_offset", DateTimeUtils.getTimeZoneOffset());
		checkInMetaJson.put("datetime", "system*"+System.currentTimeMillis()+"|timezone*"+DateTimeUtils.getTimeZoneOffset());

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

		// Adding sentinel data, if they can be retrieved
		JSONObject sentinelJson = new JSONObject();
		JSONArray sentinelPower = RfcxComm.getQueryContentProvider("admin", "database_get_all_rows", "sentinel_power", app.getApplicationContext().getContentResolver());
		sentinelJson.put("power", (sentinelPower.length() > 0) ? sentinelPower.getJSONObject(0) : new JSONObject() ); 
		checkInMetaJson.put("sentinel", sentinelJson);
		
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

			if ((minsSinceSuccess < this.failedCheckInThresholds[0]) // we haven't yet reached the first threshold for
																		// bad connectivity
					|| app.rfcxPrefs.getPrefAsBoolean("checkin_offline_mode") // we are explicitly in offline mode
					|| !isBatteryChargeSufficientForCheckIn() // checkins are explicitly paused due to low battery level
					|| (app.deviceConnectivity.isConnected() && (minsSinceConnected < this.failedCheckInThresholds[0])) // this
																														// is
																														// likely
																														// the
																														// first
																														// checkin
																														// after
																														// a
																														// period
																														// of
																														// disconnection
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
							app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_off",
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

	private void purgeSingleAsset(String assetType, String rfcxDeviceId, Context context, String timestamp,
			String fileExtension) {

		try {
			String filePath = null;

			if (assetType.equals("audio")) {
				filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(rfcxDeviceId, context,
						(long) Long.parseLong(timestamp), fileExtension);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(timestamp);
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(timestamp);
				app.audioEncodeDb.dbEncoded.deleteSingleRow(timestamp);

			} else if (assetType.equals("screenshot")) {
				filePath = DeviceScreenShot.getScreenShotFileLocation_Complete(rfcxDeviceId, context,
						(long) Long.parseLong(timestamp));
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "screenshots|" + timestamp,
						app.getApplicationContext().getContentResolver());

			} else if (assetType.equals("log")) {
				filePath = DeviceLogCat.getLogFileLocation_Complete_PostZip(rfcxDeviceId, context,
						(long) Long.parseLong(timestamp));
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "logs|" + timestamp,
						app.getApplicationContext().getContentResolver());
			}

			if ((new File(filePath)).exists()) {
				(new File(filePath)).delete();
			}

			Log.d(logTag, "Purging asset: " + assetType + ", " + timestamp + ", "
					+ filePath.substring(1 + filePath.lastIndexOf("/")));

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

	private void processInstructionMessage(byte[] messagePayload) {

		String jsonStr = StringUtils.UnGZipByteArrayToString(messagePayload);
		Log.i(logTag, "Instruction: " + jsonStr);

		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			// get api-assigned instruction guid
			if (jsonObj.has("instruction_id")) {
				String instructionId = jsonObj.getString("instruction_id");
				if (instructionId.length() > 0) {
					Log.i(logTag, "Instruction ID: " + instructionId);
					// DO SOMETHING HERE
				}
			}

			// instructions: messages
			if (jsonObj.has("messages")) {
				JSONArray instructionMsgsJson = jsonObj.getJSONArray("messages");
				for (int i = 0; i < instructionMsgsJson.length(); i++) {
					JSONObject instructionMsgJson = instructionMsgsJson.getJSONObject(i);
					RfcxComm.getQueryContentProvider("admin", "sms_send",
							instructionMsgJson.getString("address") + "|" + instructionMsgJson.getString("body"),
							app.getApplicationContext().getContentResolver());
				}
			}

			// instructions: prefs
			if (jsonObj.has("prefs")) {
				JSONArray instructionPrefsJson = jsonObj.getJSONArray("prefs");
				for (int i = 0; i < instructionPrefsJson.length(); i++) {
					JSONObject instructionPrefJson = instructionPrefsJson.getJSONObject(i);
					// Here we would set preferences...
				}
			}

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}

	}

	private void processCheckInResponseMessage(byte[] messagePayload) {

		String jsonStr = StringUtils.UnGZipByteArrayToString(messagePayload);
		Log.i(logTag, "CheckIn: " + jsonStr);

		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			// reset/record request latency
			this.requestSendReturned = System.currentTimeMillis();

			// parse audio info and use it to purge the data locally
			if (jsonObj.has("audio")) {
				JSONArray audioJson = jsonObj.getJSONArray("audio");
				for (int i = 0; i < audioJson.length(); i++) {
					
					String audioId = audioJson.getJSONObject(i).getString("id");
					purgeSingleAsset("audio", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), audioId,
							this.app.rfcxPrefs.getPrefAsString("audio_encode_codec"));
					
					if (jsonObj.has("checkin_id")) {
						String checkInId = jsonObj.getString("checkin_id");
						if (checkInId.length() > 0) {
							long[] checkInStats = this.inFlightCheckInStats.get(audioId);
							
							this.previousCheckIns = new ArrayList<String>();
							this.previousCheckIns.add( TextUtils.join("*", new String[] { checkInId+"", checkInStats[1]+"", checkInStats[2]+"" } ) );
							
							runRecentActivityHealthCheck(new long[] { checkInStats[1], (long) app.apiCheckInDb.dbQueued.getCount(), checkInStats[0] });
								
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
					RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "screenshots|" + screenShotId,
							app.getApplicationContext().getContentResolver());
					purgeSingleAsset("screenshot", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(),
							screenShotId, "png");
				}
			}

			// parse log info and use it to purge the data locally
			if (jsonObj.has("logs")) {
				JSONArray logsJson = jsonObj.getJSONArray("logs");
				for (int i = 0; i < logsJson.length(); i++) {
					String logFileId = logsJson.getJSONObject(i).getString("id");
					RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "logs|" + logFileId,
							app.getApplicationContext().getContentResolver());
					purgeSingleAsset("log", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), logFileId,
							"log");
				}
			}

			// parse sms info and use it to purge the data locally
			if (jsonObj.has("messages")) {
				JSONArray messagesJson = jsonObj.getJSONArray("messages");
				for (int i = 0; i < messagesJson.length(); i++) {
					String messageId = messagesJson.getJSONObject(i).getString("id");
					RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "sms|" + messageId,
							app.getApplicationContext().getContentResolver());
				}
			}
			
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}

	}

	private void moveCheckInEntryToSentDatabase(String inFlightCheckInAudioId) {

		if ((this.inFlightCheckInEntries.get(inFlightCheckInAudioId) != null) && (this.inFlightCheckInEntries.get(inFlightCheckInAudioId)[0] != null)) {
			String[] checkInEntry = this.inFlightCheckInEntries.get(inFlightCheckInAudioId);
			int sentCount = app.apiCheckInDb.dbSent.insert(checkInEntry[1], checkInEntry[2], checkInEntry[3], checkInEntry[4]);
			app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInEntry[1]);
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
		} else if (messageTopic.equalsIgnoreCase(this.subscribeBaseTopic + "/instructions")) {
			processInstructionMessage(mqttMessage.getPayload());

		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryToken) {

		try {
			moveCheckInEntryToSentDatabase(this.inFlightCheckInAudioId);
			
			long msgSendDuration = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(this.inFlightCheckInStats.get(this.inFlightCheckInAudioId)[0]));
			
			setInFlightCheckInStats(this.inFlightCheckInAudioId, 0, msgSendDuration, 0);
			
			// clear system metadata included in check in preflight
			clearPreFlightSystemMetaData(this.preFlightStatsQueryTimestamp);
			
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

		long minDelayBetweenConnectionAttempts = 5000;

		if (mqttCheckInClient.mqttBrokerConnectionLastAttemptedAt < (app.deviceConnectivity.lastConnectedAt()
				- minDelayBetweenConnectionAttempts)) {
			try {
				mqttCheckInClient.confirmOrCreateConnectionToBroker(this.app.deviceConnectivity.isConnected());
			} catch (MqttException e) {
				RfcxLog.logExc(logTag, e);
			}
		} else {
//			Log.e(logTag, "Last connection attempt was less than " + minDelayBetweenConnectionAttempts + "ms ago");
		}
	}

}
