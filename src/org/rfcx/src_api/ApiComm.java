package org.rfcx.src_api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rfcx.src_android.RfcxSource;
import org.rfcx.src_database.DeviceStateDb;
import org.rfcx.src_device.DeviceState;
import org.rfcx.src_util.DateTimeUtils;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

public class ApiComm {

	private static final String TAG = ApiComm.class.getSimpleName();

	private boolean networkConnectivity = false;

	private int connectivityInterval = 300; // change this value in preferences
	private int connectivityTimeout = setConnectivityTimeout();

	DateTimeUtils dateTimeUtils = new DateTimeUtils();

	private String deviceId = null;
	private String apiProtocol = "http";
	private String apiDomain;
	private int apiPort;
	private String apiEndpoint;

	private int specCount = 0;

	private HttpClient httpClient = new DefaultHttpClient();
	private HttpPost httpPost = null;
	private String httpUri = "";
	private RfcxSource rfcxSource = null;
	private DeviceState deviceState = null;
	private DeviceStateDb deviceStateDb = null;
	private byte[] jsonZipped = null;
	private Calendar transmitTime = Calendar.getInstance();

	private long signalSearchStart = 0;
	private int transmitAttempts = 0;
	public boolean isTransmitting = false;
	
	public void sendData(Context context) {
		if (rfcxSource == null)
			rfcxSource = (RfcxSource) context.getApplicationContext();
		if (httpPost != null && !rfcxSource.airplaneMode.isEnabled(context)) {
			isTransmitting = true;
			transmitAttempts++;
			if (jsonZipped == null) { preparePost(); }
			String httpResponseString = executePost();
			
			if ((httpResponseString == null) && (specCount > 0)) {
				if (transmitAttempts < 3) { sendData(context); }
			} else {
				cleanupAfterResponse(httpResponseString);
				transmitAttempts = 0;
				rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
				if (rfcxSource.verboseLogging) { Log.d(TAG, "Turning off antenna..."); }
			}
			
		}
	}

	private String executePost() {
		String httpResponseString = null;
		if (specCount > 0) {
			MultipartEntity multipartEntity = new MultipartEntity();
			multipartEntity.addPart("blob",  new InputStreamBody(new ByteArrayInputStream(jsonZipped),transmitTime.getTime()+".json"));
			httpPost.setEntity(multipartEntity);
			try {
				httpResponseString = httpResponseString(httpClient.execute(httpPost));
			} catch (ClientProtocolException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
			} catch (IOException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
			}
		} else {
			Log.d(TAG, "No spectra to send.");
		}
		return httpResponseString;
	}
	
	private void preparePost() {

		transmitTime = Calendar.getInstance();
		
		if (deviceStateDb == null) deviceStateDb = rfcxSource.deviceStateDb;
		if (deviceState == null) deviceState = rfcxSource.deviceState;

		String[] vBattery = deviceStateDb.dbBattery.getStatsSummary();
		String[] vBatteryTemp = deviceStateDb.dbBatteryTemperature.getStatsSummary();
		String[] vCpu = deviceStateDb.dbCpu.getStatsSummary();
		String[] vCpuClock = deviceStateDb.dbCpuClock.getStatsSummary();
		String[] vLight = deviceStateDb.dbLight.getStatsSummary();
		boolean vPower = !(rfcxSource.deviceState.isBatteryDisCharging());
		boolean vPowerFull = rfcxSource.deviceState.isBatteryCharged();
		long vSearchTime = Calendar.getInstance().getTimeInMillis()
				- signalSearchStart;

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
			json.put("srch", new Integer(Math.round(vSearchTime / 1000)));
		} catch (NumberFormatException e) {
			json.put("srch", null);
		}
		json.put("cpuP", (vCpu[0] != "0") ? vCpu[4] : null);
		json.put("cpuC", (vCpuClock[0] != "0") ? vCpuClock[4] : null);
		json.put("lumn", (vLight[0] != "0") ? vLight[4] : null);
		json.put("powr", (vPower) ? new Boolean(true) : new Boolean(false));
		json.put("chrg", (vPowerFull) ? new Boolean(true) : new Boolean(false));

		json.put("sent", transmitTime.getTime().toGMTString());
		json.put("udid", getDeviceId());
		json.put("appV", rfcxSource.VERSION);
		
