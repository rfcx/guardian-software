package api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rfcx.guardian.RfcxGuardian;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import database.DeviceStateDb;
import database.SmsDb;
import device.DeviceState;

import utility.DateTimeUtils;

public class ApiCore {

	private static final String TAG = ApiCore.class.getSimpleName();

	private boolean networkConnectivity = false;

	private int connectivityInterval = 300; // change this value in preferences
	private int connectivityTimeout = setConnectivityTimeout();

	DateTimeUtils dateTimeUtils = new DateTimeUtils();
	
	private String deviceId = null;
	private String apiProtocol = "http";
	private String apiDomain;
	private int apiPort;
	
	private String requestEndpoint = "/";
	private URL requestUri;

	private RfcxGuardian app = null;
	private DeviceState deviceState = null;
	private DeviceStateDb deviceStateDb = null;
	private SmsDb smsDb = null;
	
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
		
		if (deviceStateDb == null) deviceStateDb = app.deviceStateDb;
		if (smsDb == null) smsDb = app.smsDb;
		if (deviceState == null) deviceState = app.deviceState;

		String[] vBattery = deviceStateDb.dbBattery.getStatsSummary();
		String[] vBatteryTemp = deviceStateDb.dbBatteryTemperature.getStatsSummary();
		String[] vCpu = deviceStateDb.dbCpu.getStatsSummary();
		String[] vCpuClock = deviceStateDb.dbCpuClock.getStatsSummary();
		String[] vLight = deviceStateDb.dbLight.getStatsSummary();

		JSONObject json = new JSONObject();
		try {
			json.put("batt",(vBattery[0] != "0") ? Integer.parseInt(vBattery[1]) : null);
		} catch (NumberFormatException e) {
			json.put("batt", null);
		}
		try {
			json.put("temp",(vBatteryTemp[0] != "0") ? Integer.parseInt(vBatteryTemp[1]) : null);
		} catch (NumberFormatException e) {
			json.put("temp", null);
		}
		try {
			json.put("srch", new Long(Calendar.getInstance().getTimeInMillis()-signalSearchStart));
		} catch (NumberFormatException e) {
			json.put("srch", null);
		}
		json.put("cpuP", (vCpu[0] != "0") ? vCpu[4] : null);
		json.put("cpuC", (vCpuClock[0] != "0") ? vCpuClock[4] : null);
		json.put("lumn", (vLight[0] != "0") ? vLight[4] : null);
		json.put("powr", (!app.deviceState.isBatteryDisCharging()) ? new Boolean(true) : new Boolean(false));
		json.put("chrg", (app.deviceState.isBatteryCharged()) ? new Boolean(true) : new Boolean(false));

		json.put("sent", transmitTime.getTime().toGMTString());
		json.put("uuid", getDeviceId());
		json.put("appV", app.VERSION);
		json.put("sms", smsDb.dbSms.getSerializedSmsAll());
		
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
			Log.e(TAG, (e != null) ? e.getMessage() : "Exception is Null");
		} finally { if (gZIPOutputStream != null) {
			try { gZIPOutputStream.close();
			} catch (IOException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Exception is Null");
			};
		} }
		return byteArrayOutputStream.toByteArray();
	}
	
	private void cleanupAfterRequest(String httpResponseString) {
		
		resetTransmissionState();
		
		Date lastTransmitTime = transmitTime.getTime();
		if (deviceStateDb != null) {
			deviceStateDb.dbBattery.clearStatsBefore(lastTransmitTime);
			deviceStateDb.dbCpu.clearStatsBefore(lastTransmitTime);
			deviceStateDb.dbCpuClock.clearStatsBefore(lastTransmitTime);
			deviceStateDb.dbLight.clearStatsBefore(lastTransmitTime);
			deviceStateDb.dbBatteryTemperature.clearStatsBefore(lastTransmitTime);
		}
		if (smsDb != null) smsDb.dbSms.clearSmsBefore(lastTransmitTime);
		
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
			Log.e(TAG, (e != null) ? e.getMessage() : "Exception is Null");
		} catch (org.json.simple.parser.ParseException e) {
			Log.e(TAG, (e != null) ? e.getMessage() : "Exception is Null");
		} catch (NullPointerException e) {
			Log.e(TAG, (e != null) ? e.getMessage() : "Exception is Null");
		} finally {
			if (app.verboseLogging) Log.d(TAG, "API Response: " + httpResponseString);
//			app.airplaneMode.setOn(app.getApplicationContext());
		}
	}	

	
	public void resetTransmissionState() {
		isTransmitting = false;
		jsonZipped = null;
	}

	private String getDeviceId() {
		if (deviceId == null)
			deviceId = app.getDeviceId().toString();
		return deviceId;
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
			Log.e(TAG, (e != null) ? e.getMessage() : "Exception is Null");
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
