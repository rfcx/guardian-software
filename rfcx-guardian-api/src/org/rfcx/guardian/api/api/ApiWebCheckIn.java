package org.rfcx.guardian.api.api;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.GZipUtils;
import org.rfcx.guardian.utility.HttpPostMultipart;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.ShellCommands;

import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

public class ApiWebCheckIn {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiWebCheckIn.class.getSimpleName();

	private RfcxGuardian app = null;
	HttpPostMultipart httpPostMultipart = new HttpPostMultipart();

	public Date requestSendStart = new Date();
	public Date requestSendReturned = new Date();

	private List<String> previousCheckIns = new ArrayList<String>();
	private Date checkInPreFlightTimestamp = new Date();

	private DateFormat timeZoneOffsetDateFormat = new SimpleDateFormat("Z");

	public int[] connectivityToggleThresholds = new int[] { 10, 17, 24, 30 };
	public boolean[] connectivityToggleThresholdsReached = new boolean[] { false, false, false, false };

	public void init(RfcxGuardian app) {
		this.app = app;
		// setting http post timeouts to the same as the audio capture interval.
		this.httpPostMultipart.setTimeOuts(app.AUDIO_CYCLE_DURATION, app.AUDIO_CYCLE_DURATION);
		// defining customized RFCx authentication headers (necessary for API access)
		List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
		rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/" + app.getDeviceId() });
		rfcxAuthHeaders.add(new String[] { "x-auth-token", app.getDeviceToken() });
		this.httpPostMultipart.setCustomHttpHeaders(rfcxAuthHeaders);
	}

	public String getCheckInUrl() {
		return app.rfcxPrefs.getPrefAsString("api_url_base") + "/v1/guardians/" + app.getDeviceId() + "/checkins";
	}

	public void sendCheckIn(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments, boolean allowAttachments, String checkInAudioReference) {
		if (!allowAttachments)
			keyFilepathMimeAttachments = new ArrayList<String[]>();
		if (app.isConnected) {
			this.requestSendStart = new Date();
			Log.i(TAG, "CheckIn sent at: " + (new DateTimeUtils()).getDateTime(this.requestSendStart));
			String checkInResponse = httpPostMultipart.doMultipartPost(fullUrl, keyValueParameters, keyFilepathMimeAttachments);
			processCheckInResponse(checkInResponse);
			if (checkInResponse.equals("Rfcx-Utils-HttpPostMultipart-UnknownHostException")) {
				Log.e(TAG, "NOT INCREMENTING CHECK-IN ATTEMPTS");
			} else {
				app.checkInDb.dbQueued.incrementSingleRowAttempts(checkInAudioReference);
			}
		} else {
			Log.d(TAG, "No connectivity... Can't send CheckIn");
		}
	}

	public boolean addCheckInToQueue(String[] audioInfo, String filepath) {

		// serialize audio info into JSON for checkin queue insertion
		String queueJson = generateCheckInQueueJson(audioInfo);
		
		// add audio info to checkin queue
		app.checkInDb.dbQueued.insert(
					audioInfo[1]+"."+audioInfo[2],
					queueJson, 
					"0", 
					filepath
				);

		Log.d(TAG, "Queued (1/"+app.checkInDb.dbQueued.getCount()+"): " + queueJson);
		
		// once queued, remove database reference from audio role
		int purgeAudioFromDb = app.getContentResolver().delete(Uri.parse(RfcxConstants.RfcxContentProvider.audio.URI_1 + "/" + audioInfo[1]), null, null);
		
		// if the queued table has grown beyond the maximum threshold, stash the oldest checkins 
		stashOldestCheckIns();
		
		return true;
	}
	
	private void stashOldestCheckIns() {
		
		List<String[]> checkInsBeyondStashThreshold = app.checkInDb.dbQueued.getQueuedWithOffset(app.CHECKIN_STASH_THRESHOLD, app.CHECKIN_ARCHIVE_THRESHOLD);
		
		if (checkInsBeyondStashThreshold.size() > 0) {
			
			// string list for reporting stashed checkins to the log
			List<String> stashList = new ArrayList<String>();
			
			//cycle through stashable checkins and move them to the new table/database
			for (String[] checkInsToStash : checkInsBeyondStashThreshold) {
				app.checkInDb.dbStashed.insert( checkInsToStash[1], checkInsToStash[2], checkInsToStash[3], checkInsToStash[4]);
				app.checkInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkInsToStash[1].substring(0,checkInsToStash[1].lastIndexOf(".")));
				stashList.add(checkInsToStash[1]);
			}
			
			//report in the logs
			Log.i(TAG, "Stashed CheckIns ("+app.checkInDb.dbStashed.getCount()+" total in database): "+TextUtils.join(" ", stashList));
		}
		
