package guardian.api.checkin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.StringUtils;
import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceDiskUsage;
import rfcx.utility.device.DeviceMobileSIMCard;
import rfcx.utility.device.control.DeviceLogCatCapture;
import rfcx.utility.device.control.DeviceScreenShot;
import rfcx.utility.http.HttpPostMultipart;
import rfcx.utility.mqtt.MqttUtils;
import rfcx.utility.rfcx.RfcxComm;
import rfcx.utility.rfcx.RfcxLog;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import guardian.RfcxGuardian;

public class ApiCheckInUtils implements MqttCallback {
	
	public ApiCheckInUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		
		// setting http post timeouts to the same as the audio capture interval.
		setHttpCheckInTimeOuts(this.app.rfcxPrefs.getPrefAsInt("audio_cycle_duration"));
		setHttpCheckInAuthHeaders(this.app.rfcxDeviceGuid.getDeviceGuid(), this.app.rfcxDeviceGuid.getDeviceToken());
		
		this.mqttCheckInClient = new MqttUtils(RfcxGuardian.APP_ROLE, this.app.rfcxDeviceGuid.getDeviceGuid());
		this.mqttCheckInClient.setOrResetBroker("tcp", 1883, this.app.rfcxPrefs.getPrefAsString("api_mqtt_host"));
		this.mqttCheckInClient.setCallback(this);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInUtils.class);

	private RfcxGuardian app;
	private MqttUtils mqttCheckInClient = null;

	public Date requestSendStart = new Date();
	public Date requestSendReturned = new Date();
	public long requestSendDuration = 0;
	
