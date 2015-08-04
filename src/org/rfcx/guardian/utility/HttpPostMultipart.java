package org.rfcx.guardian.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import android.text.TextUtils;
import android.util.Log;

public class HttpPostMultipart {

	private static final String TAG = "RfcxGuardian-"+HttpPostMultipart.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	// These hard coded timeout values are just defaults.
	// They may be customized through the setTimeOuts method.
	private int requestReadTimeout = 300000;
	private int requestConnectTimeout = 300000;
	private static boolean useCaches = false;
	
	private List<String[]> customHttpHeaders = new ArrayList<String[]>();
	
	public void setTimeOuts(int connectTimeOutMs, int readTimeOutMs) {
		this.requestConnectTimeout = connectTimeOutMs;
		this.requestReadTimeout = readTimeOutMs;
	}
	
	public void setCustomHttpHeaders(List<String[]> keyValueHeaders) {
		List<String[]> newCustomHttpHeaders = new ArrayList<String[]>();
		for (String[] keyValueHeader : keyValueHeaders) {
			newCustomHttpHeaders.add(keyValueHeader);
		}
		this.customHttpHeaders = newCustomHttpHeaders;
	}
	
	public List<String[]> getCustomHttpHeaders() {
		return this.customHttpHeaders;
	}
	
	public String doMultipartPost(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments) {
		
		/* fullUrl: url as a string
		 * keyValueParameters: List of arrays of strings, with the indices: [fieldname, fieldvalue]
		 * keyFilepathMimeAttachments: List of arrays of string, with indices [fieldname, filepath, file-mime]
		 */
		
		MultipartEntity requestEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		try {
			for (String[] keyFilepathMime : keyFilepathMimeAttachments) {
				ContentBody contentBody = new FileBody(
						(new File(keyFilepathMime[1])),
						keyFilepathMime[1].substring(1+keyFilepathMime[1].lastIndexOf("/")), 
						keyFilepathMime[2], null);
				requestEntity.addPart(keyFilepathMime[0], contentBody);
			}
			for (String[] keyValue : keyValueParameters) {
				requestEntity.addPart(keyValue[0], new StringBody(URLEncoder.encode(keyValue[1], "UTF-8")));
			}
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return executeMultipartPost(fullUrl, requestEntity);
	}
    
	private String executeMultipartPost(String fullUrl, MultipartEntity requestEntity) {
		try {
	    	String inferredProtocol = fullUrl.substring(0, fullUrl.indexOf(":"));
			if (inferredProtocol.equals("http")) {
				return sendInsecurePostRequest((new URL(fullUrl)), requestEntity);
			} else if (inferredProtocol.equals("https")) {
				return sendSecurePostRequest((new URL(fullUrl)), requestEntity);
			} else {
				Log.e(TAG,"Inferred protocol was neither HTTP nor HTTPS.");
				return "";
			}
		} catch (MalformedURLException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			Log.e(TAG,"Malformed URL");
			return "";
		}
	}
	
	private String sendInsecurePostRequest(URL url, MultipartEntity entity) {
	    try {
	        HttpURLConnection conn;
			conn = (HttpURLConnection) url.openConnection();
	        conn.setReadTimeout(requestReadTimeout);
	        conn.setConnectTimeout(requestConnectTimeout);
	        conn.setRequestMethod("POST");
	        conn.setUseCaches(useCaches);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Connection", "Keep-Alive");
			for (String[] keyValueHeader : this.customHttpHeaders) { conn.setRequestProperty(keyValueHeader[0], keyValueHeader[1]); }
	        conn.setFixedLengthStreamingMode((int) entity.getContentLength());
	        conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());
	        OutputStream outputStream = conn.getOutputStream();
	        entity.writeTo(outputStream);
	        outputStream.close();
	        conn.connect();
		    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
	            Log.d(TAG, "HTTP Response Code: "+conn.getResponseCode()+" for "+url.toString());
		    } else {
	            Log.e(TAG, "HTTP Response Code: "+conn.getResponseCode()+" for "+url.toString());
		    }
		    return readResponseStream(conn.getInputStream());
	    } catch (UnknownHostException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
				return TAG+"-UnknownHostException";
	    } catch (ProtocolException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	    } catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
	    return "";        
	}
	
	private String sendSecurePostRequest(URL url, MultipartEntity entity) {
	    try {
	        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
	        conn.setReadTimeout(requestReadTimeout);
	        conn.setConnectTimeout(requestConnectTimeout);
	        conn.setRequestMethod("POST");
	        conn.setUseCaches(useCaches);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Connection", "Keep-Alive");
			for (String[] keyValueHeader : this.customHttpHeaders) { conn.setRequestProperty(keyValueHeader[0], keyValueHeader[1]); }
	        conn.setFixedLengthStreamingMode((int) entity.getContentLength());
	        conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());
	        OutputStream outputStream = conn.getOutputStream();
	        entity.writeTo(outputStream);
	        outputStream.close();
	        conn.connect();
		    if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
	            Log.d(TAG, "HTTP Response Code: "+conn.getResponseCode()+" for "+url.toString());
		    } else {
	            Log.e(TAG, "HTTP Response Code: "+conn.getResponseCode()+" for "+url.toString());
		    }
	        return readResponseStream(conn.getInputStream());
	    } catch (UnknownHostException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
				return TAG+"-UnknownHostException";
	    } catch (ProtocolException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	    } catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
	    return "";        
	}

	private static String readResponseStream(InputStream inputStream) {
	    BufferedReader bufferedReader = null;
	    StringBuilder stringBuilder = new StringBuilder();
	    try {
	        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	        String currentLine = "";
	        while ((currentLine = bufferedReader.readLine()) != null) {
	            stringBuilder.append(currentLine);
	        }
	    } catch (IOException e) {
	    	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	    } finally {
	        if (bufferedReader != null) {
	            try {
	                bufferedReader.close();
	            } catch (IOException e) {
	            	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	            }
	        }
	    }
	    return stringBuilder.toString();
	} 
	
}