		if (((int) Integer.parseInt(app.checkInDb.dbStashed.getCount())) >= app.CHECKIN_ARCHIVE_THRESHOLD) {
			Log.i(TAG, "TODO: STASHED CHECKINS SHOULD BE ARCHIVED HERE...");
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
			Log.e(TAG, (e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			return "{}";
		}
	}

	private List<String> getRebootEvents() {
		List<String> rebootEvents = new ArrayList<String>();
		try {
			Cursor cursor = app.getContentResolver().query(
					Uri.parse(RfcxConstants.RfcxContentProvider.reboot.URI_1),
					RfcxConstants.RfcxContentProvider.reboot.PROJECTION_1,
					null, null, null);
			if (cursor.moveToFirst()) {
				do {
					rebootEvents.add(cursor.getString(cursor.getColumnIndex(RfcxConstants.RfcxContentProvider.reboot.PROJECTION_1[1])));
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			Log.e(TAG,(e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return rebootEvents;
	}
	
	private List<String> getInstalledSoftwareVersions() {

		List<String> softwareVersions = new ArrayList<String>();

		try {
			Cursor cursor = app.getContentResolver().query(
					Uri.parse(RfcxConstants.RfcxContentProvider.updater.URI_1),
					RfcxConstants.RfcxContentProvider.updater.PROJECTION_1,
					null, null, null);
			if (cursor.moveToFirst()) {
				do {
					softwareVersions
							.add(cursor.getString(cursor
									.getColumnIndex(RfcxConstants.RfcxContentProvider.updater.PROJECTION_1[0]))
									+ "*"
									+ cursor.getString(cursor
											.getColumnIndex(RfcxConstants.RfcxContentProvider.updater.PROJECTION_1[1])));
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			Log.e(TAG,(e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}

		return softwareVersions;
	}

	private JSONObject getSystemMetaDataAsJson(JSONObject metaDataJsonObj)
			throws JSONException {

		this.checkInPreFlightTimestamp = new Date();

		try {
			Cursor cursor = app.getContentResolver().query(
					Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_META),
					RfcxConstants.RfcxContentProvider.system.PROJECTION_META,
					null, null, null);
			if (cursor.moveToFirst()) {
				do {
					for (int i = 0; i < RfcxConstants.RfcxContentProvider.system.PROJECTION_META.length; i++) {
						metaDataJsonObj.put(
								RfcxConstants.RfcxContentProvider.system.PROJECTION_META[i],
								(cursor.getString(cursor.getColumnIndex(RfcxConstants.RfcxContentProvider.system.PROJECTION_META[i])) != null)
									? cursor.getString(cursor.getColumnIndex(RfcxConstants.RfcxContentProvider.system.PROJECTION_META[i]))
									: null
							);
					}
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			Log.e(TAG,(e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}

		return metaDataJsonObj;
	}

	public String packagePreFlightCheckInJson(String checkInJsonString) throws JSONException {

		JSONObject checkInMetaJson = getSystemMetaDataAsJson(new JSONObject(checkInJsonString));

		// Adding timestamp of metadata (JSON) snapshot
		checkInMetaJson.put("measured_at", checkInPreFlightTimestamp.getTime());

		// Adding GeoCoordinates
		JSONArray geoLocation = new JSONArray();
		geoLocation.put(3.6141375); // latitude... currently this is fake, obviously
		geoLocation.put(14.2108033); // longitude... currently this is fake, obviously
		geoLocation.put(1.000001); // precision... currently this is fake, obviously
		checkInMetaJson.put("location", geoLocation);

		// Adding latency data from previous checkins
		checkInMetaJson.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));

		// Recording number of currently queued/skipped checkins
		checkInMetaJson.put("queued_checkins", app.checkInDb.dbQueued.getCount());
		checkInMetaJson.put("skipped_checkins", app.checkInDb.dbSkipped.getCount());
		checkInMetaJson.put("stashed_checkins", app.checkInDb.dbStashed.getCount());

		// Adding software role versions
		checkInMetaJson.put("software", TextUtils.join("|", getInstalledSoftwareVersions()));
		
		// Adding reboot events
		checkInMetaJson.put("reboots", TextUtils.join("|", getRebootEvents()));

		// Adding device location timezone offset
		checkInMetaJson.put("timezone_offset", timeZoneOffsetDateFormat.format(Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault()).getTime()));

		// Adding messages to JSON blob
		checkInMetaJson.put("messages", getSmsMessagesAsJson());

		// Adding screenshot meta to JSON blob
		String[] latestScreenShot = getLatestScreenShotMeta();
		checkInMetaJson.put( "screenshots", (latestScreenShot != null) ? TextUtils.join("*", latestScreenShot) : null);

		// Stringify JSON, gzip the output and convert to base 64 string for sending
		String jsonFinal = checkInMetaJson.toString();
		String jsonFinalGZipped = (new GZipUtils()).gZipStringToBase64(jsonFinal);

		int pct = Math.round(100 * (1 - ((float) jsonFinalGZipped.length()) / ((float) jsonFinal.length())));
		Log.d(TAG, "JSON MetaData Packaged: " + pct + "% reduced");

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
				Log.i(TAG, "CheckIn request time: " + (checkInDuration / 1000) + " seconds");

				// clear system metadata included in successful checkin preflight
				int clearPreFlightSystemMetaData = 
						app.getContentResolver()
						.delete(Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_META
								+ "/" + checkInPreFlightTimestamp.getTime()),
								null, null);
				
				// clear reboot events included in successful checkin preflight
				int clearPreFlightRebootEvents = 
						app.getContentResolver()
						.delete(Uri.parse(RfcxConstants.RfcxContentProvider.reboot.URI_1
								+ "/" + checkInPreFlightTimestamp.getTime()),
								null, null);

				// parse audio info and use it to purge the data locally
				JSONArray audioJsonArray = new JSONArray(responseJson.getString("audio"));
				for (int i = 0; i < audioJsonArray.length(); i++) {
					JSONObject audioJson = audioJsonArray.getJSONObject(i);
					String audioFileNameInDb = app.checkInDb.dbQueued.getSingleRowByAudioAttachmentId(audioJson.getString("id"))[1];
					int purgeAudio = app.getContentResolver().delete(Uri.parse(RfcxConstants.RfcxContentProvider.audio.URI_1+"/"+audioFileNameInDb), null, null);
					app.checkInDb.dbQueued.deleteSingleRowByAudioAttachmentId(audioJson.getString("id"));
				}

				// parse the screenshot info and use it to purge the data locally
				JSONArray screenShotJsonArray = new JSONArray(
						responseJson.getString("screenshots"));
				for (int i = 0; i < screenShotJsonArray.length(); i++) {
					JSONObject screenShotJson = screenShotJsonArray
							.getJSONObject(i);
					int deleteScreenShot = app
							.getContentResolver()
							.delete(Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT
									+ "/" + screenShotJson.getString("id")),
									null, null);
				}

				// parse the message info and use it to purge the data locally
				JSONArray msgJsonArray = new JSONArray(
						responseJson.getString("messages"));
				for (int i = 0; i < msgJsonArray.length(); i++) {
					JSONObject msgJson = msgJsonArray.getJSONObject(i);
					int deleteMsg = app.getContentResolver().delete(
							Uri.parse("content://sms/"
									+ msgJson.getString("id")), null, null);
					if (deleteMsg == 1)
						Log.i(TAG,
								"deleted sms message with id "
										+ msgJson.getString("id"));
				}

				// parse the instructions section
				JSONObject instructionsJson = responseJson
						.getJSONObject("instructions");

				// handle prefs instuctions
				JSONObject prefsJson = instructionsJson.getJSONObject("prefs");

				// handle messages instructions
				JSONArray msgsJsonArr = instructionsJson
						.getJSONArray("messages");
				for (int i = 0; i < msgsJsonArr.length(); i++) {
					JSONObject msgJson = msgsJsonArr.getJSONObject(i);
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(msgJson.getString("address"),
							null, msgJson.getString("body"), null, null);
				}

			} catch (Exception e) {
				Log.e(TAG,(e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			} finally {
				Log.i(TAG, "API Response: " + checkInResponse);
			}
		}
	}

	public JSONArray getSmsMessagesAsJson() {
		JSONArray msgJsonArray = new JSONArray();
		Cursor cursor = app.getContentResolver().query(
				Uri.parse("content://sms/"), null, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				try {
					JSONObject msgJson = new JSONObject();
					msgJson.put("android_id",
							cursor.getString(cursor.getColumnIndex("_id")));
					msgJson.put("received_at",
							cursor.getLong(cursor.getColumnIndex("date")));
					msgJson.put("address",
							cursor.getString(cursor.getColumnIndex("address")));
					msgJson.put("body",
							cursor.getString(cursor.getColumnIndex("body")));
					msgJsonArray.put(msgJson);
				} catch (Exception e) {
					Log.e(TAG,(e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
				}
			} while (cursor.moveToNext());
		}
		return msgJsonArray;
	}

	public String[] getLatestScreenShotMeta() {

		// grab latest screenshot image meta - we only attach one per check-in
		// (the latest screenshot)
		Cursor cursor = app
				.getContentResolver()
				.query(Uri
						.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT),
						RfcxConstants.RfcxContentProvider.system.PROJECTION_SCREENSHOT,
						null, null, null);
		if (cursor.moveToFirst()) {
			try {
				return new String[] {
						cursor.getString(cursor.getColumnIndex("created_at")),
						cursor.getString(cursor.getColumnIndex("timestamp")),
						cursor.getString(cursor.getColumnIndex("format")),
						cursor.getString(cursor.getColumnIndex("digest")) };
			} catch (Exception e) {
				Log.e(TAG,(e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
		return null;
	}

	public List<String[]> loadCheckInFiles(String audioFilePath) {

		List<String[]> checkInFiles = new ArrayList<String[]>();

		// attach audio file - we only attach one per check-in
		String audioFileName = audioFilePath.substring(1 + audioFilePath.lastIndexOf("/"));
		String audioId = audioFileName.substring(0, audioFileName.lastIndexOf("."));
		String audioFormat = audioFileName.substring(1 + audioFileName.lastIndexOf("."));
		try {
			if ((new File(audioFilePath)).exists()) {
				checkInFiles.add(new String[] { "audio", audioFilePath, "audio/" + audioFormat });
				Log.d(TAG, "Audio attached: " + audioId + "." + audioFormat);
			} else {
				Log.e(TAG, "Audio attachment file doesn't exist: (" + audioId+ "." + audioFormat + ") " + audioFilePath);
				String audioFileNameInDb = app.checkInDb.dbQueued.getSingleRowByAudioAttachmentId(audioId)[1];
				int purgeAudio = app.getContentResolver().delete(Uri.parse(RfcxConstants.RfcxContentProvider.audio.URI_1 + "/" + audioFileNameInDb), null, null);
				app.checkInDb.dbQueued.deleteSingleRowByAudioAttachmentId(audioId);

			}
		} catch (Exception e) {
			Log.e(TAG,(e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}

		// attach screenshot images - we only attach one per check-in (the
		// latest screenshot)
		Cursor cursor = app
				.getContentResolver()
				.query(Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT),
						RfcxConstants.RfcxContentProvider.system.PROJECTION_SCREENSHOT,
						null, null, null);
		if (cursor.moveToFirst()) {
			try {
				String imgId = cursor.getString(cursor
						.getColumnIndex("timestamp"));
				String imgFilePath = cursor.getString(cursor
						.getColumnIndex("filepath"));
				if ((new File(imgFilePath)).exists()) {
					checkInFiles.add(new String[] { "screenshot", imgFilePath,
							"image/png" });
					Log.d(TAG, "Screenshot attached: " + imgId + ".png");
				} else {
					Log.e(TAG, "Screenshot attachment file doesn't exist: "
							+ imgFilePath);
					int deleteScreenShot = app
							.getContentResolver()
							.delete(Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT
									+ "/" + imgId), null, null);
				}
			} catch (Exception e) {
				Log.e(TAG,(e != null) ? (e.getMessage() + " ||| " + TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}

		return checkInFiles;
	}

	public void connectivityToggleCheck() {

		int secsSinceSuccess = (int) ((new Date()).getTime() - this.requestSendReturned
				.getTime()) / 1000;
		if ((secsSinceSuccess / 60) < this.connectivityToggleThresholds[0]) {
			// everything is going fine and we haven't even reached the first
			// threshold of bad connectivity
			this.connectivityToggleThresholdsReached = new boolean[] { false,
					false, false, false };
		} else if (!isBatteryChargeSufficientForCheckIn()) {
			// checkins are paused due to low battery level, so we are resetting
			// the connectivity problem thesholds
			this.connectivityToggleThresholdsReached = new boolean[] { false,
					false, false, false };
		} else {
			int thresholdIndex = 0;
			for (int toggleThreshold : this.connectivityToggleThresholds) {
				if (((secsSinceSuccess / 60) >= toggleThreshold)
						&& !this.connectivityToggleThresholdsReached[thresholdIndex]) {
					this.connectivityToggleThresholdsReached[thresholdIndex] = true;
					Log.d(TAG, "ToggleCheck: AirplaneMode (" + toggleThreshold
							+ " minutes since last successful CheckIn)");
					app.airplaneMode.setOff(app.getApplicationContext());
					if (toggleThreshold == this.connectivityToggleThresholds[this.connectivityToggleThresholds.length - 1]) {
						// last index, force reboot
						Log.d(TAG, "ToggleCheck: ForcedReboot ("
								+ toggleThreshold
								+ " minutes since last successful CheckIn)");
						(new ShellCommands()).executeCommand("reboot", null,
								false, app.getApplicationContext());
					}
				}
				thresholdIndex++;
			}
		}
	}

	public boolean isBatteryChargeSufficientForCheckIn() {
		int batteryCharge = app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= app.CHECKIN_BATTERY_CUTOFF);
	}

}
