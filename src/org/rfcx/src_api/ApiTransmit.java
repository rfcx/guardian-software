package org.rfcx.src_api;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.rfcx.rfcx_src_android.RfcxSource;
import org.rfcx.src_util.DateTimeUtils;

public class ApiTransmit {

	private static final String TAG = ApiTransmit.class.getSimpleName();
	
	DateTimeUtils dateTimeUtils = new DateTimeUtils();
	
	private Date currTransmitTime = new Date();
	private Date lastTransmitTime = currTransmitTime;
	private String protocol = "http";
	private String domain = null;
	private String endpoint = "/lib/rfcx.php";
	
	private HttpClient httpClient = new DefaultHttpClient();
	private HttpPost httpPost = null;
	
	public void sendData(Context context) {
		if (httpPost != null) {
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(preparePostData(context)));
				HttpResponse httpResponse = httpClient.execute(httpPost);
	        	String strResponse = httpResponseString(httpResponse);
				if (RfcxSource.verboseLog()) { Log.d(TAG, strResponse); }
			} catch (Exception e) {
				if (RfcxSource.verboseLog()) { Log.d(TAG, e.getMessage()); }
			}
		} else {
			Log.e(TAG, "httpPost is not set");
		}
	}
	
	private List<NameValuePair> preparePostData(Context context) {
		RfcxSource app = (RfcxSource) context.getApplicationContext();
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		
		int[] statsTemp = app.arduinoDb.dbTemperature.getStatsSince(lastTransmitTime);
		int[] statsHumi = app.arduinoDb.dbHumidity.getStatsSince(lastTransmitTime);
		String[] currCharge = app.arduinoDb.dbCharge.getLast();
		
		if (statsTemp[0] > 0) { nameValuePairs.add(new BasicNameValuePair("atmp", Integer.toString(statsTemp[1]))); }
		if (statsHumi[0] > 0) { nameValuePairs.add(new BasicNameValuePair("ahmd", Integer.toString(statsHumi[1]))); }
		if (currCharge[1] != "0") { nameValuePairs.add(new BasicNameValuePair("achg", currCharge[1])); }
        nameValuePairs.add(new BasicNameValuePair("dcpu", Integer.toString(app.deviceCpuUsage.getCpuUsageAvg())));
        
        return nameValuePairs;
	}
	
	private String httpResponseString(HttpResponse httpResponse) {
		if (httpResponse.getEntity() != null) {
			lastTransmitTime = currTransmitTime;
			currTransmitTime = new Date();
			try {
				return EntityUtils.toString(httpResponse.getEntity());
			} catch (Exception e) {
				if (RfcxSource.verboseLog()) { Log.d(TAG, e.getMessage()); }
			}
		}
		return null;
	}
	
	private void cleanupArduinoDb(Context context) {
		RfcxSource app = (RfcxSource) context.getApplicationContext();
		app.arduinoDb.dbTemperature.clearStatsBefore(lastTransmitTime);
		app.arduinoDb.dbHumidity.clearStatsBefore(lastTransmitTime);	
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
		setPostUri();
	}
	
	private void setPostUri() {
		httpPost = new HttpPost(protocol+"://"+domain+endpoint);
	}
	
}
