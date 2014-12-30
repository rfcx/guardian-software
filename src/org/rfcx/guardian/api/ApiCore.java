package org.rfcx.guardian.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rfcx.guardian.RfcxGuardian;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

public class ApiCore {

	private static final String TAG = ApiCore.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private RfcxGuardian app = null;
	
//	public boolean networkConnectivity = false;

//	private int connectivityInterval = 300; // change this value in preferences
//	private int connectivityTimeout = setConnectivityTimeout();
	
//	private String apiProtocol = "https";
//	private int apiPort = 443;
//	private String apiDomain = "api.rfcx.org";
//	private String apiProtocol = "http";
//	private int apiPort = 8080;
//	private String apiDomain = "192.168.0.70";
	
//	private String requestEndpoint = "/";

	private String jsonRaw = null;
	private byte[] jsonZipped = null;

	private Calendar transmitTime = Calendar.getInstance();
	private long signalSearchStartTime = 0;
	private long requestSendStart = 0;
	
	private String lastCheckInId = null;
	private long lastCheckInDuration = 0;
	public boolean isTransmitting = false;
	
	
	public void sendCheckIn(RfcxGuardian rfcxApp) {
		app = rfcxApp;
		packageDiagnostics();
	}

	public void packageDiagnostics() {

		String[] vBattery = app.deviceStateDb.dbBattery.getStatsSummary();
		String[] vBatteryTemp = app.deviceStateDb.dbBatteryTemperature.getStatsSummary();
		String[] vCpu = app.deviceStateDb.dbCpu.getStatsSummary();
		String[] vCpuClock = app.deviceStateDb.dbCpuClock.getStatsSummary();
		String[] vLight = app.deviceStateDb.dbLight.getStatsSummary();

		JSONObject json = new JSONObject();
		
		try { json.put("batt",(vBattery[0] != "0") ? Integer.parseInt(vBattery[1]) : null);
		} catch (NumberFormatException e) { json.put("batt", null); Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC); }
		
		try { json.put("temp",(vBatteryTemp[0] != "0") ? Integer.parseInt(vBatteryTemp[1]) : null);
		} catch (NumberFormatException e) { json.put("temp", null); Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC); }
		
		try { json.put("srch", new Long(Calendar.getInstance().getTimeInMillis()-this.signalSearchStartTime));
		} catch (NumberFormatException e) { json.put("srch", null); Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC); }
		
		json.put("cpuP", (vCpu[0] != "0") ? vCpu[4] : null);
		json.put("cpuC", (vCpuClock[0] != "0") ? vCpuClock[4] : null);
		json.put("lumn", (vLight[0] != "0") ? vLight[4] : null);
		json.put("powr", (!app.deviceState.isBatteryDisCharging()) ? new Boolean(true) : new Boolean(false));
		json.put("chrg", (app.deviceState.isBatteryCharged()) ? new Boolean(true) : new Boolean(false));

		json.put("dttm", Calendar.getInstance().getTime().toGMTString());
		json.put("guid", app.getDeviceId());
		
		json.put("vers", app.version);
		json.put("msgs", app.smsDb.dbSms.getSerializedSmsAll());
		
//		if (this.lastCheckInId != null) {
//			json.put("lastId", this.lastCheckInId);
//			json.put("lastLen", new Long(this.lastCheckInDuration));
//		}
		
		jsonRaw = json.toJSONString();
		if (app.verboseLog) Log.d(TAG, "Diagnostics : " + jsonRaw);
		
		jsonZipped = gZipString(jsonRaw);
		if (app.verboseLog) { Log.d(TAG,"Unzipped JSON: "+Math.round(jsonRaw.toCharArray().length/1024)+"kB"); }
		if (app.verboseLog) { Log.d(TAG,"GZipped JSON: "+Math.round(jsonZipped.length/1024)+"kB"); }
	}
	
	private byte[] gZipString(String s) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gZIPOutputStream = null;
		try {
			gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gZIPOutputStream.write(s.getBytes("UTF-8"));
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} finally { if (gZIPOutputStream != null) {
			try { gZIPOutputStream.close();
			} catch (IOException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			};
		} }
		return byteArrayOutputStream.toByteArray();
	}
	
	private void cleanupAfterRequest(String httpResponseString) {
		
		resetTransmissionState();
		
		Date lastTransmitTime = transmitTime.getTime();

		app.deviceStateDb.dbBattery.clearStatsBefore(lastTransmitTime);
		app.deviceStateDb.dbCpu.clearStatsBefore(lastTransmitTime);
		app.deviceStateDb.dbCpuClock.clearStatsBefore(lastTransmitTime);
		app.deviceStateDb.dbLight.clearStatsBefore(lastTransmitTime);
		app.deviceStateDb.dbBatteryTemperature.clearStatsBefore(lastTransmitTime);

		app.smsDb.dbSms.clearSmsBefore(lastTransmitTime);
		
		try {
			if (httpResponseString != null) {
				JSONObject JSON = (JSONObject) (new JSONParser()).parse(httpResponseString);
				long serverUnixTime = (long) Long.parseLong(JSON.get("time").toString()+"000");
				long deviceUnixTime = Calendar.getInstance().getTimeInMillis();
				if (Math.abs(serverUnixTime - deviceUnixTime) > 3600*1000) {
					Log.d(TAG, "Setting System Clock");
					SystemClock.setCurrentTimeMillis(serverUnixTime);
				}
				this.lastCheckInId = JSON.get("checkInId").toString();
				this.lastCheckInDuration = Calendar.getInstance().getTimeInMillis() - this.requestSendStart;
			}
		} catch (NumberFormatException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} catch (org.json.simple.parser.ParseException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} catch (NullPointerException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} finally {
			if (app.verboseLog) Log.d(TAG, "API Response: " + httpResponseString);
//			app.airplaneMode.setOn(app.getApplicationContext());
		}
	}	

	
	public void resetTransmissionState() {
		isTransmitting = false;
		jsonZipped = null;
	}

	public void resetSignalSearchClock() {
		this.signalSearchStartTime = Calendar.getInstance().getTimeInMillis();
	}
	

	
}
