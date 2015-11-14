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
import android.util.Base64;
import android.util.Log;

public class ApiWebCheckIn {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+ApiWebCheckIn.class.getSimpleName();

	private RfcxGuardian app = null;
	HttpPostMultipart httpPostMultipart = new HttpPostMultipart();
	
	public Date requestSendStart = new Date();
	public Date requestSendReturned = new Date();
	
	private List<String> previousCheckIns = new ArrayList<String>();
	private Date checkInPreFlightTimestamp = new Date();
	
	private DateFormat timeZoneOffsetDateFormat = new SimpleDateFormat("Z");
	
	public long apiCheckInTriggerPeriod = 15000;
	
	public int[] connectivityToggleThresholds = new int[] { 10, 20, 30, 40 };
	public boolean[] connectivityToggleThresholdsReached = new boolean[] { false, false, false, false };
	
	public int maximumCheckInAttemptsBeforeSkip = 5;
	public int pauseCheckInsIfBatteryPercentageIsBelow = 90;
	
	public void init(RfcxGuardian app) {
		this.app = app;
		// setting http post timeouts to the same as the audio capture interval.
		int audioCaptureInterval = 1000*((int) Integer.parseInt(app.getPref("audio_capture_interval")));
		this.httpPostMultipart.setTimeOuts(audioCaptureInterval, audioCaptureInterval);
		// setting customized rfcx authentication headers (necessary for API access)
		List<String[]> rfcxAuthHeaders = new ArrayList<String[]>();
		rfcxAuthHeaders.add(new String[] { "x-auth-user", "guardian/"+app.getDeviceId() });
		rfcxAuthHeaders.add(new String[] { "x-auth-token", app.getDeviceToken() });
		this.httpPostMultipart.setCustomHttpHeaders(rfcxAuthHeaders);
	}
	
	public String getCheckInUrl() {
		return app.getPref("api_domain")+"/v1/guardians/"+app.getDeviceId()+"/checkins";
	}
	
	public void sendCheckIn(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments, boolean allowAttachments, String checkInAudioReference) {
		if (!allowAttachments) keyFilepathMimeAttachments = new ArrayList<String[]>();
		if (app.isConnected) {
			this.requestSendStart = new Date();
			Log.i(TAG,"CheckIn sent at: "+requestSendStart.toLocaleString());
			String checkInResponse = httpPostMultipart.doMultipartPost(fullUrl, keyValueParameters, keyFilepathMimeAttachments);
			processCheckInResponse(checkInResponse);
			if (checkInResponse.equals("Rfcx-"+RfcxConstants.ROLE_NAME+"-HttpPostMultipart-UnknownHostException")) {
				Log.e(TAG,"NOT INCREMENTING CHECK-IN ATTEMPTS");
			} else {
				app.checkInDb.dbQueued.incrementSingleRowAttempts(checkInAudioReference);
			}
		} else {
			Log.d(TAG,"No connectivity... Can't send CheckIn");
		}
	}
	
	public boolean addCheckInToQueue(String[] audioInfo, String filepath) {
		
		String queueJson = generateCheckInQueueJson(audioInfo);
		
		app.checkInDb.dbQueued.insert(
				audioInfo[1]+"."+audioInfo[2], 
				queueJson, 
				"0", 
				filepath
			);
		
		Log.i(TAG, "CheckIn Queued: "+queueJson);
		
		return true;
	}
	
	private String generateCheckInQueueJson(String[] audioFileInfo) {
		
		try {
			JSONObject queueJson = new JSONObject();
			
			// Recording the moment the check in was queued
			queueJson.put("queued_at", (new DateTimeUtils()).getDateTime(new Date()));
			
			// Adding audio file metadata
			List<String> audioFiles = new ArrayList<String>();
			audioFiles.add(TextUtils.join("*", audioFileInfo));
			queueJson.put("audio", TextUtils.join("|", audioFiles));
			
			return queueJson.toString();
			
		} catch (JSONException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			return "{}";
		}
	}
	
	private JSONObject getSystemMetaDataAsJson(JSONObject metaDataJsonObj) throws JSONException {

		this.checkInPreFlightTimestamp = new Date();
		
		Cursor cursor = app.getContentResolver().query(
				Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_META),
	    		RfcxConstants.RfcxContentProvider.system.PROJECTION_META,
	            null, null, null);
			if (cursor.moveToFirst()) { do {
				for (int i = 0; i < RfcxConstants.RfcxContentProvider.system.PROJECTION_META.length; i++) {
					metaDataJsonObj.put(	
						RfcxConstants.RfcxContentProvider.system.PROJECTION_META[i],
						(cursor.getString(cursor.getColumnIndex(RfcxConstants.RfcxContentProvider.system.PROJECTION_META[i])) != null) ? cursor.getString(cursor.getColumnIndex(RfcxConstants.RfcxContentProvider.system.PROJECTION_META[i])) : null
					);
				}
			 } while (cursor.moveToNext()); }
			
