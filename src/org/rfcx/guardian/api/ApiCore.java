package org.rfcx.guardian.api;

import java.io.File;
import java.net.UnknownHostException;
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
import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.HttpPostMultipart;
import org.rfcx.guardian.utility.ShellCommands;

import android.text.TextUtils;
import android.util.Log;

public class ApiCore {

	private static final String TAG = "RfcxGuardian-"+ApiCore.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

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
		// this may not be a good idea... we'll see
		int audioCaptureInterval = 1000*((int) Integer.parseInt(app.getPref("audio_capture_interval")));
		this.httpPostMultipart.setTimeOuts(audioCaptureInterval, audioCaptureInterval);
	}
	
	public String getCheckInUrl() {
		return app.getPref("api_domain")+"/v1/guardians/"+app.getDeviceId()+"/checkins";
	}
	
	public void sendCheckIn(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments, boolean allowAttachments, String checkInAudioReference) {
		if (!allowAttachments) keyFilepathMimeAttachments = new ArrayList<String[]>();
		if (app.isConnected) {
			this.requestSendStart = new Date();
			Log.i(TAG,"CheckIn sent at: "+requestSendStart.toGMTString());
			String checkInResponse = httpPostMultipart.doMultipartPost(fullUrl, keyValueParameters, keyFilepathMimeAttachments);
			processCheckInResponse(checkInResponse);
			if (checkInResponse.equals("RfcxGuardian-HttpPostMultipart-UnknownHostException")) {
				Log.e(TAG,"NOT INCREMENTING CHECK-IN ATTEMPTS");
			} else {
				app.checkInDb.dbQueued.incrementSingleRowAttempts(checkInAudioReference);
			}
		} else {
			Log.d(TAG,"No connectivity... Can't send CheckIn");
		}
	}
	
	public void createCheckIn() {
		
		String[] audioInfo = app.audioDb.dbEncoded.getLatestRow();
		app.checkInDb.dbQueued.insert(audioInfo[1]+"."+audioInfo[2], getCheckInJson(audioInfo), "0");
		
		Date statsSnapshot = new Date();
		app.deviceStateDb.dbBattery.clearRowsBefore(statsSnapshot);
		app.deviceStateDb.dbCPU.clearRowsBefore(statsSnapshot);
		app.deviceStateDb.dbPower.clearRowsBefore(statsSnapshot);
		app.deviceStateDb.dbNetwork.clearRowsBefore(statsSnapshot);
		app.deviceStateDb.dbOffline.clearRowsBefore(statsSnapshot);
		app.deviceStateDb.dbLightMeter.clearRowsBefore(statsSnapshot);
		app.dataTransferDb.dbTransferred.clearRowsBefore(statsSnapshot);
	}
	
	
	public String getCheckInJson(String[] audioFileInfo) {
		
		String[] vBattery = app.deviceStateDb.dbBattery.getConcatRows();
		String[] vCpu = app.deviceStateDb.dbCPU.getConcatRows();
		String[] vPower = app.deviceStateDb.dbPower.getConcatRows();
		String[] vNetwork = app.deviceStateDb.dbNetwork.getConcatRows();
		String[] vOffline = app.deviceStateDb.dbOffline.getConcatRows();
		String[] vLightMeter = app.deviceStateDb.dbLightMeter.getConcatRows();
		String[] vDataTransferred = app.dataTransferDb.dbTransferred.getConcatRows();
		
		String timeZoneOffset = timeZoneOffsetDateFormat.format(Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault()).getTime());
		
		try {

			JSONObject json = new JSONObject();
		
			json.put("battery", (vBattery[0] != "0") ? vBattery[1] : null);
			json.put("cpu", (vCpu[0] != "0") ? vCpu[1] : null);
			json.put("power", (vPower[0] != "0") ? vPower[1] : null);
			json.put("network", (vNetwork[0] != "0") ? vNetwork[1] : null);
			json.put("offline", (vOffline[0] != "0") ? vOffline[1] : null);
			json.put("lightmeter", (vLightMeter[0] != "0") ? vLightMeter[1] : null);
			
			json.put("measured_at", (new DateTimeUtils()).getDateTime(Calendar.getInstance().getTime()));
			json.put("software_version", app.version);
			json.put("timezone_offset", timeZoneOffset);
			
			json.put("data_transfer",  (vDataTransferred[0] != "0") ? vDataTransferred[1] : null);
			
			json.put("queued_checkins", app.checkInDb.dbQueued.getCount());
			json.put("skipped_checkins", app.checkInDb.dbSkipped.getCount());
			
			json.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));
			this.previousCheckIns = new ArrayList<String>();
	
			List<String> audioFiles = new ArrayList<String>();
			audioFiles.add(TextUtils.join("*", audioFileInfo));
			json.put("audio", TextUtils.join("|", audioFiles));
			
			Log.i(TAG, "CheckIn: "+json.toString());
			
			return json.toString();
			
		} catch (JSONException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
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
					app.audioCore.purgeSingleAudioAsset(app.audioDb, audioJson.getString("id"));
					app.checkInDb.dbQueued.deleteSingleRowByAudioAttachment(audioJson.getString("id")+".m4a");
			    }
				
				// parse the screenshot info and use it to purge the data locally
			    JSONArray screenShotJsonArray = new JSONArray(responseJson.getString("screenshots"));
			    for (int i = 0; i < screenShotJsonArray.length(); i++) {
			    	JSONObject screenShotJson = screenShotJsonArray.getJSONObject(i);
					app.deviceScreenShot.purgeSingleScreenShot(screenShotJson.getString("id"));
					app.screenShotDb.dbScreenShot.deleteSingleScreenShot(screenShotJson.getString("id"));
			    }
				
				// parse the message info and use it to purge the data locally
			    JSONArray messageJsonArray = new JSONArray(responseJson.getString("messages"));
			    for (int i = 0; i < messageJsonArray.length(); i++) {
			    	JSONObject messageJson = messageJsonArray.getJSONObject(i);
					app.smsDb.dbReceived.deleteSingleMessage(messageJson.getString("digest"));
			    }
				
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				Log.i(TAG, "API Response: " + checkInResponse);
			}
		}
	}			
	
	
	public String getMessagesAsJson() {
		List<String[]> msgList = app.smsDb.dbReceived.getAllMessages();
		List<String> jsonList = new ArrayList<String>();
		for (String[] msg : msgList) {
			try {
				JSONObject msgJson = new JSONObject();
				msgJson.put("received_at", msg[0]);
				msgJson.put("number", msg[1]);
				msgJson.put("body", msg[2]);
				msgJson.put("digest", msg[3]);
				jsonList.add(msgJson.toString());
			} catch (JSONException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			}
		}
		
		if (jsonList.size() > 0) {
			String jsonArray = "["+TextUtils.join(",", jsonList)+"]";
			Log.v(TAG,"Messages: "+jsonArray);
			return jsonArray;
		} else {
			return "[]";
		}
		
	}

	public List<String[]> loadCheckInFiles(String audioFile) {

		List<String[]> checkInFiles = new ArrayList<String[]>();
		
		// attach audio file - we only attach one per check-in
		String audioId = audioFile.substring(0, audioFile.lastIndexOf("."));
		String audioFormat = audioFile.substring(1+audioFile.lastIndexOf("."));
		String audioFilePath = app.audioCore.wavDir.substring(0,app.audioCore.wavDir.lastIndexOf("/"))+"/"+audioFormat+"/"+audioId+"."+audioFormat;
		try {
			if ((new File(audioFilePath)).exists()) {
				checkInFiles.add(new String[] {"audio", audioFilePath, "audio/"+audioFormat});
				Log.d(TAG, "Audio attached: "+audioId+"."+audioFormat);
			} else {
				Log.e(TAG, "Audio attachment file doesn't exist: "+audioId+"."+audioFormat);
			}
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		
		
		// attach screenshot images - we only attach one per check-in (the latest screenshot)
		String[] screenShot = app.screenShotDb.dbScreenShot.getLastScreenShot();
		if (screenShot.length > 0) {
			String imgFilePath = app.getApplicationContext().getFilesDir().toString()+"/img/"+screenShot[1]+".png";
			try {
				if ((new File(imgFilePath)).exists()) {
					checkInFiles.add(new String[] {"screenshot", imgFilePath, "image/png"});
					Log.d(TAG, "Screenshot attached: "+screenShot[1]+".png");
				} else {
					Log.e(TAG, "Screenshot attachment file doesn't exist: "+screenShot[1]+".png");
				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
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
						(new ShellCommands()).executeCommandAsRoot("reboot",null,app.getApplicationContext());
					}
				}
				thresholdIndex++;
			}
		}
	}
	
//	private byte[] gZipString(String s) {
//		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//		GZIPOutputStream gZIPOutputStream = null;
//		try {
//			gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
//			gZIPOutputStream.write(s.getBytes("UTF-8"));
//		} catch (IOException e) {
//			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
//		} finally { if (gZIPOutputStream != null) {
//			try { gZIPOutputStream.close();
//			} catch (IOException e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
//			};
//		} }
//		return byteArrayOutputStream.toByteArray();
//	}
//	

	
}
