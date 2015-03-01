package org.rfcx.guardian.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.HttpPostMultipart;

import android.text.TextUtils;
import android.util.Log;

public class ApiCore {

	private static final String TAG = "RfcxGuardian-"+ApiCore.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private RfcxGuardian app = null;
	HttpPostMultipart httpPostMultipart = new HttpPostMultipart();
	
	//public long signalSearchStartTime = 0;
	private DateTimeUtils dateTimeUtils = new DateTimeUtils();
	private Date requestSendStart = new Date();
	
	private String lastCheckInId = null;
	private long lastCheckInDuration = 0;
	
	private long lastCheckInTriggered = 0;
	
	private int checkInCountTotal = 0;
	private int checkInCountSuccess = 0;
	private int checkInCountConsecutiveFailures = 0;
	
	private static final int MAX_FAILED_CHECKINS = 3;
	
	public void init(RfcxGuardian app) {
		this.app = app;
	}
	
	public void triggerCheckIn(boolean forceStart) {
		if (app.isConnected) {
	//		if ((System.currentTimeMillis() - lastCheckInTriggered) > 1000) {
				lastCheckInTriggered = System.currentTimeMillis();
				app.triggerService("ApiCheckIn", forceStart);
		//	} else {
		//		Log.d(TAG,"Skipping attempt to double check-in");
		//	}
		} else {
			Log.d(TAG,"No connectivity... Toggling AirplaneMode");
			app.airplaneMode.setOff(app.getApplicationContext());
		}
	}
	
	public String getCheckInUrl() {
		return app.getPref("api_domain")+"/v1/guardians/"+app.getDeviceId()+"/checkins";
	}
	
	public void sendCheckIn(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments, boolean allowAttachments) {
		//Log.d(TAG, keyValueParameters.toString());
		if (!allowAttachments) keyFilepathMimeAttachments = new ArrayList<String[]>();
		if (app.isConnected) {
			this.requestSendStart = new Date();
			if (app.verboseLog) Log.i(TAG,"CheckIn sent at: "+requestSendStart.toGMTString());
			processCheckInResponse(httpPostMultipart.doMultipartPost(fullUrl, keyValueParameters, keyFilepathMimeAttachments));
		} else {
			Log.d(TAG,"No connectivity... Can't send CheckIn");
		}
	}
	
	public void createCheckIn() {
		
		String[] audioInfo = app.audioDb.dbEncoded.getLatestRow();
		app.checkInDb.dbQueued.insert(audioInfo[1]+"."+audioInfo[2], getCheckInJson(audioInfo));
		
		Date statsSnapshot = new Date();
		app.deviceStateDb.dbBattery.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbCpu.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbCpuClock.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbLight.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbBatteryTemperature.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbNetworkSearch.clearStatsBefore(statsSnapshot);
		
		Log.d(TAG,"CheckIn created: "+audioInfo[1]);
	}
	
	public String getCheckInJson(String[] audioFileInfo) {
		
		String[] vBattery = app.deviceStateDb.dbBattery.getStatsSummary();
		String[] vBatteryTemp = app.deviceStateDb.dbBatteryTemperature.getStatsSummary();
		String[] vCpu = app.deviceStateDb.dbCpu.getStatsSummary();
		String[] vCpuClock = app.deviceStateDb.dbCpuClock.getStatsSummary();
		String[] vLight = app.deviceStateDb.dbLight.getStatsSummary();
		String[] vNetworkSearch = app.deviceStateDb.dbNetworkSearch.getStatsSummary();

		JSONObject json = new JSONObject();
		
		json.put("battery_percent", (vBattery[0] != "0") ? vBattery[4] : null);
		json.put("battery_temperature", (vBatteryTemp[0] != "0") ? vBatteryTemp[4] : null);
		json.put("cpu_percent", (vCpu[0] != "0") ? vCpu[4] : null);
		json.put("cpu_clock", (vCpuClock[0] != "0") ? vCpuClock[4] : null);
		json.put("internal_luminosity", (vLight[0] != "0") ? vLight[4] : null);
		json.put("network_search_time", (vNetworkSearch[0] != "0") ? vNetworkSearch[4] : null);
		json.put("has_power", (!app.deviceState.isBatteryDisCharging()) ? new Boolean(true) : new Boolean(false));
		json.put("is_charged", (app.deviceState.isBatteryCharged()) ? new Boolean(true) : new Boolean(false));
		json.put("measured_at", dateTimeUtils.getDateTime(Calendar.getInstance().getTime()));
		json.put("software_version", app.version);
		json.put("messages", app.smsDb.dbSms.getSerializedSmsAll());
		
		json.put("last_checkin_id", (this.lastCheckInId != null) ? this.lastCheckInId : null);
		json.put("last_checkin_duration", (this.lastCheckInId != null) ? new Long(this.lastCheckInDuration) : null);

		List<String> audioFiles = new ArrayList<String>();
		audioFiles.add(TextUtils.join("*", audioFileInfo));
		json.put("audio", TextUtils.join("|", audioFiles));
		
		if (app.verboseLog) { Log.d(TAG, "JSON: "+json.toJSONString()); }
		
		return json.toJSONString();
	}
	