//	private String[] inFlightCheckInEntry = null;
	
	private Map<String, String[]> inFlightCheckIns = new HashMap<String, String[]>();

	private List<String> previousCheckIns = new ArrayList<String>();
	private Date checkInPreFlightTimestamp = new Date();

	public int[] connectivityToggleThresholds = new int[] { 10, 20, 30, 45, 60, 75, 90, 110, 130, 150, 180 };
	public boolean[] connectivityToggleThresholdsReached = new boolean[] { false, false, false, false, false, false, false, false, false, false, false };


	public boolean addCheckInToQueue(String[] audioInfo, String filepath) {

		// serialize audio info into JSON for checkin queue insertion
		String queueJson = generateCheckInQueueJson(audioInfo);
		
		// add audio info to checkin queue
		app.apiCheckInDb.dbQueued.insert(
					audioInfo[1]+"."+audioInfo[2],
					queueJson, 
					"0", 
					filepath
				);

		Log.d(logTag, "Queued (1/"+app.apiCheckInDb.dbQueued.getCount()+"): "+queueJson+" | "+filepath);
		
		// once queued, remove database reference from encode role
		String[] encodedAudioFromDb = app.audioEncodeDb.dbEncoded.getSingleRowByAudioId(audioInfo[1]);
		if (encodedAudioFromDb[1] != null) { app.audioEncodeDb.dbEncoded.deleteSingleRow(encodedAudioFromDb[1]); }		
		
		// if the queued table has grown beyond the maximum threshold, stash the oldest checkins 
		stashOldestCheckIns();
		
		return true;
	}
	
	private void stashOldestCheckIns() {
		
		List<String[]> checkInsBeyondStashThreshold = app.apiCheckInDb.dbQueued.getRowsWithOffset(app.rfcxPrefs.getPrefAsInt("checkin_stash_threshold"), app.rfcxPrefs.getPrefAsInt("checkin_archive_threshold"));
		
		if (checkInsBeyondStashThreshold.size() > 0) {
			
			// string list for reporting stashed checkins to the log
			List<String> stashList = new ArrayList<String>();
			
			//cycle through stashable checkins and move them to the new table/database
			for (String[] checkInsToStash : checkInsBeyondStashThreshold) {
				app.apiCheckInDb.dbStashed.insert( checkInsToStash[1], checkInsToStash[2], checkInsToStash[3], checkInsToStash[4]);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInsToStash[1].substring(0,checkInsToStash[1].lastIndexOf(".")));
				stashList.add(checkInsToStash[1]);
			}
			
			//report in the logs
			Log.i(logTag, "Stashed CheckIns ("+app.apiCheckInDb.dbStashed.getCount()+" total in database): "+TextUtils.join(" ", stashList));
		}
		
		if (app.apiCheckInDb.dbStashed.getCount() >= app.rfcxPrefs.getPrefAsInt("checkin_archive_threshold")) {
			Log.i(logTag, "TODO: STASHED CHECKINS SHOULD BE ARCHIVED HERE...");
		}
	}

	private String generateCheckInQueueJson(String[] audioFileInfo) {

		try {
			JSONObject queueJson = new JSONObject();

			// Recording the moment the check in was queued
			queueJson.put("queued_at", (new Date()).getTime());

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
	
	private List<String> getInstalledSoftwareVersions() {

		List<String> softwareVersions = new ArrayList<String>();

//		try {
//			Cursor cursor = app.getContentResolver().query(
//					Uri.parse(RfcxRole.ContentProvider.updater.URI_1),
//					RfcxRole.ContentProvider.updater.PROJECTION_1,
//					null, null, null);
//
//			if (cursor.getCount() > 0) { try { if (cursor.moveToFirst()) { do { 
//				
//				softwareVersions
//				.add(cursor.getString(cursor
//						.getColumnIndex(RfcxRole.ContentProvider.updater.PROJECTION_1[0]))
//						+ "*"
//						+ cursor.getString(cursor.getColumnIndex(RfcxRole.ContentProvider.updater.PROJECTION_1[1])));
//	
//			} while (cursor.moveToNext()); } } finally { cursor.close(); } }
//
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}

		return softwareVersions;
	}

	private JSONObject getSystemMetaDataAsJson(JSONObject metaDataJsonObj) throws JSONException {

		this.checkInPreFlightTimestamp = new Date();

		try {
			metaDataJsonObj.put(	"battery", 		app.deviceSystemDb.dbBattery.getConcatRows());
			metaDataJsonObj.put("cpu", 			app.deviceSystemDb.dbCPU.getConcatRows());
			metaDataJsonObj.put("power",			app.deviceSystemDb.dbPower.getConcatRows());
			metaDataJsonObj.put("network",		app.deviceSystemDb.dbTelephony.getConcatRows());
			metaDataJsonObj.put("offline",		app.deviceSystemDb.dbOffline.getConcatRows());
			metaDataJsonObj.put("lightmeter",	app.deviceSensorDb.dbLightMeter.getConcatRows());
			metaDataJsonObj.put("data_transfer",	app.deviceDataTransferDb.dbTransferred.getConcatRows());
			metaDataJsonObj.put("accelerometer",	app.deviceSensorDb.dbAccelerometer.getConcatRows());
			metaDataJsonObj.put("reboots",		app.rebootDb.dbRebootComplete.getConcatRows());
			metaDataJsonObj.put("disk_usage",	DeviceDiskUsage.concatDiskStats());
			
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
			app.deviceDataTransferDb.dbTransferred.clearRowsBefore(deleteBefore);
			app.rebootDb.dbRebootComplete.clearRowsBefore(deleteBefore);
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	public String buildCheckInJson(String checkInJsonString, String[] screenShotMeta, String[] logFileMeta) throws JSONException, IOException {
		
		JSONObject checkInMetaJson = getSystemMetaDataAsJson(new JSONObject(checkInJsonString));
		
		// Adding Guardian GUID
		checkInMetaJson.put("guid", this.app.rfcxDeviceGuid.getDeviceGuid());

		// Adding timestamp of metadata (JSON) snapshot
		checkInMetaJson.put("measured_at", checkInPreFlightTimestamp.getTime());

		// Adding GeoCoordinates
		checkInMetaJson.put("location", app.deviceGeoLocation.getSerializedGeoLocation());

		// Adding latency data from previous checkins
		checkInMetaJson.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));

		// Recording number of currently queued/skipped/stashed checkins
		checkInMetaJson.put("queued_checkins", app.apiCheckInDb.dbQueued.getCount());
		checkInMetaJson.put("skipped_checkins", app.apiCheckInDb.dbSkipped.getCount());
		checkInMetaJson.put("stashed_checkins", app.apiCheckInDb.dbStashed.getCount());

		// Telephony and SIM card info
		checkInMetaJson.put("phone_sim", DeviceMobileSIMCard.getSIMSerial(app.getApplicationContext()));
		checkInMetaJson.put("phone_imsi", DeviceMobileSIMCard.getIMSI(app.getApplicationContext()));
		checkInMetaJson.put("phone_imei", DeviceMobileSIMCard.getIMEI(app.getApplicationContext()));

		// Adding software role versions
		checkInMetaJson.put("software", TextUtils.join("|", getInstalledSoftwareVersions()));

		// Adding device location timezone offset
		checkInMetaJson.put("timezone_offset", DateTimeUtils.getTimeZoneOffset());
		
		// Adding messages to JSON blob
		checkInMetaJson.put("messages", RfcxComm.getQueryContentProvider("admin", "database_get_all_rows", "sms", app.getApplicationContext().getContentResolver()));

		// Adding screenshot meta to JSON blob
		checkInMetaJson.put( "screenshots", (screenShotMeta[0] != null) ? (screenShotMeta[1]+"*"+screenShotMeta[2]+"*"+screenShotMeta[3]+"*"+screenShotMeta[4]) : null);

		// Adding logs meta to JSON blob
		checkInMetaJson.put( "logs", (logFileMeta[0] != null) ? (logFileMeta[1]+"*"+logFileMeta[2]+"*"+logFileMeta[3]+"*"+logFileMeta[4]) : null);
		
		return checkInMetaJson.toString();
		
	}
	
	public byte[] packageMqttPayload(String checkInJsonString, String checkInAudioFilePath) throws UnsupportedEncodingException, IOException, JSONException {
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		
		String[] screenShotMeta = getLatestExternalAssetMeta("screenshots");
		String[] logFileMeta = getLatestExternalAssetMeta("logs");
		
		byte[] jsonBlobAsBytes = StringUtils.gZipStringToByteArray(buildCheckInJson(checkInJsonString, screenShotMeta, logFileMeta));
		String jsonBlobMetaSection = String.format("%012d", jsonBlobAsBytes.length);
		byteArrayOutputStream.write(jsonBlobMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(jsonBlobAsBytes);
		
		byte[] audioFileAsBytes = FileUtils.fileAsByteArray(checkInAudioFilePath);
		String audioFileMetaSection = String.format("%012d", audioFileAsBytes.length);
		byteArrayOutputStream.write(audioFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(audioFileAsBytes);
		
		byte[] screenShotFileAsBytes = new byte[0];
		if (screenShotMeta[0] != null) { screenShotFileAsBytes = FileUtils.fileAsByteArray(screenShotMeta[0]); }
		String screenShotFileMetaSection = String.format("%012d", screenShotFileAsBytes.length);
		byteArrayOutputStream.write(screenShotFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(screenShotFileAsBytes);
		
		byte[] logFileAsBytes = new byte[0];
		if (logFileMeta[0] != null) { logFileAsBytes = FileUtils.fileAsByteArray(logFileMeta[0]); }
		String logFileMetaSection = String.format("%012d", logFileAsBytes.length);
		byteArrayOutputStream.write(logFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(logFileAsBytes);
		
		byteArrayOutputStream.close();
		
		return byteArrayOutputStream.toByteArray();
	}
	
	public void sendMqttCheckIn(String[] checkInDatabaseEntry) {
		
		try {
			this.inFlightCheckIns.remove("0");
			this.inFlightCheckIns.put("0", checkInDatabaseEntry);
			this.requestSendStart = this.mqttCheckInClient.publishMessage( packageMqttPayload( checkInDatabaseEntry[2], checkInDatabaseEntry[4] ) );
			
		} catch (MqttPersistenceException e) {
			RfcxLog.logExc(logTag, e);
		} catch (MqttException e) {
			RfcxLog.logExc(logTag, e);
		} catch (UnsupportedEncodingException e) {
			RfcxLog.logExc(logTag, e);
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		
	}

	public String[] getLatestExternalAssetMeta(String assetType) {
		
		String[] assetMeta = new String[] { null };
		try {
			JSONArray latestAssetMetaArray = RfcxComm.getQueryContentProvider("admin", "database_get_latest_row", assetType, app.getApplicationContext().getContentResolver());
			if (latestAssetMetaArray.length() > 0) {
				JSONObject latestAssetMeta = latestAssetMetaArray.getJSONObject(0);
				assetMeta = new String[] { latestAssetMeta.getString("filepath"), latestAssetMeta.getString("created_at"), latestAssetMeta.getString("timestamp"), latestAssetMeta.getString("format"), latestAssetMeta.getString("digest") };
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return assetMeta;
	}

	public void connectivityToggleCheck() {

		int secsSinceSuccess = (int) ((new Date()).getTime() - this.requestSendReturned.getTime()) / 1000;
		if ((secsSinceSuccess / 60) < this.connectivityToggleThresholds[0]) {
			// everything is going fine and we haven't even reached the first
			// threshold of bad connectivity
			this.connectivityToggleThresholdsReached = new boolean[] { false, false, false, false };
		} else if (!isBatteryChargeSufficientForCheckIn()) {
			// checkins are paused due to low battery level, so we are resetting
			// the connectivity problem thesholds
			this.connectivityToggleThresholdsReached = new boolean[] { false, false, false, false };
		} else {
			int thresholdIndex = 0;
			for (int toggleThreshold : this.connectivityToggleThresholds) {
				if (((secsSinceSuccess / 60) >= toggleThreshold) && !this.connectivityToggleThresholdsReached[thresholdIndex]) {
					this.connectivityToggleThresholdsReached[thresholdIndex] = true;
					if (toggleThreshold == this.connectivityToggleThresholds[this.connectivityToggleThresholds.length - 1]) {
						// last index, force reboot
						Log.d(logTag, "ToggleCheck: ForcedReboot (" + toggleThreshold + " minutes since last successful CheckIn)");
						app.deviceControlUtils.runOrTriggerDeviceControl("reboot", app.getApplicationContext().getContentResolver());
					} else {
						Log.d(logTag, "ToggleCheck: AirplaneMode (" + toggleThreshold + " minutes since last successful CheckIn)");
						app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_off", app.getApplicationContext().getContentResolver());
					}
				}
				thresholdIndex++;
			}
		}
	}

	public boolean isBatteryChargeSufficientForCheckIn() {
		int batteryCharge = app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= app.rfcxPrefs.getPrefAsInt("checkin_battery_cutoff"));
	}
	
	public boolean isBatteryChargedButBelowCheckInThreshold() {
		return (app.deviceBattery.isBatteryCharged(app.getApplicationContext(), null) && !isBatteryChargeSufficientForCheckIn());
	}
	
	private static void purseSingleAssetFromDisk(String assetType, String rfcxDeviceId, Context context, String timestamp, String fileExtension) {
		try {
			String filePath = null;
			
			if (assetType.equals("audio")) {
				filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(rfcxDeviceId, context, (long) Long.parseLong(timestamp), fileExtension);
				
			} else if (assetType.equals("screenshot")) {
				filePath = DeviceScreenShot.getScreenShotFileLocation_Complete(rfcxDeviceId, context, (long) Long.parseLong(timestamp));
				
			} else if (assetType.equals("log")) {
				filePath = DeviceLogCatCapture.getLogFileLocation_Complete_PostZip(rfcxDeviceId, context, (long) Long.parseLong(timestamp));
			}
			
			(new File(filePath)).delete();
			Log.d(logTag, "Purging "+assetType+" asset: "+timestamp+"."+fileExtension);
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	private void processCheckInResponseJson(String responseJsonString) {
		
		try {
			
			JSONObject jsonObj = new JSONObject(responseJsonString);
			
			// reset/record request latency
			this.requestSendReturned = new Date();
			
			String checkInId = jsonObj.getString("checkin_id");
			if (checkInId.length() > 0) {
				this.previousCheckIns = new ArrayList<String>();
				this.previousCheckIns.add((new StringBuilder()).append(checkInId).append("*").append(this.requestSendDuration).toString());
			}

			// clear system metadata included in successful checkin preflight
			clearPreFlightSystemMetaData(this.checkInPreFlightTimestamp);
			
			// parse audio info and use it to purge the data locally
			JSONArray audioJson = jsonObj.getJSONArray("audio");
			for (int i = 0; i < audioJson.length(); i++) {
				String audioId = audioJson.getJSONObject(i).getString("id");
				app.audioEncodeDb.dbEncoded.deleteSingleRow(audioId);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(audioId);
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(audioId);
				purseSingleAssetFromDisk("audio", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), audioId, this.app.rfcxPrefs.getPrefAsString("audio_encode_codec"));
				this.inFlightCheckIns.remove("0");
			}
			
			// parse screenshot info and use it to purge the data locally
			JSONArray screenShotJson = jsonObj.getJSONArray("screenshots");
			for (int i = 0; i < screenShotJson.length(); i++) {
				String screenShotId = screenShotJson.getJSONObject(i).getString("id");
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "screenshots|"+screenShotId, app.getApplicationContext().getContentResolver());
				purseSingleAssetFromDisk("screenshot", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), screenShotId, "png");
			}
			
			// parse log info and use it to purge the data locally
			JSONArray logsJson = jsonObj.getJSONArray("logs");
			for (int i = 0; i < logsJson.length(); i++) {
				String logFileId = logsJson.getJSONObject(i).getString("id");
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "logs|"+logFileId, app.getApplicationContext().getContentResolver());
				purseSingleAssetFromDisk("log", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), logFileId, "log");
			}
			
			// parse sms info and use it to purge the data locally
			JSONArray messagesJson = jsonObj.getJSONArray("messages");
			for (int i = 0; i < messagesJson.length(); i++) {
				String messageId = messagesJson.getJSONObject(i).getString("id");
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "sms|"+messageId, app.getApplicationContext().getContentResolver());
			}

			// parse the instructions section
			JSONObject instructionsJson = jsonObj.getJSONObject("instructions");
			
			// instructions: messages
			JSONArray messagesInstructionsJson = instructionsJson.getJSONArray("messages");
			for (int i = 0; i < messagesInstructionsJson.length(); i++) {
				JSONObject messageInstructions = messagesInstructionsJson.getJSONObject(i);
			//	(SmsManager.getDefault()).sendTextMessage(msgJson.getString("address"), null, msgJson.getString("body"), null, null);
			}
			
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			Log.i(logTag, "Api Response: "+responseJsonString);
		}
	}
	
	private void moveCheckInEntryToSentDatabase(String inFlightCheckInAudioId) {
		
		if ((this.inFlightCheckIns.get(inFlightCheckInAudioId) != null) && (this.inFlightCheckIns.get(inFlightCheckInAudioId)[0] != null)) {
			String[] checkInEntry = this.inFlightCheckIns.get(inFlightCheckInAudioId);
			this.app.apiCheckInDb.dbSent.insert(checkInEntry[1], checkInEntry[2], checkInEntry[3], checkInEntry[4]);
			this.app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInEntry[1].substring(0,checkInEntry[1].lastIndexOf(".")));
		}
	}
	
	@Override
	public void messageArrived(String messageTopic, MqttMessage mqttMessage) throws Exception {
		
		if (messageTopic.equalsIgnoreCase("guardians/"+this.app.rfcxDeviceGuid.getDeviceGuid())) {
			String msgBody = new String(mqttMessage.getPayload());
			processCheckInResponseJson(msgBody);
		}
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryToken) {
		this.requestSendDuration = (new Date()).getTime() - this.requestSendStart.getTime();
		Log.i(logTag, "CheckIn delivery time: " + (this.requestSendDuration / 1000) + " seconds");
		
		moveCheckInEntryToSentDatabase("0");
		
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		Log.e(logTag, "Connection lost.");
		cause.printStackTrace();
		RfcxLog.logThrowable(logTag, cause);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	HttpPostMultipart httpPostMultipart = new HttpPostMultipart();
	
	private void setHttpCheckInAuthHeaders(String deviceGuid, String deviceToken) {
		List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
		rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/" + deviceGuid });
		rfcxAuthHeaders.add(new String[] { "x-auth-token", deviceToken });
		this.httpPostMultipart.setCustomHttpHeaders(rfcxAuthHeaders);
	}
	
	private void setHttpCheckInTimeOuts(int timeOut) {
		this.httpPostMultipart.setTimeOuts(timeOut, timeOut);
	}

	public String getHttpCheckInUrl() {
		return app.rfcxPrefs.getPrefAsString("api_url_base") + "/v1/guardians/" + app.rfcxDeviceGuid.getDeviceGuid() + "/checkins";
	}

	public void sendHttpCheckIn(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments, boolean allowAttachments, String checkInAudioReference) {
		if (!allowAttachments)
			keyFilepathMimeAttachments = new ArrayList<String[]>();
		if (app.deviceConnectivity.isConnected()) {
			this.requestSendStart = new Date();
			Log.i(logTag, "CheckIn sent at: " + DateTimeUtils.getDateTime(this.requestSendStart));
			String checkInResponse = httpPostMultipart.doMultipartPost(fullUrl, keyValueParameters, keyFilepathMimeAttachments);
			processHttpCheckInResponse(checkInResponse);
			if (checkInResponse.equals("Rfcx-Utils-HttpPostMultipart-UnknownHostException")) {
				Log.e(logTag, "NOT INCREMENTING CHECK-IN ATTEMPTS");
			} else {
				app.apiCheckInDb.dbQueued.incrementSingleRowAttempts(checkInAudioReference);
			}
		} else {
			Log.d(logTag, "No connectivity... Can't send CheckIn");
		}
	}
	
	public static boolean validateHttpCheckInAttachments(List<String[]> checkInFiles) {
		boolean includesAudio = false;
		boolean includesScreenShot = false;
		for (String[] fileItems : checkInFiles) {
			if (fileItems[0].equals("audio")) { includesAudio = true; }
			if (fileItems[0].equals("screenshot")) { includesScreenShot = true; }
		}
		return (includesAudio);
	}

	public String packageHttpCheckInJson(String checkInJsonString) throws JSONException, IOException {

		// For HTTP
		// Stringify JSON, gzip the output and convert to base 64 string for sending	
		String jsonFinal = buildCheckInJson(checkInJsonString, getLatestExternalAssetMeta("screenshots"), getLatestExternalAssetMeta("logs"));	
		Log.d(logTag, jsonFinal);
		String jsonFinalGZipped = StringUtils.gZipStringToBase64(jsonFinal);

		return jsonFinalGZipped;
	}


	public List<String[]> loadHttpCheckInFiles(String audioFilePath) {

		List<String[]> checkInFiles = new ArrayList<String[]>();

		// attach audio file - we only attach one per check-in
		String audioFileName = audioFilePath.substring(1 + audioFilePath.lastIndexOf("/"));
		String audioId = audioFileName.substring(0, audioFileName.lastIndexOf("."));
		String audioFormat = audioFileName.substring(1 + audioFileName.lastIndexOf("."));
		try {
			if ((new File(audioFilePath)).exists() && (new File(audioFilePath)).canRead()) {
				checkInFiles.add(new String[] { "audio", audioFilePath, "audio/" + audioFormat });
				Log.d(logTag, "Audio attached: " + audioId + "." + audioFormat);
			} else {
				Log.e(logTag, "Audio attachment file doesn't exist or isn't readable: (" + audioId+ "." + audioFormat + ") " + audioFilePath);
				String audioFileNameInDb = app.apiCheckInDb.dbQueued.getSingleRowByAudioAttachmentId(audioId)[1];
				String[] audioFromDb = app.audioEncodeDb.dbEncoded.getSingleRowByAudioId(audioFileNameInDb);
				if (audioFromDb[1] != null) { app.audioEncodeDb.dbEncoded.deleteSingleRow(audioFromDb[1]); }
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(audioId);
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(audioId);
				purseSingleAssetFromDisk("audio", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), audioId, audioFormat);
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

//		// attach screenshot images - we only attach one per check-in (the
//		// latest screenshot)
//		Cursor cursor = app
//				.getContentResolver()
//				.query(Uri.parse(RfcxRole.ContentProvider.system.URI_SCREENSHOT),
//						RfcxRole.ContentProvider.system.PROJECTION_SCREENSHOT,
//						null, null, null);
//
//		if (cursor.getCount() > 0) { try { if (cursor.moveToFirst()) {
//
//			try {
//				String imgId = cursor.getString(cursor.getColumnIndex("timestamp"));
//				String imgFilePath = cursor.getString(cursor.getColumnIndex("filepath"));
//				if ((new File(imgFilePath)).exists() && (new File(imgFilePath)).canRead()) {
//					checkInFiles.add(new String[] { "screenshot", imgFilePath, "image/png" });
//					Log.d(logTag, "Screenshot attached: " + imgId + ".png");
//				} else {
//					Log.e(logTag, "Screenshot attachment file doesn't exist or isn't readable ("+imgId+"): "+ imgFilePath);
//					int deleteScreenShot = app.getContentResolver().delete(Uri.parse(RfcxRole.ContentProvider.system.URI_SCREENSHOT+"/"+imgId), null, null);
//				}
//			} catch (Exception e) {
//				RfcxLog.logExc(logTag, e);
//			}
//			
//		} } finally { cursor.close(); } }

		return checkInFiles;
	}
	
	public void processHttpCheckInResponse(String checkInResponse) {
		if ((checkInResponse != null) && !checkInResponse.isEmpty()) {
			
			this.requestSendDuration = (new Date()).getTime() - this.requestSendStart.getTime();
			
			processCheckInResponseJson(checkInResponse);

		}
	}

}
