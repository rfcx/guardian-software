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
import org.rfcx.guardian.utility.ShellCommands;

import android.text.TextUtils;
import android.util.Log;

public class ApiCore {

	private static final String TAG = "RfcxGuardian-"+ApiCore.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private RfcxGuardian app = null;
	HttpPostMultipart httpPostMultipart = new HttpPostMultipart();
	
	private Date requestSendStart = new Date();
	public Date requestSendReturned = new Date();
	
	private List<String> previousCheckIns = new ArrayList<String>();
	
	public long apiCheckInTriggerPeriod = 15000;
	
//	private int[] connectivityToggleThresholds = new int[] { 15, 30, 45, 60 };
	private int[] connectivityToggleThresholds = new int[] { 10, 20, 30, 40 };
	private boolean[] connectivityToggleThresholdsReached = new boolean[] { false, false, false, false };
	
	public static final int MAX_CHECKIN_ATTEMPTS = 5;
	
	public void init(RfcxGuardian app) {
		this.app = app;
		this.httpPostMultipart.setTimeOuts(90000);
	}
	
	public String getCheckInUrl() {
		return app.getPref("api_domain")+"/v1/guardians/"+app.getDeviceId()+"/checkins";
	}
	
	public void sendCheckIn(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments, boolean allowAttachments, String checkInAudioReference) {
		if (!allowAttachments) keyFilepathMimeAttachments = new ArrayList<String[]>();
		if (app.isConnected) {
			this.requestSendStart = new Date();
			if (app.verboseLog) Log.i(TAG,"CheckIn sent at: "+requestSendStart.toGMTString());
			processCheckInResponse(httpPostMultipart.doMultipartPost(fullUrl, keyValueParameters, keyFilepathMimeAttachments));
			app.checkInDb.dbQueued.incrementSingleRowAttempts(checkInAudioReference);
		} else {
			Log.d(TAG,"No connectivity... Can't send CheckIn");
		}
	}
	
	public void createCheckIn() {
		
		String[] audioInfo = app.audioDb.dbEncoded.getLatestRow();
		app.checkInDb.dbQueued.insert(audioInfo[1]+"."+audioInfo[2], getCheckInJson(audioInfo), "0");
		
		Date statsSnapshot = new Date();
		app.deviceStateDb.dbBattery.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbCpu.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbCpuClock.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbLight.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbBatteryTemperature.clearStatsBefore(statsSnapshot);
		app.deviceStateDb.dbNetworkSearch.clearStatsBefore(statsSnapshot);
		
		Log.d(TAG,"CheckIn created: "+audioInfo[1]);
	}
	
	@SuppressWarnings("unchecked")
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
		json.put("has_power", (!app.deviceState.isBatteryDisCharging()) ? Boolean.valueOf(true) : Boolean.valueOf(false));
		json.put("is_charged", (app.deviceState.isBatteryCharged()) ? Boolean.valueOf(true) : Boolean.valueOf(false));
		json.put("measured_at", (new DateTimeUtils()).getDateTime(Calendar.getInstance().getTime()));
		json.put("software_version", app.version);
		json.put("messages", app.smsDb.dbSms.getSerializedSmsAll());
		
		json.put("previous_checkins", TextUtils.join("|", this.previousCheckIns));
		this.previousCheckIns = new ArrayList<String>();

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
				String audioJsonString = responseJson.get("audio").toString();
				JSONObject audioJson = (JSONObject) (new JSONParser()).parse(audioJsonString.substring(audioJsonString.indexOf("[")+1, audioJsonString.lastIndexOf("]")));
				
				long checkInDuration = System.currentTimeMillis() - this.requestSendStart.getTime();
				
				this.previousCheckIns.add(responseJson.get("checkin_id").toString()+"*"+checkInDuration);
				this.requestSendReturned = new Date();
				if (app.verboseLog) Log.d(TAG,"CheckIn request time: "+(checkInDuration/1000)+" seconds");
				
				app.audioCore.purgeSingleAudioAsset(app.audioDb, audioJson.get("id").toString());
				app.checkInDb.dbQueued.deleteSingleRow(audioJson.get("id").toString()+".m4a");
				
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				if (app.verboseLog) Log.d(TAG, "API Response: " + checkInResponse);
			}
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
				if (app.verboseLog) { Log.d(TAG, "Audio attached: "+audioId+"."+audioFormat); }
			} else if (app.verboseLog) {
				Log.e(TAG, "Audio attachment file doesn't exist: "+audioId+"."+audioFormat);
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
					if (app.verboseLog) { Log.d(TAG, "Screenshot attached: "+screenShotEntry[1]+".png"); }
				} else if (app.verboseLog) {
					Log.d(TAG, "Screenshot attachment file doesn't exist: "+screenShotEntry[1]+".png");
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
