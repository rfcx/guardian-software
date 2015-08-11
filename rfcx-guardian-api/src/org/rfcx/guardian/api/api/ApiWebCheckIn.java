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
	
	private DateFormat timeZoneOffsetDateFormat = new SimpleDateFormat("Z");
	
	public long apiCheckInTriggerPeriod = 15000;
	
	public int[] connectivityToggleThresholds = new int[] { 10, 20, 30, 40 };
	public boolean[] connectivityToggleThresholdsReached = new boolean[] { false, false, false, false };
	
	public static final int MAX_CHECKIN_ATTEMPTS = 5;
	
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
	
	public boolean createCheckIn(String[] audioInfo, String filepath) {
		
		app.checkInDb.dbQueued.insert(audioInfo[1]+"."+audioInfo[2], generateCheckInJson(audioInfo), "0", filepath);
		
		return true;
	}
	
	
	public String generateCheckInJson(String[] audioFileInfo) {
		
		try {

			JSONObject json = new JSONObject();
			
			Date clearStatsBefore = new Date();
			Cursor cursor = app.getContentResolver().query(
				Uri.parse(RfcxConstants.RfcxContentProvider.system.URI),
	    		RfcxConstants.RfcxContentProvider.system.PROJECTION,
	            null, null, null);
			if (cursor.moveToFirst()) { do {
				for (int i = 0; i < RfcxConstants.RfcxContentProvider.system.PROJECTION.length; i++) {
					json.put(	RfcxConstants.RfcxContentProvider.system.PROJECTION[i],
								(cursor.getString(cursor.getColumnIndex(RfcxConstants.RfcxContentProvider.system.PROJECTION[i])) != null) ? cursor.getString(cursor.getColumnIndex(RfcxConstants.RfcxContentProvider.system.PROJECTION[i])) : null
							);
				}
			 } while (cursor.moveToNext()); }
			int clearStats = app.getContentResolver().delete(Uri.parse(RfcxConstants.RfcxContentProvider.system.URI+"/"+clearStatsBefore.getTime()), null, null);
			
			
			json.put("measured_at", (new DateTimeUtils()).getDateTime(clearStatsBefore));
			json.put("timezone_offset", timeZoneOffsetDateFormat.format(Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault()).getTime()));
		
			json.put("queued_checkins", app.checkInDb.dbQueued.getCount());
			json.put("skipped_checkins", app.checkInDb.dbSkipped.getCount());
			
			json.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));
			this.previousCheckIns = new ArrayList<String>();
			
			List<String> softwareVersions = new ArrayList<String>();
			softwareVersions.add("api"+"*"+app.version);
			json.put("software_version", TextUtils.join("|", softwareVersions));
	
			List<String> audioFiles = new ArrayList<String>();
			audioFiles.add(TextUtils.join("*", audioFileInfo));
			json.put("audio", TextUtils.join("|", audioFiles));
			
			Log.i(TAG, "CheckIn: "+json.toString());
			
			return json.toString();
			
		} catch (JSONException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			return "{}";
		}
	}
	
	public void processCheckInResponse(String checkInResponse) {
		if ((checkInResponse != null) && !checkInResponse.isEmpty()) {
			
			try {
				// parse response json
				JSONObject responseJson = new JSONObject(checkInResponse);
				
				// reset/record request latency
				long checkInDuration = System.currentTimeMillis() - this.requestSendStart.getTime();
				this.previousCheckIns.add(responseJson.getString("checkin_id")+"*"+checkInDuration);
				this.requestSendReturned = new Date();
				Log.i(TAG,"CheckIn request time: "+(checkInDuration/1000)+" seconds");
				
				// parse audio info and use it to purge the data locally
			    JSONArray audioJsonArray = new JSONArray(responseJson.getString("audio"));
			    for (int i = 0; i < audioJsonArray.length(); i++) {
			    	JSONObject audioJson = audioJsonArray.getJSONObject(i);
					app.checkInDb.dbQueued.deleteSingleRowByAudioAttachment(audioJson.getString("id")+".m4a");
					int deleteAudio = app.getContentResolver().delete(Uri.parse(RfcxConstants.RfcxContentProvider.audio.URI+"/"+audioJson.getString("id")), null, null);
			    }
//				
//				// parse the screenshot info and use it to purge the data locally
//			    JSONArray screenShotJsonArray = new JSONArray(responseJson.getString("screenshots"));
//			    for (int i = 0; i < screenShotJsonArray.length(); i++) {
//			    	JSONObject screenShotJson = screenShotJsonArray.getJSONObject(i);
//					app.deviceScreenShot.purgeSingleScreenShot(screenShotJson.getString("id"));
//					app.screenShotDb.dbScreenShot.deleteSingleScreenShot(screenShotJson.getString("id"));
//			    }
//				
//				// parse the message info and use it to purge the data locally
//			    JSONArray messageJsonArray = new JSONArray(responseJson.getString("messages"));
//			    for (int i = 0; i < messageJsonArray.length(); i++) {
//			    	JSONObject messageJson = messageJsonArray.getJSONObject(i);
//					app.smsDb.dbReceived.deleteSingleMessage(messageJson.getString("digest"));
//			    }
				
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			} finally {
				Log.i(TAG, "API Response: " + checkInResponse);
			}
		}
	}			
	
	
	public String getMessagesAsJson() {
//		List<String[]> msgList = app.smsDb.dbReceived.getAllMessages();
//		List<String> jsonList = new ArrayList<String>();
//		for (String[] msg : msgList) {
//			try {
//				JSONObject msgJson = new JSONObject();
//				msgJson.put("received_at", msg[0]);
//				msgJson.put("number", msg[1]);
//				msgJson.put("body", msg[2]);
//				msgJson.put("digest", msg[3]);
//				jsonList.add(msgJson.toString());
//			} catch (JSONException e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//			}
//		}
//		
//		if (jsonList.size() > 0) {
//			String jsonArray = "["+TextUtils.join(",", jsonList)+"]";
//			Log.v(TAG,"Messages: "+jsonArray);
//			return jsonArray;
//		} else {
			return "[]";
//		}
		
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
//		
//		
//		// attach screenshot images - we only attach one per check-in (the latest screenshot)
//		String[] screenShot = app.screenShotDb.dbScreenShot.getLastScreenShot();
//		if (screenShot.length > 0) {
//			String imgFilePath = app.getApplicationContext().getFilesDir().toString()+"/img/"+screenShot[1]+".png";
//			try {
//				if ((new File(imgFilePath)).exists()) {
//					checkInFiles.add(new String[] {"screenshot", imgFilePath, "image/png"});
//					Log.d(TAG, "Screenshot attached: "+screenShot[1]+".png");
//				} else {
//					Log.e(TAG, "Screenshot attachment file doesn't exist: "+screenShot[1]+".png");
//				}
//			} catch (Exception e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//			}
//		}
		
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
