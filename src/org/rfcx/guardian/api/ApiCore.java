package org.rfcx.guardian.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.FileUtils;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

public class ApiCore {

	private static final String TAG = "RfcxGuardian-"+ApiCore.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private RfcxGuardian app = null;
	
	//public long signalSearchStartTime = 0;
	private DateTimeUtils dateTimeUtils = new DateTimeUtils();
	private Calendar requestSendStart = Calendar.getInstance();
	
	private String lastCheckInId = null;
	private long lastCheckInDuration = 0;
	
	private static final int MAX_FAILED_CHECKINS = 3;
	private int recentFailedCheckins = 0;
	
	public void sendCheckIn(RfcxGuardian rfcxApp) {
		this.app = rfcxApp;
		if (app.isConnected) {
			app.triggerService("ApiCheckIn", true);
		} else {
			Log.d(TAG,"No connectivity... Skipping Check In attempt");
		}
	}
	
	public String getCheckInUrl() {
		return app.getPref("api_domain")+"/v1/guardians/"+app.getDeviceId()+"/checkins";
	}
	
	public String getCheckInJson() {
		
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

		List<String[]> encodedAudio = app.audioDb.dbEncoded.getAllEncoded();
		List<String> audioFiles = new ArrayList<String>();
		for (String[] audioEntry : encodedAudio) {			
			String filePath = app.audioCore.wavDir.substring(0,app.audioCore.wavDir.lastIndexOf("/"))+"/"+audioEntry[2]+"/"+audioEntry[1]+"."+audioEntry[2];
			try {
				if ((new File(filePath)).exists()) {
					audioFiles.add(TextUtils.join("*", audioEntry));
				} else if (app.verboseLog) {
					Log.d(TAG, "Audio didn't exist: "+audioEntry[1]+"."+audioEntry[2]);
				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			}
		}
		json.put("audio", TextUtils.join("|", audioFiles));
		
		this.requestSendStart = Calendar.getInstance();
		
		if (app.verboseLog) { Log.d(TAG, json.toJSONString()); }
		
		return json.toJSONString();
	}
	
	public void processCheckIn(String checkInResponse) {
	
		//	resetTransmissionState();
		Date clearDataBefore = this.requestSendStart.getTime();
	
		app.deviceStateDb.dbBattery.clearStatsBefore(clearDataBefore);
		app.deviceStateDb.dbCpu.clearStatsBefore(clearDataBefore);
		app.deviceStateDb.dbCpuClock.clearStatsBefore(clearDataBefore);
		app.deviceStateDb.dbLight.clearStatsBefore(clearDataBefore);
		app.deviceStateDb.dbBatteryTemperature.clearStatsBefore(clearDataBefore);
		app.deviceStateDb.dbNetworkSearch.clearStatsBefore(clearDataBefore);
		
		app.smsDb.dbSms.clearSmsBefore(clearDataBefore);
		//turning off for deployment on jan 21, 2015. will reactivate for later
		//		app.audioCore.purgeEncodedAssetsUpTo(app.audioDb, clearDataBefore);

		if ((checkInResponse != null) && !checkInResponse.isEmpty()) {
			try {
				JSONObject responseJson = (JSONObject) (new JSONParser()).parse(checkInResponse);
				Log.d(TAG,responseJson.toJSONString());
				this.lastCheckInId = responseJson.get("checkin_id").toString();
				this.lastCheckInDuration = Calendar.getInstance().getTimeInMillis() - this.requestSendStart.getTimeInMillis();
			} catch (org.json.simple.parser.ParseException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				if (app.verboseLog) Log.d(TAG, "API Response: " + checkInResponse);
				app.airplaneMode.setOn(app.getApplicationContext());
			}
		}
	}			
	

	public List<String[]> getCheckInFiles() {

		List<String[]> checkInFiles = new ArrayList<String[]>();
		
		// attach audio files
//		List<String[]> encodedAudio = app.audioDb.dbEncoded.getAllEncoded();
//		for (String[] audioEntry : encodedAudio) {
//			String filePath = app.audioCore.wavDir.substring(0,app.audioCore.wavDir.lastIndexOf("/"))+"/"+audioEntry[2]+"/"+audioEntry[1]+"."+audioEntry[2];
//			try {
//				if ((new File(filePath)).exists()) {
//					checkInFiles.add(new String[] {"audio", filePath, "audio/"+audioEntry[2]});
//					if (app.verboseLog) { Log.d(TAG, "Audio added: "+audioEntry[1]+"."+audioEntry[2]); }
//				} else if (app.verboseLog) {
//					Log.d(TAG, "Audio didn't exist: "+audioEntry[1]+"."+audioEntry[2]);
//				}
//			} catch (Exception e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
//			}
//		}
		
		// attach screenshot images
//		List<String[]> screenShots = app.screenShotDb.dbScreenShot.getScreenShots();
//		for (String[] screenShotEntry : screenShots) {
//			String filePath = app.getApplicationContext().getFilesDir().toString()+"/img/"+screenShotEntry[1]+".png";
//			try {
//				if ((new File(filePath)).exists()) {
//					checkInFiles.add(new String[] {"screenshot", filePath, "image/png"});
//					if (app.verboseLog) { Log.d(TAG, "Screenshot added: "+screenShotEntry[1]+".png"); }
//				} else if (app.verboseLog) {
//					Log.d(TAG, "Screenshot didn't exist: "+screenShotEntry[1]+".png");
//				}
//			} catch (Exception e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
//			}
//		}
		
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
