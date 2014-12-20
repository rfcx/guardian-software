package org.rfcx.guardian.utility;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.rfcx.guardian.RfcxGuardian;

import android.content.Context;
import android.util.Log;

public class HttpHttpsPostMultiPart {

	private static final String TAG = HttpHttpsPostMultiPart.class.getSimpleName();
	
	public void executePostMultiPart(Context context) {
		Log.d(TAG, "Doing it");
		String filePath = context.getFilesDir().getPath()+"/flac/1418600660225.flac";
		File fileObj = new File(filePath);
		ContentBody contentBody = new FileBody(fileObj, "1418600660225.flac", "application/x-flac", null);
		
		String json = "{'guid':'123123123','name':'topher'}";
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		reqEntity.addPart("flac", contentBody);
		try {
			reqEntity.addPart("user", new StringBody(URLEncoder.encode(json)));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getMessage());
		}
		Log.d(TAG, postMultiPart("http://192.168.43.15:8080/v1/mapping/register", reqEntity));
	}
    
    
	
	private static String postMultiPart(String reqEndpoint, MultipartEntity reqEntity) {
	    try {
	        URL reqUrl = new URL(reqEndpoint);
	        HttpURLConnection reqConn = (HttpURLConnection) reqUrl.openConnection();
//	        HttpsURLConnection reqConn = (HttpsURLConnection) reqUrl.openConnection();
//	        conn.setReadTimeout(10000);
//	        conn.setConnectTimeout(15000);
	        reqConn.setRequestMethod("POST");
	        reqConn.setUseCaches(false);
	        reqConn.setDoInput(true);
	        reqConn.setDoOutput(true);

	        reqConn.setRequestProperty("Connection", "Keep-Alive");
	        reqConn.setFixedLengthStreamingMode((int) reqEntity.getContentLength());
//	        reqConn.addRequestProperty("Content-length", reqEntity.getContentLength()+"");
	        reqConn.addRequestProperty(reqEntity.getContentType().getName(), reqEntity.getContentType().getValue());

	        OutputStream outputStream = reqConn.getOutputStream();
	        reqEntity.writeTo(outputStream);
	        outputStream.close();
	        reqConn.connect();

	        //if (reqConn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
		    if (reqConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
	            return readResponseStream(reqConn.getInputStream());
	        }

	    } catch (Exception e) {
	        Log.e(TAG, "multipart post error " + e + "(" + reqEndpoint + ")");
	    }
	    return "no http return";        
	}

	private static String readResponseStream(InputStream in) {
	    BufferedReader reader = null;
	    StringBuilder builder = new StringBuilder();
	    try {
	        reader = new BufferedReader(new InputStreamReader(in));
	        String line = "";
	        while ((line = reader.readLine()) != null) {
	            builder.append(line);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (reader != null) {
	            try {
	                reader.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    return builder.toString();
	} 
	
	
}
