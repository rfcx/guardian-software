package org.rfcx.guardian.api.checkin;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.GZipUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.audio.RfcxAudio;
import org.rfcx.guardian.utility.device.DeviceDiskUsage;
import org.rfcx.guardian.utility.device.DeviceGeoLocation;
import org.rfcx.guardian.utility.http.HttpPostMultipart;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

public class ApiCheckInUtils {
	
	public ApiCheckInUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		// setting http post timeouts to the same as the audio capture interval.
		setCheckInTimeOuts(this.app.rfcxPrefs.getPrefAsInt("audio_cycle_duration"));
		setCheckInAuthHeaders(this.app.rfcxDeviceId.getDeviceGuid(), this.app.rfcxDeviceId.getDeviceToken());
	}

	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiCheckInUtils.class.getSimpleName();

	private RfcxGuardian app;
	HttpPostMultipart httpPostMultipart = new HttpPostMultipart();

	public Date requestSendStart = new Date();
	public Date requestSendReturned = new Date();

	private List<String> previousCheckIns = new ArrayList<String>();
	private Date checkInPreFlightTimestamp = new Date();

	public int[] connectivityToggleThresholds = new int[] { 10, 17, 24, 30 };
	public boolean[] connectivityToggleThresholdsReached = new boolean[] { false, false, false, false };
	
	private void setCheckInAuthHeaders(String deviceGuid, String deviceToken) {
		List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
		rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/" + deviceGuid });
		rfcxAuthHeaders.add(new String[] { "x-auth-token", deviceToken });
		this.httpPostMultipart.setCustomHttpHeaders(rfcxAuthHeaders);
	}
	
	private void setCheckInTimeOuts(int timeOut) {
		this.httpPostMultipart.setTimeOuts(timeOut, timeOut);
	}

	public String getCheckInUrl() {
		return app.rfcxPrefs.getPrefAsString("api_url_base") + "/v1/guardians/" + app.rfcxDeviceId.getDeviceGuid() + "/checkins";
	}

	public void sendCheckIn(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments, boolean allowAttachments, String checkInAudioReference) {
		if (!allowAttachments)
			keyFilepathMimeAttachments = new ArrayList<String[]>();
		if (app.deviceConnectivity.isConnected()) {
			this.requestSendStart = new Date();
			Log.i(logTag, "CheckIn sent at: " + DateTimeUtils.getDateTime(this.requestSendStart));
			String checkInResponse = httpPostMultipart.doMultipartPost(fullUrl, keyValueParameters, keyFilepathMimeAttachments);
			processCheckInResponse(checkInResponse);
			if (checkInResponse.equals("Rfcx-Utils-HttpPostMultipart-UnknownHostException")) {
				Log.e(logTag, "NOT INCREMENTING CHECK-IN ATTEMPTS");
			} else {
				app.apiCheckInDb.dbQueued.incrementSingleRowAttempts(checkInAudioReference);
			}
		} else {
			Log.d(logTag, "No connectivity... Can't send CheckIn");
		}
	}
	
	public static boolean validateCheckInAttachments(List<String[]> checkInFiles) {
		boolean includesAudio = false;
		boolean includesScreenShot = false;
		for (String[] fileItems : checkInFiles) {
			if (fileItems[0].equals("audio")) { includesAudio = true; }
			if (fileItems[0].equals("screenshot")) { includesScreenShot = true; }
		}
		return (includesAudio);
	}

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
			metaDataJsonObj.put("reboots",		app.deviceRebootDb.dbReboot.getConcatRows());
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
			app.deviceRebootDb.dbReboot.clearRowsBefore(deleteBefore);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	public String packagePreFlightCheckInJson(String checkInJsonString) throws JSONException {

		JSONObject checkInMetaJson = getSystemMetaDataAsJson(new JSONObject(checkInJsonString));

		// Adding timestamp of metadata (JSON) snapshot
		checkInMetaJson.put("measured_at", checkInPreFlightTimestamp.getTime());

		// Adding GeoCoordinates
		DeviceGeoLocation TEMPORARY_DEVICE_GEOLOCATION_PLACEHOLDER = new DeviceGeoLocation(RfcxGuardian.APP_ROLE);
		checkInMetaJson.put("location", TEMPORARY_DEVICE_GEOLOCATION_PLACEHOLDER.getSerializedGeoLocation());

		// Adding latency data from previous checkins
		checkInMetaJson.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));

		// Recording number of currently queued/skipped/stashed checkins
		checkInMetaJson.put("queued_checkins", app.apiCheckInDb.dbQueued.getCount());
		checkInMetaJson.put("skipped_checkins", app.apiCheckInDb.dbSkipped.getCount());
		checkInMetaJson.put("stashed_checkins", app.apiCheckInDb.dbStashed.getCount());

		// Adding software role versions
		checkInMetaJson.put("software", TextUtils.join("|", getInstalledSoftwareVersions()));

		// Adding device location timezone offset
		checkInMetaJson.put("timezone_offset", DateTimeUtils.getTimeZoneOffset());
		
		// Adding messages to JSON blob
		checkInMetaJson.put("messages", getSmsMessagesAsJson());

		// Adding screenshot meta to JSON blob
		String[] latestScreenShot = getLatestScreenShotMeta();
		checkInMetaJson.put( "screenshots", (latestScreenShot != null) ? TextUtils.join("*", latestScreenShot) : null);

		// Stringify JSON, gzip the output and convert to base 64 string for sending
		String jsonFinal = checkInMetaJson.toString();
		String jsonFinalGZipped = GZipUtils.gZipStringToBase64(jsonFinal);
		
		Log.d(logTag, checkInMetaJson.toString());

		int pct = Math.round(100 * (1 - ((float) jsonFinalGZipped.length()) / ((float) jsonFinal.length())));
		Log.d(logTag, "JSON MetaData Packaged: " + pct + "% reduced");

		return jsonFinalGZipped;
	}

	public void processCheckInResponse(String checkInResponse) {
		if ((checkInResponse != null) && !checkInResponse.isEmpty()) {

			try {
				// parse response json
				JSONObject responseJson = new JSONObject(checkInResponse);

				// reset/record request latency
				long checkInDuration = System.currentTimeMillis() - this.requestSendStart.getTime();
				this.previousCheckIns = new ArrayList<String>();
				this.previousCheckIns.add(responseJson.getString("checkin_id") + "*" + checkInDuration);
				this.requestSendReturned = new Date();
				Log.i(logTag, "CheckIn request time: " + (checkInDuration / 1000) + " seconds");

				// clear system metadata included in successful checkin preflight
				clearPreFlightSystemMetaData(checkInPreFlightTimestamp);

				// parse audio info and use it to purge the data locally
				JSONArray audioJsonArray = new JSONArray(responseJson.getString("audio"));
				for (int i = 0; i < audioJsonArray.length(); i++) {
					JSONObject audioJson = audioJsonArray.getJSONObject(i);
					String audioFileNameInDb = app.apiCheckInDb.dbQueued.getSingleRowByAudioAttachmentId(audioJson.getString("id"))[1];
					String[] audioFromDb = app.audioEncodeDb.dbEncoded.getSingleRowByAudioId(audioJson.getString("id"));
					if (audioFromDb[1] != null) { app.audioEncodeDb.dbEncoded.deleteSingleRow(audioFromDb[1]); }
					app.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(audioJson.getString("id"));
					purgeSingleAudioAssetFromDisk(app.getApplicationContext(), audioJson.getString("id"), audioFileNameInDb.substring(1+audioFileNameInDb.lastIndexOf(".")));
				}

//				// parse the screenshot info and use it to purge the data locally
//				JSONArray screenShotJsonArray = new JSONArray(responseJson.getString("screenshots"));
//				for (int i = 0; i < screenShotJsonArray.length(); i++) {
//					JSONObject screenShotJson = screenShotJsonArray.getJSONObject(i);
//					int deleteScreenShot = app.getContentResolver()
//							.delete(Uri.parse(RfcxRole.ContentProvider.system.URI_SCREENSHOT
//									+ "/" + screenShotJson.getString("id")), null, null);
//				}

				// parse the message info and use it to purge the data locally
				JSONArray msgJsonArray = new JSONArray(responseJson.getString("messages"));
				for (int i = 0; i < msgJsonArray.length(); i++) {
					JSONObject msgJson = msgJsonArray.getJSONObject(i);
					int deleteMsg = app.getContentResolver().delete(
							Uri.parse("content://sms/"+ msgJson.getString("id")), null, null);
					if (deleteMsg == 1)
						Log.i(logTag, "deleted sms message with id "+ msgJson.getString("id"));
				}

				// parse the instructions section
				JSONObject instructionsJson = responseJson.getJSONObject("instructions");

				// handle messages instructions
				JSONArray msgsJsonArr = instructionsJson.getJSONArray("messages");
				for (int i = 0; i < msgsJsonArr.length(); i++) {
					JSONObject msgJson = msgsJsonArr.getJSONObject(i);
					(SmsManager.getDefault()).sendTextMessage(msgJson.getString("address"), null, msgJson.getString("body"), null, null);
				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				Log.i(logTag, "API Response: " + checkInResponse);
			}
		}
	}

	public JSONArray getSmsMessagesAsJson() {
		JSONArray msgJsonArray = new JSONArray();
		Cursor cursor = app.getContentResolver().query(Uri.parse("content://sms/"), null, null, null, null);
		
		if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
			do {
				try {
					JSONObject msgJson = new JSONObject();
					msgJson.put("android_id", cursor.getString(cursor.getColumnIndex("_id")));
					msgJson.put("received_at", cursor.getLong(cursor.getColumnIndex("date")));
					msgJson.put("address", cursor.getString(cursor.getColumnIndex("address")));
					msgJson.put("body", cursor.getString(cursor.getColumnIndex("body")));
					msgJsonArray.put(msgJson);
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
				
		return msgJsonArray;
	}

	public String[] getLatestScreenShotMeta() {

//		// grab latest screenshot image meta - we only attach one per check-in
//		// (the latest screenshot)
//		Cursor cursor = app
//				.getContentResolver()
//				.query(Uri.parse(RfcxRole.ContentProvider.system.URI_SCREENSHOT),
//						RfcxRole.ContentProvider.system.PROJECTION_SCREENSHOT,
//						null, null, null);
//
//		if (cursor.getCount() > 0) { try { if (cursor.moveToFirst()) {
//			try {
//				return new String[] {
//						cursor.getString(cursor.getColumnIndex("created_at")),
//						cursor.getString(cursor.getColumnIndex("timestamp")),
//						cursor.getString(cursor.getColumnIndex("format")),
//						cursor.getString(cursor.getColumnIndex("digest")) };
//			} catch (Exception e) {
//				RfcxLog.logExc(logTag, e);
//			}
//		} } finally { cursor.close(); } }
		
		return null;
	}

	public List<String[]> loadCheckInFiles(String audioFilePath) {

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
				purgeSingleAudioAssetFromDisk(app.getApplicationContext(), audioId, audioFormat);
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
					Log.d(logTag, "ToggleCheck: AirplaneMode (" + toggleThreshold + " minutes since last successful CheckIn)");
					app.deviceAirplaneMode.setOff(app.getApplicationContext());
					if (toggleThreshold == this.connectivityToggleThresholds[this.connectivityToggleThresholds.length - 1]) {
						// last index, force reboot
						Log.d(logTag, "ToggleCheck: ForcedReboot (" + toggleThreshold + " minutes since last successful CheckIn)");
						ShellCommands.executeCommand("reboot", null, false, app.getApplicationContext());
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
	
	private static void purgeSingleAudioAssetFromDisk(Context context, String audioTimestamp, String audioFileExtension) {
		try {
			(new File(RfcxAudio.getAudioFileLocation_Complete_PostZip(context, (long) Long.parseLong(audioTimestamp),audioFileExtension))).delete();
			Log.d(logTag, "Purging audio asset: "+audioTimestamp+"."+audioFileExtension);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

}
