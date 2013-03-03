package org.rfcx.src_api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

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
import org.rfcx.rfcx_src_android.RfcxSource;
import org.rfcx.src_arduino.ArduinoState;
import org.rfcx.src_util.DateTimeUtils;

public class ApiComm {

	private static final String TAG = ApiComm.class.getSimpleName();

	private static final boolean API_TRANSMIT_ENABLED = true;
	private boolean networkConnectivity = false;
	
	private int connectivityInterval = 60000;
	
	DateTimeUtils dateTimeUtils = new DateTimeUtils();
	
	private Date currTransmitTime = new Date();
	private Date lastTransmitTime = currTransmitTime;
	private String deviceId = null;
	private String protocol = "http";
	private String domain = null;
	private int port = 8080;
	private String endpoint = "/freq";
	
	private HttpClient httpClient = new DefaultHttpClient();
	private HttpPost httpPost = null;
	
	public void sendData(Context context) {
		if (httpPost != null) {
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(preparePostData(context)));
				HttpResponse httpResponse = httpClient.execute(httpPost);
	        	String strResponse = httpResponseString(httpResponse);
	        	if (strResponse != null) {
	        		cleanupArduinoDb(context);
	        		if (RfcxSource.verboseLog()) { Log.d(TAG, strResponse); }
	        	} else {
	        		if (RfcxSource.verboseLog()) { Log.d(TAG, "null response from API"); }
	        	}
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, e.getMessage());
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}

		} else {
			Log.e(TAG, "httpPost is not set");
		}
	}
	
	private List<NameValuePair> preparePostData(Context context) {
		RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("id", getDeviceId(rfcxSource)));
		
		if (ArduinoState.isArduinoEnabled()) {
			int[] statsTemp = rfcxSource.arduinoDb.dbTemperature.getStatsSince(lastTransmitTime);
			int[] statsHumi = rfcxSource.arduinoDb.dbHumidity.getStatsSince(lastTransmitTime);
			String[] currCharge = rfcxSource.arduinoDb.dbCharge.getLast();
			if (statsTemp[0] > 0) { nameValuePairs.add(new BasicNameValuePair("atmp", Integer.toString(statsTemp[1]))); }
			if (statsHumi[0] > 0) { nameValuePairs.add(new BasicNameValuePair("ahmd", Integer.toString(statsHumi[1]))); }
			if (currCharge[1] != "0") { nameValuePairs.add(new BasicNameValuePair("achg", currCharge[1])); }
		}

		String[] spectrum = rfcxSource.audioDb.dbSpectrum.getLast();
		StringBuilder spectrumSend = (new StringBuilder()).append(spectrum[0]).append(";").append(spectrum[1]);
		nameValuePairs.add(new BasicNameValuePair("spec", spectrumSend.toString()));
        
        return nameValuePairs;
	}
	
	private String httpResponseString(HttpResponse httpResponse) {
		if (httpResponse.getEntity() != null) {
			lastTransmitTime = currTransmitTime;
			currTransmitTime = new Date();
			try {
				return EntityUtils.toString(httpResponse.getEntity());
			} catch (ParseException e) {
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return null;
	}
	
	private void cleanupArduinoDb(Context context) {
		try {
			RfcxSource app = (RfcxSource) context.getApplicationContext();
			app.arduinoDb.dbTemperature.clearStatsBefore(lastTransmitTime);
			app.arduinoDb.dbHumidity.clearStatsBefore(lastTransmitTime);
		} catch (Exception e) {
			if (RfcxSource.verboseLog()) { Log.d(TAG, e.getMessage()); }
		}
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
		setPostUri();
	}
	
	private void setPostUri() {
		httpPost = new HttpPost(protocol+"://"+domain+":"+port+endpoint);
	}
	
	public String getDeviceId(RfcxSource rfcxSource) {
		if (deviceId == null) {
			deviceId = rfcxSource.getDeviceId().toString();
		}
		return deviceId;
	}
	
	
	// Getters & Setters
	
	public static boolean isApiCommEnabled() {
		return API_TRANSMIT_ENABLED;
	}
	
	public boolean isConnectivityPossible() {
		return networkConnectivity;
	}
	
	public void setConnectivity(boolean isConnected) {
		networkConnectivity = isConnected;
	}
	
	public int getConnectivityInterval() {
		return connectivityInterval;
	}
	
	public void setConnectivityInterval(int connectivityInterval) {
		this.connectivityInterval = connectivityInterval;
	}
	
}