	public void processCheckInResponse(String checkInResponse) {
		
		if ((checkInResponse != null) && !checkInResponse.isEmpty()) {
			app.smsDb.dbSms.clearSmsBefore(this.requestSendStart);
			app.deviceScreenShot.purgeAllScreenShots(app.screenShotDb);
			
			try {
				JSONObject responseJson = (JSONObject) (new JSONParser()).parse(checkInResponse);
				this.lastCheckInId = responseJson.get("checkin_id").toString();
				this.lastCheckInDuration = System.currentTimeMillis() - this.requestSendStart.getTime();
				
				if (app.verboseLog) Log.d(TAG,"CheckIn request time: "+(this.lastCheckInDuration/1000)+" seconds");
				String audioJsonString = responseJson.get("audio").toString();
				JSONObject audioJson = (JSONObject) (new JSONParser()).parse(audioJsonString.substring(audioJsonString.indexOf("[")+1, audioJsonString.lastIndexOf("]")));
				app.audioCore.purgeSingleAudioAsset(app.audioDb, audioJson.get("id").toString());
				app.checkInDb.dbQueued.deleteSingleRow(audioJson.get("id").toString()+".m4a");
				
			} catch (org.json.simple.parser.ParseException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				if (app.verboseLog) Log.d(TAG, "API Response: " + checkInResponse);
			}
		} else {
			Log.e(TAG,"No CheckIn Response (or null). Trying again.");
		}
	}			
	

	public List<String[]> loadCheckInFiles(String audioFile) {

		List<String[]> checkInFiles = new ArrayList<String[]>();
		
		// attach audio file - we only attach one at a time
		String audioId = audioFile.substring(0, audioFile.lastIndexOf("."));
		String audioFormat = audioFile.substring(1+audioFile.lastIndexOf("."));
		String audioFilePath = app.audioCore.wavDir.substring(0,app.audioCore.wavDir.lastIndexOf("/"))+"/"+audioFormat+"/"+audioId+"."+audioFormat;
		try {
			if ((new File(audioFilePath)).exists()) {
				checkInFiles.add(new String[] {"audio", audioFilePath, "audio/"+audioFormat});
				if (app.verboseLog) { Log.d(TAG, "Audio added: "+audioId+"."+audioFormat); }
			} else if (app.verboseLog) {
				Log.d(TAG, "Audio didn't exist: "+audioId+"."+audioFormat);
			}
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		
		
		// attach screenshot images
		List<String[]> screenShots = app.screenShotDb.dbScreenShot.getScreenShots();
		for (String[] screenShotEntry : screenShots) {
			String imgFilePath = app.getApplicationContext().getFilesDir().toString()+"/img/"+screenShotEntry[1]+".png";
			try {
				if ((new File(imgFilePath)).exists()) {
					checkInFiles.add(new String[] {"screenshot", imgFilePath, "image/png"});
					if (app.verboseLog) { Log.d(TAG, "Screenshot added: "+screenShotEntry[1]+".png"); }
				} else if (app.verboseLog) {
					Log.d(TAG, "Screenshot didn't exist: "+screenShotEntry[1]+".png");
				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			}
		}
		
		return checkInFiles;
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