		if (rfcxSource.verboseLogging) Log.d(TAG, httpUri + " - " + json.toJSONString());
		
		specCount = rfcxSource.audioState.fftSendBufferLength();
		Log.d(TAG, "Compiling Spectra ("+specCount+")...");
		
		ArrayList<Calendar> specT_raw = rfcxSource.audioState.getFftSendBufferTimestampsUpTo(specCount);
		String[] specT_str = new String[specCount];
		ArrayList<String[]> specV = rfcxSource.audioState.getFftSendBufferUpTo(specCount);
		
		if (specV.size() > 0) {
			String[] specV_grp = new String[specCount];
			for (int i = 0; i < specCount; i++) {
				specT_str[i] = Long.toHexString(Math.round((transmitTime.getTimeInMillis()-specT_raw.get(i).getTimeInMillis())/1000));
				specV_grp[i] = TextUtils.join(",", specV.get(i));
			}
			json.put("specV", TextUtils.join("*", specV_grp));
			json.put("specT",TextUtils.join(",", specT_str));

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			GZIPOutputStream gZIPOutputStream = null;
			try {
				gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
				gZIPOutputStream.write(json.toJSONString().getBytes("UTF-8"));
			} catch (IOException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
			} finally { if (gZIPOutputStream != null) {
				try { gZIPOutputStream.close();
				} catch (IOException e) { };
			} }
			jsonZipped = byteArrayOutputStream.toByteArray();
			if (rfcxSource.verboseLogging) {
				Log.d(TAG, "Spectra: "+specCount+" - GZipped JSON: "+Math.round(jsonZipped.length/1024)+"kB");
			}
		}
	}

	private void cleanupAfterResponse(String httpResponseString) {
		
		resetTransmissionState();

		if (deviceStateDb != null) {
			deviceStateDb.dbBattery.clearStatsBefore(transmitTime.getTime());
			deviceStateDb.dbCpu.clearStatsBefore(transmitTime.getTime());
			deviceStateDb.dbCpuClock.clearStatsBefore(transmitTime.getTime());
			deviceStateDb.dbLight.clearStatsBefore(transmitTime.getTime());
			deviceStateDb.dbBatteryTemperature.clearStatsBefore(transmitTime.getTime());
		}

		if (specCount > 0) { rfcxSource.audioState.clearFFTSendBufferUpTo(specCount); }
		
		
		try {
			if (httpResponseString != null) {
				JSONObject JSON = (JSONObject) (new JSONParser()).parse(httpResponseString);
				long serverUnixTime = (long) Long.parseLong(JSON.get("time").toString()+"000");
				long deviceUnixTime = Calendar.getInstance().getTimeInMillis();
				if (Math.abs(serverUnixTime - deviceUnixTime) > 3600*1000) {
					Log.d(TAG, "Setting System Clock");
					SystemClock.setCurrentTimeMillis(serverUnixTime);
				}
			}
		} catch (NumberFormatException e) {
			Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
		} catch (org.json.simple.parser.ParseException e) {
			Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
		} catch (NullPointerException e) {
			Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
		} finally {
			if (rfcxSource.verboseLogging) Log.d(TAG, "API Response: " + httpResponseString);
			rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
		}
	}

	private String httpResponseString(HttpResponse httpResponse) {
		if (httpResponse.getEntity() != null) {
			try {
				return EntityUtils.toString(httpResponse.getEntity());
			} catch (ParseException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
			} catch (IOException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
			}
		}
		return null;
	}
	
	public void resetTransmissionState() {
		isTransmitting = false;
		jsonZipped = null;
	}

	private void setPostUri() {
		this.httpUri = apiProtocol + "://" + apiDomain + ":" + apiPort
				+ apiEndpoint;
	}

	private void setPostRequest() {
		setPostUri();
		this.httpPost = new HttpPost(this.httpUri);
	}

	private String getDeviceId() {
		if (deviceId == null)
			deviceId = rfcxSource.getDeviceId().toString();
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
		setPostRequest();
	}

	public void setApiPort(int apiPort) {
		this.apiPort = apiPort;
		setPostRequest();
	}

	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
		setPostRequest();
	}

}
