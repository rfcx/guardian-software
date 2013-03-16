package org.rfcx.src_api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
import org.rfcx.src_android.RfcxSource;
import org.rfcx.src_database.DeviceStateDb;
import org.rfcx.src_device.DeviceState;
import org.rfcx.src_util.DateTimeUtils;

import android.content.Context;
import android.util.Log;

public class ApiComm {

	private static final String TAG = ApiComm.class.getSimpleName();

	public static final boolean SERVICE_ENABLED = true;
	private boolean networkConnectivity = false;
	
	public static final int CONNECTIVITY_INTERVAL = 240;
	public static final int CONNECTIVITY_TIMEOUT = 150;
	
	DateTimeUtils dateTimeUtils = new DateTimeUtils();
	
	private String deviceId = null;
	private String protocol = "http";
	private String domain = null;
	private int port = 80;
	private String endpoint = "/";
	
	private HttpClient httpClient = new DefaultHttpClient();
	private HttpPost httpPost = null;
	private RfcxSource rfcxSource = null;
	private DeviceState deviceState = null;
	private DeviceStateDb deviceStateDb = null;
	
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
				Log.e(TAG, e.getMessage());
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			} finally {
				if ((strResponse == null) && (transmitAttempts <= 3)) {
					sendData(context);
					if (RfcxSource.VERBOSE) { Log.d(TAG, "Retransmitting... (attempt #"+transmitAttempts+")"); }
				} else {
					if (strResponse != null) {
						flushTransmittedData(sendDateTime);
						if (RfcxSource.VERBOSE) { Log.d(TAG, "Response: "+strResponse); }
					}
					transmitAttempts = 0;
					rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
					if (RfcxSource.VERBOSE) { Log.d(TAG, "Turning off antenna..."); }
				}
			}

		} else {
			Log.e(TAG, "httpPost is not set");
		}
	}
	
	private List<NameValuePair> preparePostData() {
		
		if (deviceStateDb == null) deviceStateDb = rfcxSource.deviceStateDb;
		if (deviceState == null) deviceState = rfcxSource.deviceState;
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
		nameValuePairs.add(new BasicNameValuePair("id", getDeviceId()));
		
		String[] vBattery = deviceStateDb.dbBattery.getStatsSummary();
		String[] vCpu = deviceStateDb.dbCpu.getStatsSummary();
		String[] vCpuClock = deviceStateDb.dbCpuClock.getStatsSummary();
		String[] vLight = deviceStateDb.dbLight.getStatsSummary();
		boolean vPower = !(rfcxSource.deviceState.isBatteryDisCharging());
		
		nameValuePairs.add(new BasicNameValuePair("battery", (vBattery[0]!="0") ? vBattery[4] : "") );
		nameValuePairs.add(new BasicNameValuePair("cpu",  (vCpu[0]!="0") ? vCpu[4] : "") );
		nameValuePairs.add(new BasicNameValuePair("cpuclock",  (vCpuClock[0]!="0") ? vCpuClock[4] : "") );
		nameValuePairs.add(new BasicNameValuePair("light",  (vLight[0]!="0") ? vLight[4] : "") );
		nameValuePairs.add(new BasicNameValuePair("power",  (vPower) ? "+" : "-") );
	    
        return nameValuePairs;
	}
	
	private void flushTransmittedData(Date date) {
		if (deviceStateDb != null) {
			deviceStateDb.dbBattery.clearStatsBefore(date);
			deviceStateDb.dbCpu.clearStatsBefore(date);
			deviceStateDb.dbCpuClock.clearStatsBefore(date);
			deviceStateDb.dbLight.clearStatsBefore(date);
		}
	}
	
	private String httpResponseString(HttpResponse httpResponse) {
		if (httpResponse.getEntity() != null) {
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

	
	public void setDomain(String domain) {
		this.domain = domain;
		setPostUri();
	}
	
	private void setPostUri() {
		httpPost = new HttpPost(protocol+"://"+domain+":"+port+endpoint);
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
	
	
}
