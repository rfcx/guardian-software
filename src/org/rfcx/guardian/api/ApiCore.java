package org.rfcx.guardian.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rfcx.guardian.RfcxGuardian;

import android.os.SystemClock;
import android.util.Log;

public class ApiCore {

	private static final String TAG = ApiCore.class.getSimpleName();
	private static final String EXCEPTION_FALLBACK = "Exception thrown, but exception itself is null.";

	private RfcxGuardian app = null;
	
	private boolean networkConnectivity = false;

	private int connectivityInterval = 300; // change this value in preferences
	private int connectivityTimeout = setConnectivityTimeout();
	
	private String apiProtocol = "https";
	private int apiPort = 443;
	private String apiDomain = "api.rfcx.org";
	
	private String requestEndpoint = "/";
	private URL requestUri;


	
	private String jsonRaw = null;
	private byte[] jsonZipped = null;

	private Calendar transmitTime = Calendar.getInstance();
	private long signalSearchStart = 0;
	private long requestSendStart = 0;
	
	private String lastCheckInId = null;
	private long lastCheckInDuration = 0;
	public boolean isTransmitting = false;
	
	

	public void prepareDiagnostics() {

		transmitTime = Calendar.getInstance();

		String[] vBattery = app.deviceStateDb.dbBattery.getStatsSummary();
		String[] vBatteryTemp = app.deviceStateDb.dbBatteryTemperature.getStatsSummary();
		String[] vCpu = app.deviceStateDb.dbCpu.getStatsSummary();
		String[] vCpuClock = app.deviceStateDb.dbCpuClock.getStatsSummary();
		String[] vLight = app.deviceStateDb.dbLight.getStatsSummary();

		JSONObject json = new JSONObject();
		try {
			json.put("batt",(vBattery[0] != "0") ? Integer.parseInt(vBattery[1]) : null);
		} catch (NumberFormatException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
			json.put("batt", null);
		}
		try {
			json.put("temp",(vBatteryTemp[0] != "0") ? Integer.parseInt(vBatteryTemp[1]) : null);
		} catch (NumberFormatException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
			json.put("temp", null);
		}
		try {
			json.put("srch", new Long(Calendar.getInstance().getTimeInMillis()-signalSearchStart));
		} catch (NumberFormatException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
			json.put("srch", null);
		}
		json.put("cpuP", (vCpu[0] != "0") ? vCpu[4] : null);
		json.put("cpuC", (vCpuClock[0] != "0") ? vCpuClock[4] : null);
		json.put("lumn", (vLight[0] != "0") ? vLight[4] : null);
		json.put("powr", (!app.deviceState.isBatteryDisCharging()) ? new Boolean(true) : new Boolean(false));
		json.put("chrg", (app.deviceState.isBatteryCharged()) ? new Boolean(true) : new Boolean(false));

		json.put("sent", transmitTime.getTime().toGMTString());
		json.put("guid", app.getDeviceId());
		json.put("appV", app.version);
		json.put("sms", app.smsDb.dbSms.getSerializedSmsAll());
		
		if (this.lastCheckInId != null) {
			json.put("lastId", this.lastCheckInId);
			json.put("lastLen", new Long(this.lastCheckInDuration));
		}
		
		jsonRaw = json.toJSONString();
		if (app.verboseLogging) Log.d(TAG, "Diagnostics : " + jsonRaw);
		
		jsonZipped = gZipString(jsonRaw);
		if (app.verboseLogging) { Log.d(TAG,"GZipped JSON: "+Math.round(jsonZipped.length/1024)+"kB"); }
	}
	
	private byte[] gZipString(String s) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gZIPOutputStream = null;
		try {
			gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gZIPOutputStream.write(s.getBytes("UTF-8"));
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
		} finally { if (gZIPOutputStream != null) {
			try { gZIPOutputStream.close();
			} catch (IOException e) {
				Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
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
			Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
		} catch (org.json.simple.parser.ParseException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
		} catch (NullPointerException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
		} finally {
			if (app.verboseLogging) Log.d(TAG, "API Response: " + httpResponseString);
//			app.airplaneMode.setOn(app.getApplicationContext());
		}
	}	

	
	public void resetTransmissionState() {
		isTransmitting = false;
		jsonZipped = null;
	}

	// Getters & Setters

	public boolean isConnectivityPossible() {
		return networkConnectivity;
	}

	public void setConnectivity(boolean isConnected) {
		networkConnectivity = isConnected;
	}

	public void setSignalSearchStart(Calendar calendar) {
		this.signalSearchStart = calendar.getTimeInMillis();
	}

	public void setConnectivityInterval(int connectivityInterval) {
		this.connectivityInterval = connectivityInterval;
		this.connectivityTimeout = setConnectivityTimeout();
	}

	public int getConnectivityInterval() {
		return this.connectivityInterval;
	}

	public int getConnectivityTimeout() {
		return this.connectivityTimeout;
	}

	private int setConnectivityTimeout() {
		double divisor = (this.connectivityInterval > 120) ? 0.4 : 0.8;
		return (int) Math.round(divisor * this.connectivityInterval);
	}

	public void setApiDomain(String apiDomain) {
		this.apiDomain = apiDomain;
		setRequestUri();
	}

	public void setApiPort(int apiPort) {
		this.apiPort = apiPort;
		setRequestUri();
	}
	
	public void setApiProtocol(String apiProtocolPrefs) {
		String[] apiProtocolParts = apiProtocolPrefs.split(":");
		this.apiProtocol = apiProtocolParts[0].trim().toLowerCase();
		this.apiPort = Integer.parseInt(apiProtocolParts[1].trim().toLowerCase());
		Log.d(TAG, "set "+ this.apiProtocol+" - "+this.apiPort);
	}
	
	public String getApiProtocol() {
		return this.apiProtocol+":"+this.apiPort;
	}

//	public void setApiEndpointCheckIn(String apiEndpointCheckIn) {
//		this.apiEndpointCheckIn = apiEndpointCheckIn;
//		setRequestUri();
//	}

	public void setRequestEndpoint(String requestEndpoint) {
		this.requestEndpoint = requestEndpoint;
		setRequestUri();
	}
	
	private void setRequestUri() {
		try {
			this.requestUri = new URL(this.apiProtocol + "://" + this.apiDomain + ":" + this.apiPort + this.requestEndpoint);
		} catch (MalformedURLException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : EXCEPTION_FALLBACK);
		}
	}

	public String getJsonRaw() {
		return jsonRaw;
	}
	
	public byte[] getJsonZipped() {
		return jsonZipped;
	}
	
	public boolean isTransmitting() {
		return isTransmitting;
	}

	public void setTransmitting(boolean isTransmitting) {
		this.isTransmitting = isTransmitting;
	}
	
	public long getRequestSendStart() {
		return requestSendStart;
	}

	public void setRequestSendStart(long requestSendStart) {
		this.requestSendStart = requestSendStart;
	}

	public long getSignalSearchStart() {
		return signalSearchStart;
	}

	public void setSignalSearchStart(long signalSearchStart) {
		this.signalSearchStart = signalSearchStart;
	}
	
	public void setApp(RfcxGuardian app) {
		this.app = app;
	}

	
}
