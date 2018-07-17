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
		
		// setting http post timeouts to the same as the audio capture interval.
//		setHttpCheckInTimeOuts(this.app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") * 1000);
//		setHttpCheckInAuthHeaders(this.app.rfcxDeviceGuid.getDeviceGuid(), this.app.rfcxDeviceGuid.getDeviceToken());
		
		this.mqttCheckInClient = new MqttUtils(RfcxGuardian.APP_ROLE, this.app.rfcxDeviceGuid.getDeviceGuid());
		this.mqttCheckInClient.setOrResetBroker("tcp", 1883, this.app.rfcxPrefs.getPrefAsString("api_checkin_host"));
		this.mqttCheckInClient.setCallback(this);
		this.mqttCheckInClient.setActionTimeout(this.requestTimeOutLength);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInUtils.class);

	private RfcxGuardian app;
	private MqttUtils mqttCheckInClient = null;

	public Date requestSendStart = new Date();
	public Date requestSendReturned = new Date();
	public long requestSendDuration = 0;
	
	public long requestTimeOutLength = 0;
	
//	private String[] inFlightCheckInEntry = null;
	
	private Map<String, String[]> inFlightCheckIns = new HashMap<String, String[]>();

	private List<String> previousCheckIns = new ArrayList<String>();
	private Date checkInPreFlightTimestamp = new Date();

	private int[] failedCheckInThresholds = new int[0];
	private boolean[] failedCheckInThresholdsReached = new boolean[0];

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
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInsToStash[1]);
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
		
		for (String appRole : RfcxRole.ALL_ROLES) {
			
			if (appRole.equalsIgnoreCase(RfcxGuardian.APP_ROLE)) {
				
			} else {
				
			}
		}
		
		//RfcxRole.getRoleVersion(app.getApplicationContext(), logTag)

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
			
			RfcxComm.deleteQueryContentProvider("admin", "database_delete_rows_before", "sentinel_power|"+deleteBefore.getTime(), app.getApplicationContext().getContentResolver());
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	public String buildCheckInJson(String checkInJsonString, String[] screenShotMeta, String[] logFileMeta) throws JSONException, IOException {
		
		JSONObject checkInMetaJson = getSystemMetaDataAsJson(new JSONObject(checkInJsonString));
		
		// Adding Guardian GUID
		checkInMetaJson.put("guardian_guid", this.app.rfcxDeviceGuid.getDeviceGuid());

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
		checkInMetaJson.put( "screenshots", (screenShotMeta[0] != null) ? (screenShotMeta[1]+"*"+screenShotMeta[2]+"*"+screenShotMeta[3]+"*"+screenShotMeta[4]) : "");

		// Adding logs meta to JSON blob
		checkInMetaJson.put( "logs", (logFileMeta[0] != null) ? (logFileMeta[1]+"*"+logFileMeta[2]+"*"+logFileMeta[3]+"*"+logFileMeta[4]) : "");
		
		// Adding sentinel data, if they can be retrieved
//		JSONArray sentinelPower = RfcxComm.getQueryContentProvider("admin", "database_get_all_rows", "sentinel_power", app.getApplicationContext().getContentResolver());
//		for (int i = 0; i < sentinelPower.length(); i++) {
////			checkInMetaJson.put("sentinel_power", sentinelPower.getJSONObject(i));
//		}
		
		return checkInMetaJson.toString();
		
	}
	
	public byte[] packageMqttPayload(String checkInJsonString, String checkInAudioFilePath) throws UnsupportedEncodingException, IOException, JSONException {
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		
		String[] screenShotMeta = getLatestExternalAssetMeta("screenshots");
		if ((screenShotMeta[0] != null) && !(new File(screenShotMeta[0])).exists()) { purgeSingleAsset("screenshot", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), screenShotMeta[2], screenShotMeta[3]); }
		
		String[] logFileMeta = getLatestExternalAssetMeta("logs");
		if ((logFileMeta[0] != null) && !(new File(logFileMeta[0])).exists()) { purgeSingleAsset("log", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), logFileMeta[2], logFileMeta[3]); }
		
		byte[] jsonBlobAsBytes = StringUtils.gZipStringToByteArray(buildCheckInJson(checkInJsonString, screenShotMeta, logFileMeta));
		String jsonBlobMetaSection = String.format("%012d", jsonBlobAsBytes.length);
		byteArrayOutputStream.write(jsonBlobMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(jsonBlobAsBytes);
		
		byte[] audioFileAsBytes = new byte[0];
		if ((new File(checkInAudioFilePath)).exists()) { audioFileAsBytes = FileUtils.fileAsByteArray(checkInAudioFilePath); }
		String audioFileMetaSection = String.format("%012d", audioFileAsBytes.length);
		byteArrayOutputStream.write(audioFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(audioFileAsBytes);
		
		byte[] screenShotFileAsBytes = new byte[0];
		if ((screenShotMeta[0] != null) && (new File(screenShotMeta[0])).exists()) { screenShotFileAsBytes = FileUtils.fileAsByteArray(screenShotMeta[0]); }
		String screenShotFileMetaSection = String.format("%012d", screenShotFileAsBytes.length);
		byteArrayOutputStream.write(screenShotFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(screenShotFileAsBytes);
		
		byte[] logFileAsBytes = new byte[0];
		if ((logFileMeta[0] != null) && (new File(logFileMeta[0])).exists()) { logFileAsBytes = FileUtils.fileAsByteArray(logFileMeta[0]); }
		String logFileMetaSection = String.format("%012d", logFileAsBytes.length);
		byteArrayOutputStream.write(logFileMetaSection.getBytes("UTF-8"));
		byteArrayOutputStream.write(logFileAsBytes);
		
		byteArrayOutputStream.close();
		
		return byteArrayOutputStream.toByteArray();
	}
	
	public void sendMqttCheckIn(String[] checkInDatabaseEntry) {
		
		try {
			
			String audioId = checkInDatabaseEntry[1].substring(0,checkInDatabaseEntry[1].lastIndexOf("."));
			String audioExt = checkInDatabaseEntry[1].substring(1+checkInDatabaseEntry[1].lastIndexOf("."));
			String audioPath = checkInDatabaseEntry[4];
			String audioJson = checkInDatabaseEntry[2];
			byte[] checkInPayload = packageMqttPayload(audioJson, audioPath);
			
			if ((new File(audioPath)).exists()) {
				this.inFlightCheckIns.remove("0");
				this.inFlightCheckIns.put("0", checkInDatabaseEntry);
				this.requestSendStart = this.mqttCheckInClient.publishMessage(checkInPayload);
			} else {
				purgeSingleAsset("audio", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), audioId, audioExt );
			}
			
		} catch (Exception e) {
			
			if (RfcxLog.getExceptionContentAsString(e).contains("UnknownHostException")) {
				Log.e(logTag, "UnknownHostException");
			} else {
				RfcxLog.logExc(logTag, e);
			}
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

	private void initializeFailedCheckInThresholds() {
		
		String[] checkInThresholdsStr = TextUtils.split(app.rfcxPrefs.getPrefAsString("checkin_failure_thresholds"), ",");
		
		int[] checkInThresholds = new int[checkInThresholdsStr.length];
		boolean[] checkInThresholdsReached = new boolean[checkInThresholdsStr.length];
		
		for (int i = 0; i < checkInThresholdsStr.length; i++) { try { 
			checkInThresholds[i] = Integer.parseInt(checkInThresholdsStr[i]); 
			checkInThresholdsReached[i] = false; 
		} catch(Exception e) { RfcxLog.logExc(logTag, e); } }
		
		this.failedCheckInThresholds = checkInThresholds;
		this.failedCheckInThresholdsReached = checkInThresholdsReached;
	}
	
	public void updateFailedCheckInThresholds() {

		if (this.failedCheckInThresholds.length > 0) {
		
			int minsSinceSuccess = (int) Math.floor((((new Date()).getTime() - this.requestSendReturned.getTime()) / 1000) / 60);
			
			if (		(minsSinceSuccess < this.failedCheckInThresholds[0]) 
				|| 	app.rfcxPrefs.getPrefAsBoolean("checkin_offline_mode")
				|| 	!isBatteryChargeSufficientForCheckIn()
				) {
				// 1) everything is going fine and we haven't even reached the first threshold of bad connectivity
				// OR 2) checkins are paused due to low battery level, so we are resetting the connectivity problem thresholds
				for (int i = 0; i < this.failedCheckInThresholdsReached.length; i++) {
					this.failedCheckInThresholdsReached[i] = false;
				}
			} else {
				int i = 0;
				for (int toggleThreshold : this.failedCheckInThresholds) {
					if ((minsSinceSuccess >= toggleThreshold) && !this.failedCheckInThresholdsReached[i]) {
						this.failedCheckInThresholdsReached[i] = true;
						if (toggleThreshold == this.failedCheckInThresholds[this.failedCheckInThresholds.length-1]) {
							// last index, force role(s) relaunch
							Log.d(logTag, "ToggleCheck: Forced Relaunch (" + toggleThreshold + " minutes since last successful CheckIn)");
							app.deviceControlUtils.runOrTriggerDeviceControl("relaunch", app.getApplicationContext().getContentResolver());
						} else {
							Log.d(logTag, "ToggleCheck: Airplane Mode (" + toggleThreshold + " minutes since last successful CheckIn)");
							app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_off", app.getApplicationContext().getContentResolver());
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
		return (app.deviceBattery.isBatteryCharged(app.getApplicationContext(), null) && !isBatteryChargeSufficientForCheckIn());
	}
	
	private void purgeSingleAsset(String assetType, String rfcxDeviceId, Context context, String timestamp, String fileExtension) {
		
		try {
			String filePath = null;
			
			if (assetType.equals("audio")) {
				filePath = RfcxAudioUtils.getAudioFileLocation_Complete_PostGZip(rfcxDeviceId, context, (long) Long.parseLong(timestamp), fileExtension);
				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(timestamp);
				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(timestamp);
				app.audioEncodeDb.dbEncoded.deleteSingleRow(timestamp);
				
			} else if (assetType.equals("screenshot")) {
				filePath = DeviceScreenShot.getScreenShotFileLocation_Complete(rfcxDeviceId, context, (long) Long.parseLong(timestamp));
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "screenshots|"+timestamp, app.getApplicationContext().getContentResolver());
				
			} else if (assetType.equals("log")) {
				filePath = DeviceLogCatCapture.getLogFileLocation_Complete_PostZip(rfcxDeviceId, context, (long) Long.parseLong(timestamp));
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "logs|"+timestamp, app.getApplicationContext().getContentResolver());
			}
			
			if ((new File(filePath)).exists()) { (new File(filePath)).delete(); }
			
			Log.d(logTag, "Purging asset: "+assetType+", "+timestamp+", "+filePath.substring(1+filePath.lastIndexOf("/")));
			
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
				purgeSingleAsset("audio", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), audioId, this.app.rfcxPrefs.getPrefAsString("audio_encode_codec"));
				this.inFlightCheckIns.remove("0");
			}
			
			// parse screenshot info and use it to purge the data locally
			JSONArray screenShotJson = jsonObj.getJSONArray("screenshots");
			for (int i = 0; i < screenShotJson.length(); i++) {
				String screenShotId = screenShotJson.getJSONObject(i).getString("id");
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "screenshots|"+screenShotId, app.getApplicationContext().getContentResolver());
				purgeSingleAsset("screenshot", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), screenShotId, "png");
			}
			
			// parse log info and use it to purge the data locally
			JSONArray logsJson = jsonObj.getJSONArray("logs");
			for (int i = 0; i < logsJson.length(); i++) {
				String logFileId = logsJson.getJSONObject(i).getString("id");
				RfcxComm.deleteQueryContentProvider("admin", "database_delete_row", "logs|"+logFileId, app.getApplicationContext().getContentResolver());
				purgeSingleAsset("log", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), logFileId, "log");
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
			JSONArray instrMsgsJson = instructionsJson.getJSONArray("messages");
			for (int i = 0; i < instrMsgsJson.length(); i++) {
				JSONObject instrMsgJson = instrMsgsJson.getJSONObject(i);
				RfcxComm.getQueryContentProvider("admin", "sms_send", instrMsgJson.getString("address")+"|"+instrMsgJson.getString("body"), app.getApplicationContext().getContentResolver());
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
			this.app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInEntry[1]);
		}
	}
	
	@Override
	public void messageArrived(String messageTopic, MqttMessage mqttMessage) throws Exception {
		
		if (messageTopic.equalsIgnoreCase("guardians/"+this.app.rfcxDeviceGuid.getDeviceGuid())) {
			processCheckInResponseJson(StringUtils.UnGZipByteArrayToString(mqttMessage.getPayload()));
		}
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryToken) {
		
		moveCheckInEntryToSentDatabase("0");
		this.requestSendDuration = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(this.requestSendStart));
		Log.i(logTag, (new StringBuilder()).append("CheckIn delivery time: ").append(DateTimeUtils.milliSecondDurationAsReadableString(this.requestSendDuration, true)).toString() );

	}
	
	@Override
	public void connectionLost(Throwable cause) {
		
		Log.e(logTag, (new StringBuilder()).append("Connection lost. ").append( DateTimeUtils.timeStampDifferenceFromNowAsReadableString(this.requestSendStart) ).append(" since last CheckIn publication was launched").toString());
		//		cause.printStackTrace();
		RfcxLog.logThrowable(logTag, cause);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

//	HttpPostMultipart httpPostMultipart = new HttpPostMultipart();
//	
//	private void setHttpCheckInAuthHeaders(String deviceGuid, String deviceToken) {
//		List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
//		rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/" + deviceGuid });
//		rfcxAuthHeaders.add(new String[] { "x-auth-token", deviceToken });
//		this.httpPostMultipart.setCustomHttpHeaders(rfcxAuthHeaders);
//	}
//	
//	private void setHttpCheckInTimeOuts(int timeOut) {
//		this.httpPostMultipart.setTimeOuts(timeOut, timeOut);
//	}
//
//	public String getHttpCheckInUrl() {
//		return app.rfcxPrefs.getPrefAsString("api_checkin_host") + "/v1/guardians/" + app.rfcxDeviceGuid.getDeviceGuid() + "/checkins";
//	}
//	
//	
//	public void prepAndSendHttpCheckIn(String[] queuedCheckInRow) {
//		List<String[]> stringParameters = new ArrayList<String[]>();
//		stringParameters.add(new String[] { "meta", packageHttpCheckInJson(queuedCheckInRow[2]) });
//		List<String[]> checkInFiles = loadHttpCheckInFiles(queuedCheckInRow[4]);
//		
//		if (ApiCheckInUtils.validateHttpCheckInAttachments(checkInFiles)) {
//			sendHttpCheckIn(
//				getHttpCheckInUrl(),
//				stringParameters, 
//				checkInFiles,
//				true, // allow (or, if false, block) file attachments (audio/screenshots)
//				queuedCheckInRow[1]
//			);	
//		}
//	}
//
//	public void sendHttpCheckIn(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments, boolean allowAttachments, String checkInAudioReference) {
//		if (!allowAttachments)
//			keyFilepathMimeAttachments = new ArrayList<String[]>();
//		if (app.deviceConnectivity.isConnected()) {
//			this.requestSendStart = new Date();
//			Log.i(logTag, "CheckIn sent at: " + DateTimeUtils.getDateTime(this.requestSendStart));
//			String checkInResponse = httpPostMultipart.doMultipartPost(fullUrl, keyValueParameters, keyFilepathMimeAttachments);
//			processHttpCheckInResponse(checkInResponse);
//			if (checkInResponse.equals("Rfcx-Utils-HttpPostMultipart-UnknownHostException")) {
//				Log.e(logTag, "NOT INCREMENTING CHECK-IN ATTEMPTS");
//			} else {
//				app.apiCheckInDb.dbQueued.incrementSingleRowAttempts(checkInAudioReference);
//			}
//		} else {
//			Log.d(logTag, "No connectivity... Can't send CheckIn");
//		}
//	}
//	
//	public static boolean validateHttpCheckInAttachments(List<String[]> checkInFiles) {
//		boolean includesAudio = false;
//		boolean includesScreenShot = false;
//		for (String[] fileItems : checkInFiles) {
//			if (fileItems[0].equals("audio")) { includesAudio = true; }
//			if (fileItems[0].equals("screenshot")) { includesScreenShot = true; }
//		}
//		return (includesAudio);
//	}
//
//	public String packageHttpCheckInJson(String checkInJsonString) throws JSONException, IOException {
//
//		// For HTTP
//		// Stringify JSON, gzip the output and convert to base 64 string for sending	
//		String jsonFinal = buildCheckInJson(checkInJsonString, getLatestExternalAssetMeta("screenshots"), getLatestExternalAssetMeta("logs"));	
//		Log.d(logTag, jsonFinal);
//		String jsonFinalGZipped = StringUtils.gZipStringToBase64(jsonFinal);
//
//		return jsonFinalGZipped;
//	}
//
//
//	public List<String[]> loadHttpCheckInFiles(String audioFilePath) {
//
//		List<String[]> checkInFiles = new ArrayList<String[]>();
//
//		// attach audio file - we only attach one per check-in
//		String audioFileName = audioFilePath.substring(1 + audioFilePath.lastIndexOf("/"));
//		String audioId = audioFileName.substring(0, audioFileName.lastIndexOf("."));
//		String audioFormat = audioFileName.substring(1 + audioFileName.lastIndexOf("."));
//		try {
//			if ((new File(audioFilePath)).exists() && (new File(audioFilePath)).canRead()) {
//				checkInFiles.add(new String[] { "audio", audioFilePath, "audio/" + audioFormat });
//				Log.d(logTag, "Audio attached: " + audioId + "." + audioFormat);
//			} else {
//				Log.e(logTag, "Audio attachment file doesn't exist or isn't readable: (" + audioId+ "." + audioFormat + ") " + audioFilePath);
//				String audioFileNameInDb = app.apiCheckInDb.dbQueued.getSingleRowByAudioAttachmentId(audioId)[1];
//				String[] audioFromDb = app.audioEncodeDb.dbEncoded.getSingleRowByAudioId(audioFileNameInDb);
//				if (audioFromDb[1] != null) { app.audioEncodeDb.dbEncoded.deleteSingleRow(audioFromDb[1]); }
//				app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(audioId);
//				app.apiCheckInDb.dbSent.deleteSingleRowByAudioAttachmentId(audioId);
//				purseSingleAssetFromDisk("audio", app.rfcxDeviceGuid.getDeviceGuid(), app.getApplicationContext(), audioId, audioFormat);
//			}
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}
//
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
//
//		return checkInFiles;
//	}
//	
//	public void processHttpCheckInResponse(String checkInResponse) {
//		if ((checkInResponse != null) && !checkInResponse.isEmpty()) {
//			
//			this.requestSendDuration = (new Date()).getTime() - this.requestSendStart.getTime();
//			
//			processCheckInResponseJson(checkInResponse);
//
//		}
//	}

}