		return metaDataJsonObj;
	}
	
	public String packagePreFlightCheckInJson(String checkInJsonString) throws JSONException {
			
			JSONObject checkInMetaJson = getSystemMetaDataAsJson(new JSONObject(checkInJsonString));
				
			// Adding timestamp of metadata (JSON) snapshot
			checkInMetaJson.put("measured_at", (new DateTimeUtils()).getDateTime(checkInPreFlightTimestamp));
			
			// Adding GeoCoordinates
			JSONArray latLng = new JSONArray();
			latLng.put(3.6141375); // latitude... fake, obviously
			latLng.put(14.2108033); // longitude... fake, obviously
			checkInMetaJson.put("location", latLng);
			
			// Adding latency data from previous checkins
			checkInMetaJson.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));

			// Recording number of currently queued/skipped checkins
			checkInMetaJson.put("queued_checkins", app.checkInDb.dbQueued.getCount());
			checkInMetaJson.put("skipped_checkins", app.checkInDb.dbSkipped.getCount());
			
			// Adding softare role versions
			List<String> softwareVersions = new ArrayList<String>();
			// TO-DO add all roles...
			softwareVersions.add("api"+"*"+app.version);
			checkInMetaJson.put("software_version", TextUtils.join("|", softwareVersions));
			
			// Adding device location timezone offset
			checkInMetaJson.put("timezone_offset", timeZoneOffsetDateFormat.format(Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault()).getTime()));

			// Adding messages to JSON blob
			checkInMetaJson.put("messages", getSmsMessagesAsJson());

			// Adding screenshot meta to JSON blob
			String[] latestScreenShot = getLatestScreenShotMeta();
			checkInMetaJson.put("screenshots", (latestScreenShot != null) ? TextUtils.join("*",latestScreenShot) : null);
			
			// Stringify JSON, gzip the output and convert to base 64 string for sending
			return (new GZipUtils()).gZipStringToBase64(checkInMetaJson.toString());
	}
	
	
	public void processCheckInResponse(String checkInResponse) {
		if ((checkInResponse != null) && !checkInResponse.isEmpty()) {
			
			try {
				// parse response json
				JSONObject responseJson = new JSONObject(checkInResponse);
				
				// reset/record request latency
				long checkInDuration = System.currentTimeMillis() - this.requestSendStart.getTime();
				this.previousCheckIns = new ArrayList<String>();
				this.previousCheckIns.add(responseJson.getString("checkin_id")+"*"+checkInDuration);
				this.requestSendReturned = new Date();
				Log.i(TAG,"CheckIn request time: "+(checkInDuration/1000)+" seconds");
				
				// clear system metadata included in successful checkin preflight
				int clearPreFlightSystemMetaData = app.getContentResolver().delete(
						Uri.parse(
							RfcxConstants.RfcxContentProvider.system.URI_META+"/"+checkInPreFlightTimestamp.getTime()
						), null, null);

				// parse audio info and use it to purge the data locally
			    JSONArray audioJsonArray = new JSONArray(responseJson.getString("audio"));
			    for (int i = 0; i < audioJsonArray.length(); i++) {
			    	JSONObject audioJson = audioJsonArray.getJSONObject(i);
					app.checkInDb.dbQueued.deleteSingleRowByAudioAttachment(audioJson.getString("id")+".m4a");
					int deleteAudio = app.getContentResolver().delete(Uri.parse(RfcxConstants.RfcxContentProvider.audio.URI_1+"/"+audioJson.getString("id")), null, null);
			    }
				
				// parse the screenshot info and use it to purge the data locally
			    JSONArray screenShotJsonArray = new JSONArray(responseJson.getString("screenshots"));
			    for (int i = 0; i < screenShotJsonArray.length(); i++) {
			    	JSONObject screenShotJson = screenShotJsonArray.getJSONObject(i);
			    	int deleteScreenShot = app.getContentResolver().delete(Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT+"/"+screenShotJson.getString("id")), null, null);
			    }
				
				// parse the message info and use it to purge the data locally
			    JSONArray msgJsonArray = new JSONArray(responseJson.getString("messages"));
			    for (int i = 0; i < msgJsonArray.length(); i++) {
			    	JSONObject msgJson = msgJsonArray.getJSONObject(i);
			    	int deleteMsg = app.getContentResolver().delete(Uri.parse("content://sms/"+msgJson.getString("id")), null, null);
			    	if (deleteMsg == 1) Log.i(TAG, "deleted sms message with id "+msgJson.getString("id"));
			    }
			    
				// parse the instructions section
			    JSONObject instructionsJson = responseJson.getJSONObject("instructions");
			    
			    // handle prefs instuctions
			    JSONObject prefsJson = instructionsJson.getJSONObject("prefs");
			    
			    // handle messages instructions
			    JSONArray msgsJsonArr = instructionsJson.getJSONArray("messages");
			    for (int i = 0; i < msgsJsonArr.length(); i++) {
			    	JSONObject msgJson = msgsJsonArr.getJSONObject(i);
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(msgJson.getString("address"), null, msgJson.getString("body"), null, null);
			    }
				
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			} finally {
				Log.i(TAG, "API Response: " + checkInResponse);
			}
		}
	}			
	
	
	public JSONArray getSmsMessagesAsJson() {
		JSONArray msgJsonArray = new JSONArray();
		Cursor cursor = app.getContentResolver().query(Uri.parse("content://sms/"), null, null, null, null);
		if (cursor.moveToFirst()) {
	    	do {
				try {
					JSONObject msgJson = new JSONObject();
					msgJson.put("android_id", cursor.getString(cursor.getColumnIndex("_id")));
					msgJson.put("received_at", (new DateTimeUtils()).getDateTime(new Date(cursor.getLong(cursor.getColumnIndex("date")))));
					msgJson.put("address", cursor.getString(cursor.getColumnIndex("address")));
					msgJson.put("body", cursor.getString(cursor.getColumnIndex("body")));
					msgJsonArray.put(msgJson);
				} catch (Exception e) {
					Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
				}
			} while (cursor.moveToNext());
		}
	    return msgJsonArray;
	}

	public String[] getLatestScreenShotMeta() {
		
		// grab latest screenshot image meta - we only attach one per check-in (the latest screenshot)
		Cursor cursor = app.getContentResolver().query(
				Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT),
	    		RfcxConstants.RfcxContentProvider.system.PROJECTION_SCREENSHOT,
	            null, null, null);
		if (cursor.moveToFirst()) {
			try {
				return new String[] {
					cursor.getString(cursor.getColumnIndex("created_at")),
					cursor.getString(cursor.getColumnIndex("timestamp")),
					cursor.getString(cursor.getColumnIndex("format")),
					cursor.getString(cursor.getColumnIndex("digest"))
				};
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
		return null;
	}
	
	public List<String[]> loadCheckInFiles(String audioFilePath) {

		List<String[]> checkInFiles = new ArrayList<String[]>();
		
		// attach audio file - we only attach one per check-in
		String audioFileName = audioFilePath.substring(1+audioFilePath.lastIndexOf("/"));
		String audioId = audioFileName.substring(0, audioFileName.lastIndexOf("."));
		String audioFormat = audioFileName.substring(1+audioFileName.lastIndexOf("."));
		try {
			if ((new File(audioFilePath)).exists()) {
				checkInFiles.add(new String[] {"audio", audioFilePath, "audio/"+audioFormat});
				Log.d(TAG, "Audio attached: "+audioId+"."+audioFormat);
			} else {
				Log.e(TAG, "Audio attachment file doesn't exist: "+audioId+"."+audioFormat);
			}
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		
		
		// attach screenshot images - we only attach one per check-in (the latest screenshot)
		Cursor cursor = app.getContentResolver().query(
				Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT),
	    		RfcxConstants.RfcxContentProvider.system.PROJECTION_SCREENSHOT,
	            null, null, null);
		if (cursor.moveToFirst()) {
			try {
				String imgId = cursor.getString(cursor.getColumnIndex("timestamp"));
				String imgFilePath = cursor.getString(cursor.getColumnIndex("filepath"));
				if ((new File(imgFilePath)).exists()) {
					checkInFiles.add(new String[] {"screenshot", imgFilePath, "image/png"});
					Log.d(TAG, "Screenshot attached: "+imgId+".png");
				} else {
					Log.e(TAG, "Screenshot attachment file doesn't exist: "+imgFilePath);
					int deleteScreenShot = app.getContentResolver().delete(Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT+"/"+imgId), null, null);
				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
		
		return checkInFiles;
	}
	
	
	public void connectivityToggleCheck() {
		
		int secsSinceSuccess = (int) ((new Date()).getTime() - this.requestSendReturned.getTime()) / 1000;
		if ((secsSinceSuccess/60) < this.connectivityToggleThresholds[0]) {
			this.connectivityToggleThresholdsReached = new boolean[] { false, false, false, false };	
		} else {
			int thresholdIndex = 0;
			for (int toggleThreshold : this.connectivityToggleThresholds) {
				if (((secsSinceSuccess/60) >= toggleThreshold) && !this.connectivityToggleThresholdsReached[thresholdIndex]) {
					this.connectivityToggleThresholdsReached[thresholdIndex] = true;
					Log.d(TAG,"ToggleCheck: AirplaneMode ("+toggleThreshold+" minutes since last successful CheckIn)");
					app.airplaneMode.setOff(app.getApplicationContext());
					if (toggleThreshold == this.connectivityToggleThresholds[this.connectivityToggleThresholds.length-1]) {
						//last index, force reboot
						Log.d(TAG,"ToggleCheck: ForcedReboot ("+toggleThreshold+" minutes since last successful CheckIn)");
						(new ShellCommands()).executeCommand("reboot", null, false, app.getApplicationContext());
					}
				}
				thresholdIndex++;
			}
		}
	}

}
