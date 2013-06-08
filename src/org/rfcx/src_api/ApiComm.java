package org.rfcx.src_api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.rfcx.src_android.RfcxSource;
import org.rfcx.src_database.DeviceStateDb;
import org.rfcx.src_device.DeviceState;
import org.rfcx.src_util.DateTimeUtils;
import org.rfcx.src_util.DeflateUtils;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

public class ApiComm {

	private static final String TAG = ApiComm.class.getSimpleName();

	private boolean networkConnectivity = false;
	
	private int connectivityInterval = 300; //change this value in preferences
	private int connectivityTimeout = setConnectivityTimeout();
	
	DateTimeUtils dateTimeUtils = new DateTimeUtils();
	
	private String deviceId = null;
	private String apiProtocol = "http";
	private String apiDomain;
	private int apiPort;
	private String apiEndpoint;
	
	private HttpClient httpClient = new DefaultHttpClient();
	private HttpPost httpPost = null;
	private String httpUri = "";
	private RfcxSource rfcxSource = null;
	private DeviceState deviceState = null;
	private DeviceStateDb deviceStateDb = null;
	
	private long signalSearchStart = 0;
	
	private int transmitAttempts = 0;
	
	public void sendData(Context context) {
		if (rfcxSource == null) rfcxSource = (RfcxSource) context.getApplicationContext();
		if (httpPost != null) {
			String strResponse = null;
			Date sendDateTime = new Date();
			try {
				transmitAttempts++;
				httpPost.setEntity(new UrlEncodedFormEntity(preparePostData()));
				HttpResponse httpResponse = httpClient.execute(httpPost);
	        	strResponse = httpResponseString(httpResponse);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, (e!=null) ? e.getMessage() : "Null Exception");
			} catch (ClientProtocolException e) {
				Log.e(TAG, (e!=null) ? e.getMessage() : "Null Exception");
			} catch (IOException e) {
				Log.e(TAG, (e!=null) ? e.getMessage() : "Null Exception");
			} finally {
				if (!rfcxSource.airplaneMode.isEnabled(context)) {
					if ((strResponse == null) && (transmitAttempts <= 3)) {
						sendData(context);
						if (RfcxSource.VERBOSE) { Log.d(TAG, "Retransmitting... (attempt #"+transmitAttempts+")"); }
					} else {
						if (strResponse != null) {
							cleanupAfterResponse(strResponse, sendDateTime);
						}
						transmitAttempts = 0;
						rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
						if (RfcxSource.VERBOSE) { Log.d(TAG, "Turning off antenna..."); }
					}
				}
			}
		} else {
		}
	}
	
	private List<NameValuePair> preparePostData() {
		
		if (deviceStateDb == null) deviceStateDb = rfcxSource.deviceStateDb;
		if (deviceState == null) deviceState = rfcxSource.deviceState;
		
		String[] vBattery = deviceStateDb.dbBattery.getStatsSummary();
		String[] vBatteryTemp = deviceStateDb.dbBatteryTemperature.getStatsSummary();
		String[] vCpu = deviceStateDb.dbCpu.getStatsSummary();
		String[] vCpuClock = deviceStateDb.dbCpuClock.getStatsSummary();
		String[] vLight = deviceStateDb.dbLight.getStatsSummary();
		boolean vPower = !(rfcxSource.deviceState.isBatteryDisCharging());
		boolean vPowerFull = rfcxSource.deviceState.isBatteryCharged();
		long vSearchTime = Calendar.getInstance().getTimeInMillis() - signalSearchStart;
				
		JSONObject json = new JSONObject();
		try { json.put("batt", (vBattery[0]!="0") ? Integer.parseInt(vBattery[1]) : null ); } catch (NumberFormatException e) { json.put("batt",null); }
		try { json.put("temp", (vBatteryTemp[0]!="0") ? Integer.parseInt(vBatteryTemp[1]) : null ); } catch (NumberFormatException e) { json.put("temp",null); }
		try { json.put("srch", new Integer(Math.round(vSearchTime/1000)) ); } catch (NumberFormatException e) { json.put("srch",null); }
		json.put("cpuP", (vCpu[0]!="0") ? vCpu[4] : null );
		json.put("cpuC", (vCpuClock[0]!="0") ? vCpuClock[4] : null );
		json.put("lumn", (vLight[0]!="0") ? vLight[4] : null );
		json.put("powr", (vPower) ? new Boolean(true) : new Boolean(false) );
		json.put("chrg", (vPowerFull) ? new Boolean(true) : new Boolean(false) );

		Log.d(TAG, httpUri+" - "+ json.toJSONString());
		
		long[] specSend = rfcxSource.audioState.getFftSpecSend();
		String[] specComp = new String[specSend.length];
		for (int i = 0; i < specSend.length; i++) {
			specComp[i] = formatAmplitude(specSend[i]);
		}
		json.put("spec", TextUtils.join(",", specComp));

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
		nameValuePairs.add(new BasicNameValuePair("udid", getDeviceId()));
		nameValuePairs.add(new BasicNameValuePair("blob",DeflateUtils.deflate(json.toJSONString())));
		nameValuePairs.add(new BasicNameValuePair("json",json.toJSONString()));
		
		
        return nameValuePairs;
	}
	
	private void cleanupAfterResponse(String strResponse, Date sendDateTime) {
		
		if (deviceStateDb != null) {
			deviceStateDb.dbBattery.clearStatsBefore(sendDateTime);
			deviceStateDb.dbCpu.clearStatsBefore(sendDateTime);
			deviceStateDb.dbCpuClock.clearStatsBefore(sendDateTime);
			deviceStateDb.dbLight.clearStatsBefore(sendDateTime);
			deviceStateDb.dbBatteryTemperature.clearStatsBefore(sendDateTime);
		}
		
		try {
//			long timeMillis = (long) Long.parseLong(strResponse);
			Log.d(TAG, "Response: "+strResponse);
		} catch (NumberFormatException e) {
			Log.e(TAG, (e!=null) ? e.getMessage() : "Null Exception");
		}
	}
	
	private String httpResponseString(HttpResponse httpResponse) {
		if (httpResponse.getEntity() != null) {
			try {
				return EntityUtils.toString(httpResponse.getEntity());
			} catch (ParseException e) {
				Log.e(TAG, (e!=null) ? e.getMessage() : "Null Exception");
			} catch (IOException e) {
				Log.e(TAG, (e!=null) ? e.getMessage() : "Null Exception");
			}
		}
		return null;
	}
	
	private void setPostUri() {
		this.httpUri = apiProtocol+"://"+apiDomain+":"+apiPort+apiEndpoint;
	}
	
	private void setPostRequest() {
		setPostUri();
		this.httpPost = new HttpPost(this.httpUri);	
	}
	
	private String getDeviceId() {
		if (deviceId == null) deviceId = rfcxSource.getDeviceId().toString();
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
	
	private String formatAmplitude(double amplitude) {
		return Integer.toHexString( (int) Math.round( amplitude * 512 ) );
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
		return (int) Math.round(divisor*this.connectivityInterval);
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
