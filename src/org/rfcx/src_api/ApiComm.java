package org.rfcx.src_api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
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
	private int specLength = 0;

	private HttpClient httpClient = new DefaultHttpClient();
	private HttpPost httpPost = null;
	private String httpUri = "";
	private RfcxSource rfcxSource = null;
	private DeviceState deviceState = null;
	private DeviceStateDb deviceStateDb = null;

	private long signalSearchStart = 0;

	private int transmitAttempts = 0;

	public boolean isTransmitting = false;

	
	public void sendData(Context context) {
		if (rfcxSource == null)
			rfcxSource = (RfcxSource) context.getApplicationContext();
		if (httpPost != null && !rfcxSource.airplaneMode.isEnabled(context)) {
			isTransmitting = true;
			String strResponse = null;
			Date sendDateTime = new Date();
			try {
				transmitAttempts++;
				preparePostData();
				HttpResponse httpResponse = httpClient.execute(httpPost);
				strResponse = httpResponseString(httpResponse);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
			} catch (ClientProtocolException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
			} catch (IOException e) {
				Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
			} finally {
				if (strResponse == null) {
					if (transmitAttempts < 3) {
						sendData(context);
					}
					if (RfcxSource.VERBOSE) {
						Log.d(TAG, "Retransmitting... (attempt #"
								+ transmitAttempts + ")");
					}
				} else {
					cleanupAfterResponse(strResponse, sendDateTime);
					transmitAttempts = 0;
					rfcxSource.airplaneMode.setOn(rfcxSource
							.getApplicationContext());
					if (RfcxSource.VERBOSE) {
						Log.d(TAG, "Turning off antenna...");
					}
				}
			}
		} else {
		}
	}

	private void preparePostData() {

		if (deviceStateDb == null)
			deviceStateDb = rfcxSource.deviceStateDb;
		if (deviceState == null)
			deviceState = rfcxSource.deviceState;

		String[] vBattery = deviceStateDb.dbBattery.getStatsSummary();
		String[] vBatteryTemp = deviceStateDb.dbBatteryTemperature
				.getStatsSummary();
		String[] vCpu = deviceStateDb.dbCpu.getStatsSummary();
		String[] vCpuClock = deviceStateDb.dbCpuClock.getStatsSummary();
		String[] vLight = deviceStateDb.dbLight.getStatsSummary();
		boolean vPower = !(rfcxSource.deviceState.isBatteryDisCharging());
		boolean vPowerFull = rfcxSource.deviceState.isBatteryCharged();
		long vSearchTime = Calendar.getInstance().getTimeInMillis()
				- signalSearchStart;

		JSONObject json = new JSONObject();
		try {
			json.put("batt",
					(vBattery[0] != "0") ? Integer.parseInt(vBattery[1]) : null);
		} catch (NumberFormatException e) {
			json.put("batt", null);
		}
		try {
			json.put(
					"temp",
					(vBatteryTemp[0] != "0") ? Integer
							.parseInt(vBatteryTemp[1]) : null);
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

		Calendar calendar = Calendar.getInstance();
		json.put("sent", calendar.getTime().toGMTString());
		
		if (RfcxSource.VERBOSE) {
			Log.d(TAG, httpUri + " - " + json.toJSONString());
		}
		
		specCount = rfcxSource.audioState.fftSendBufferLength();
		Log.d(TAG, "Compiling Spectra ("+specCount+")...");
		
		ArrayList<Calendar> specT_raw = rfcxSource.audioState.getFftSendBufferTimestampsUpTo(specCount);
		String[] specT_str = new String[specCount];
		ArrayList<String[]> specV = rfcxSource.audioState.getFftSendBufferUpTo(specCount);
		
		if (specV.size() > 0) {
			specLength = specV.get(0).length;
			String[] specV_grp = new String[specCount];
			for (int i = 0; i < specCount; i++) {
				specT_str[i] = Long.toHexString(Math.round((calendar.getTimeInMillis()-specT_raw.get(i).getTimeInMillis())/1000));
				specV_grp[i] = TextUtils.join(",", specV.get(i));
			}
			json.put("specV", TextUtils.join("*", specV_grp));
			json.put("specT",TextUtils.join(",", specT_str));
		} else {
			Log.e(TAG, "Spectra buffer at zero length");
			json.put("specV", "");
			json.put("specT", "");
		}

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gZIPOutputStream = null;
		try {
			gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gZIPOutputStream.write(json.toJSONString().getBytes("UTF-8"));
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} finally { if (gZIPOutputStream != null) {
			try { gZIPOutputStream.close();
			} catch (IOException e) { };
		} }
		byte[] jsonZipped = byteArrayOutputStream.toByteArray();
		
		try {
			MultipartEntity multipartEntity = new MultipartEntity();
			multipartEntity.addPart("udid", new StringBody(getDeviceId()));
			multipartEntity.addPart("blob",  new InputStreamBody(new ByteArrayInputStream(jsonZipped),calendar.getTime().toGMTString()+".json.gz"));
			httpPost.setEntity(multipartEntity);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getMessage());
		}
		
		if (RfcxSource.VERBOSE) {
			Log.d(TAG,
					"Spectra: " + specCount
					+ " - GZipped JSON: " + Math.round(jsonZipped.length / 1024) + "kB"
					);
		}
	}

	private void cleanupAfterResponse(String strResponse, Date sendDateTime) {

		isTransmitting = false;

		if (deviceStateDb != null) {
			deviceStateDb.dbBattery.clearStatsBefore(sendDateTime);
			deviceStateDb.dbCpu.clearStatsBefore(sendDateTime);
			deviceStateDb.dbCpuClock.clearStatsBefore(sendDateTime);
			deviceStateDb.dbLight.clearStatsBefore(sendDateTime);
			deviceStateDb.dbBatteryTemperature.clearStatsBefore(sendDateTime);
		}

		if (specCount > 0) {
			rfcxSource.audioState.clearFFTSendBufferUpTo(specCount);
		}

		try {
			// long timeMillis = (long) Long.parseLong(strResponse);
			Log.d(TAG, "Response: " + strResponse);
		} catch (NumberFormatException e) {
			Log.e(TAG, (e != null) ? e.getMessage() : "Null Exception");
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
