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
import java.util.List;
import java.util.Locale;

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
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceLogCat;
import org.rfcx.guardian.utility.device.capture.DeviceScreenShot;
import org.rfcx.guardian.utility.network.MqttUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class ApiCheckInUtils implements MqttCallback {

	public ApiCheckInUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

		this.mqttCheckInClient = new MqttUtils(context, RfcxGuardian.APP_ROLE, this.app.rfcxGuardianIdentity.getGuid());
		this.mqttCheckInClient.addSubscribeTopic(this.app.rfcxGuardianIdentity.getGuid()+"/cmd");
		setOrResetBrokerConfig();
		this.mqttCheckInClient.setCallback(this);
		getSetCheckInPublishTimeOutLength();
		initializeFailedCheckInThresholds();

		confirmOrCreateConnectionToBroker(true);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInUtils");

	private RfcxGuardian app;
	private MqttUtils mqttCheckInClient = null;

	private long checkInPublishTimeOutLength = 0;
	private long checkInPublishCompletedAt = System.currentTimeMillis();

	private int inFlightCheckInAttemptCounter = 0;
	private int inFlightCheckInAttemptCounterLimit = 5;

	private int[] failedCheckInThresholds = new int[0];
	private boolean[] failedCheckInThresholdsReached = new boolean[0];

	public void setOrResetBrokerConfig() {
		String[] authUserPswd = app.rfcxPrefs.getPrefAsString("api_mqtt_auth_creds").split(",");
		String authUser = !app.rfcxPrefs.getPrefAsBoolean("enable_mqtt_auth") ? null : authUserPswd[0];
		String authPswd = !app.rfcxPrefs.getPrefAsBoolean("enable_mqtt_auth") ? null : authUserPswd[1];
		this.mqttCheckInClient.setOrResetBroker(
			this.app.rfcxPrefs.getPrefAsString("api_mqtt_protocol"),
			this.app.rfcxPrefs.getPrefAsInt("api_mqtt_port"),
			this.app.rfcxPrefs.getPrefAsString("api_mqtt_host"),
			this.app.rfcxGuardianIdentity.getKeystorePassphrase(),
			!authUser.equalsIgnoreCase("[guid]") ? authUser : app.rfcxGuardianIdentity.getGuid(),
			!authPswd.equalsIgnoreCase("[token]") ? authPswd : app.rfcxGuardianIdentity.getAuthToken()
		);
	}

	public long getSetCheckInPublishTimeOutLength() {
		long timeOutLength = 2 * this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
		if (this.checkInPublishTimeOutLength < timeOutLength) {
			this.checkInPublishTimeOutLength = timeOutLength;
			this.mqttCheckInClient.setActionTimeout(timeOutLength);
		}
		return this.checkInPublishTimeOutLength;
	}

	boolean addCheckInToQueue(String[] audioInfo, String filePath) {

		// serialize audio info into JSON for checkin queue insertion
		String queueJson = app.apiJsonUtils.buildCheckInQueueJson(audioInfo);

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

		if (	app.apiCheckInHealthUtils.validateRecentCheckInHealthCheck(
					app.rfcxPrefs.getPrefAsLong("audio_cycle_duration"),
					app.rfcxPrefs.getPrefAsString("checkin_requeue_bounds_hours"),
					currentCheckInStats
				)
			&& 	(app.apiCheckInDb.dbStashed.getCount() > 0)
		) {
			reQueueAudioAssetForCheckIn("stashed", app.apiCheckInDb.dbStashed.getLatestRow()[1]);
		}
	}

	private byte[] packageMqttCheckInPayload(String checkInJsonString, String checkInAudioFilePath) throws IOException, JSONException {

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
		byte[] jsonBlobAsBytes = StringUtils.gZipStringToByteArray(app.apiJsonUtils.buildCheckInJson(checkInJsonString, screenShotMeta, logFileMeta, photoFileMeta, videoFileMeta));
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

				app.apiCheckInHealthUtils.updateInFlightCheckInOnSend(audioId, checkInDbEntry);
				this.inFlightCheckInAttemptCounter++;

				app.apiCheckInDb.dbQueued.incrementSingleRowAttempts(audioId);
				long msgSendStart = publishMessageOnConfirmedConnection("guardians/checkins", true, checkInPayload);

				app.apiCheckInHealthUtils.setInFlightCheckInStats(audioId, msgSendStart, 0, checkInPayload.length);
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
				if (milliSecondsSinceAccessed > this.checkInPublishTimeOutLength) {
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

		this.checkInPublishCompletedAt = System.currentTimeMillis();

		Log.v(logTag, "Resetting CheckIn Failure Thresholds");
	}

	void updateFailedCheckInThresholds() {

		if (this.failedCheckInThresholds.length > 0) {

			int minsSinceSuccess = (int) Math.floor(((System.currentTimeMillis() - this.checkInPublishCompletedAt) / 1000) / 60);
	//		int minsSinceConnected = (int) Math.floor(((System.currentTimeMillis() - app.deviceConnectivity.lastConnectedAt()) / 1000) / 60);

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
		return (batteryCharge >= app.rfcxPrefs.getPrefAsInt("checkin_cutoff_battery"));
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
				app.metaDb.dbMeta.deleteSingleRowByTimestamp(assetId);
				app.assetExchangeLogDb.dbPurged.insert(assetType, assetId);

			} else if (assetType.equals("instruction")) {
				app.instructionsDb.dbExecutedInstructions.deleteSingleRowById(assetId);
				app.instructionsDb.dbQueuedInstructions.deleteSingleRowById(assetId);

			}

			boolean isPurgeReported = false;
			// delete asset file after it has been purged from records
			for (String filePath : filePaths) {
				if ((filePath != null) && (new File(filePath)).exists()) {
					FileUtils.delete(filePath);
					app.assetExchangeLogDb.dbPurged.insert(assetType, assetId);
					Log.d(logTag, "Purging asset: " + assetType + ", " + assetId + ", " + filePath.substring(1 + filePath.lastIndexOf("/")));
					isPurgeReported = true;
				}
			}
			if (!isPurgeReported) { Log.d(logTag, "Purging asset: " + assetType + ", " + assetId); }

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	private void processCheckInResponseMessage(String jsonStr) {

		Log.i(logTag, "Received: " + jsonStr);
		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			// parse audio info and use it to purge the data locally
			// this assumes that the audio array has only one item in it
			// multiple audio items returned in this array would cause an error
			if (jsonObj.has("audio")) {
				JSONArray audioJson = jsonObj.getJSONArray("audio");
				String audioId = audioJson.getJSONObject(0).getString("id");
				purgeSingleAsset("audio", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), audioId);

				if (jsonObj.has("checkin_id")) {
					String checkInId = jsonObj.getString("checkin_id");
					if (checkInId.length() > 0) {
						long[] checkInStats = app.apiCheckInHealthUtils.getInFlightCheckInStatsEntry(audioId);
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
						}
					}
				}
				app.apiCheckInHealthUtils.updateInFlightCheckInOnReceive(audioId);
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
							app.assetExchangeLogDb.dbPurged.deleteSingleRowByTimestamp(assetId);
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
						app.apiJsonUtils.prefsSha1FullApiSync = prefsObj.getString("sha1").toLowerCase();
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

		if ((app.apiCheckInHealthUtils.getInFlightCheckInEntry(inFlightCheckInAudioId) != null) && (app.apiCheckInHealthUtils.getInFlightCheckInEntry(inFlightCheckInAudioId)[0] != null)) {
			String[] checkInEntry = app.apiCheckInHealthUtils.getInFlightCheckInEntry(inFlightCheckInAudioId);
			// delete latest instead to keep present info
			if (app.apiCheckInHealthUtils.getLatestCheckInAudioId() != null) {
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(app.apiCheckInHealthUtils.getLatestCheckInAudioId());
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

	@Override
	public void messageArrived(String messageTopic, MqttMessage mqttMessage) {

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
					moveCheckInEntryToSentDatabase(app.apiCheckInHealthUtils.getInFlightCheckInAudioId());
					long publishDuration = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds( app.apiCheckInHealthUtils.getCurrentInFlightCheckInStatsEntry()[0] ));
					app.apiCheckInHealthUtils.setInFlightCheckInStats(app.apiCheckInHealthUtils.getInFlightCheckInAudioId(), 0, publishDuration, 0);
					this.checkInPublishCompletedAt = System.currentTimeMillis();
					String publishDurationReadable = DateTimeUtils.milliSecondDurationAsReadableString(publishDuration, true);
					SocketManager.INSTANCE.sendCheckInTestMessage(SocketManager.CheckInState.PUBLISHED, publishDurationReadable);
					Log.i(logTag, "CheckIn delivery time: " + publishDurationReadable);
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
							+ DateTimeUtils.timeStampDifferenceFromNowAsReadableString( app.apiCheckInHealthUtils.getCurrentInFlightCheckInStatsEntry()[0] )
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
													app.rfcxPrefs.getPrefAsString("api_mqtt_protocol"),
													app.rfcxPrefs.getPrefAsString("api_mqtt_host"),
													app.rfcxPrefs.getPrefAsInt("api_mqtt_port"));

					app.rfcxServiceHandler.triggerService("ApiCheckInJob", false);
				}
			} catch (MqttException e) {
				RfcxLog.logExc(logTag, e, "confirmOrCreateConnectionToBroker");
			}
		} else {
//			Log.e(logTag, "Last broker connection attempt was less than " + DateTimeUtils.milliSecondDurationAsReadableString(minDelayBetweenConnectionAttempts) + " ago");
		}
	}

	private long publishMessageOnConfirmedConnection(String publishTopic, boolean trackDuration, byte[] messageByteArray) throws MqttException {
		confirmOrCreateConnectionToBroker(true);
		if (publishTopic.equalsIgnoreCase("guardians/checkins")) { SocketManager.INSTANCE.sendCheckInTestMessage(SocketManager.CheckInState.PUBLISHING, null); }
		return this.mqttCheckInClient.publishMessage(publishTopic, trackDuration, messageByteArray);
	}

	public boolean isConnectedToBroker() {
		return mqttCheckInClient.isConnected();
	}

	// Ping Messages

	public void sendMqttPing(boolean includeAllExtraFields, String[] includeExtraFields) {

		try {

			long pingSendStart = publishMessageOnConfirmedConnection("guardians/pings", false, packageMqttPingPayload(app.apiJsonUtils.buildPingJson(includeAllExtraFields, includeExtraFields)));

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
