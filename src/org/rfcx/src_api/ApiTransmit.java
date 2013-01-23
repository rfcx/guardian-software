package org.rfcx.src_api;

import java.io.IOException;
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
import org.rfcx.src_util.DateTimeUtils;

public class ApiTransmit {

	private static final String TAG = ApiTransmit.class.getSimpleName();
	
	DateTimeUtils dateTimeUtils = new DateTimeUtils();
	
	private Date currTransmitTime = new Date();
	private Date lastTransmitTime = currTransmitTime;
	private String protocol = "http";
	private String domain = "10.0.0.23";
	private String endpoint = "/lib/rfcx.php";
	
	private HttpClient httpClient = new DefaultHttpClient();
	private HttpPost httpPost = new HttpPost(protocol+"://"+domain+endpoint);
	
	public void sendData(Context context) {
	    try {
	        httpPost.setEntity(new UrlEncodedFormEntity(preparePostData(context)));
	        HttpResponse httpResponse = httpClient.execute(httpPost);
	        Log.d(TAG, httpResponseString(httpResponse));
	    } catch (ClientProtocolException e) {
	    	Log.d(TAG, e.getMessage());
	    } catch (IOException e) {
	    	Log.d(TAG, e.getMessage());
	    }
	}
	
	private List<NameValuePair> preparePostData(Context context) {
		RfcxSource app = (RfcxSource) context.getApplicationContext();
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		
		int[] stats = app.arduinoDb.dbTemperature.getStatsSince(lastTransmitTime);
		Log.d(TAG, stats[0]+"- "+stats[1]);
		
        nameValuePairs.add(new BasicNameValuePair("temp", Integer.toString(app.deviceCpuUsage.getCpuUsageAvg())));
        nameValuePairs.add(new BasicNameValuePair("humi", Integer.toString(app.deviceCpuUsage.getCpuUsageAvg())));
        nameValuePairs.add(new BasicNameValuePair("dcpu", Integer.toString(app.deviceCpuUsage.getCpuUsageAvg())));
        
        return nameValuePairs;
	}
	
	private String httpResponseString(HttpResponse httpResponse) {
		if (httpResponse.getEntity() != null) {
			lastTransmitTime = currTransmitTime;
			currTransmitTime = new Date();
			try {
				return EntityUtils.toString(httpResponse.getEntity());
			} catch (ParseException e) {
				Log.d(TAG, e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, e.getMessage());
			}
		}
		return null;
	}
	
}
