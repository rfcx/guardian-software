package org.rfcx.guardian.utility.http;

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

import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class HttpPostMultipart {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", HttpPostMultipart.class);

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
			RfcxLog.logExc(logTag, e);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
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
				Log.e(logTag,"Inferred protocol was neither HTTP nor HTTPS.");
				return "";
			}
		} catch (MalformedURLException e) {
			RfcxLog.logExc(logTag, e);
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
	            Log.d(logTag, "HTTP Response Code: "+conn.getResponseCode()+" for "+url.toString());
		    } else {
	            Log.e(logTag, "HTTP Response Code: "+conn.getResponseCode()+" for "+url.toString());
		    }
		    return readResponseStream(conn.getInputStream());
	    } catch (UnknownHostException e) {
			RfcxLog.logExc(logTag, e);
			return logTag+"-UnknownHostException";
	    } catch (ProtocolException e) {
			RfcxLog.logExc(logTag, e);
	    } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
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
	            Log.d(logTag, "HTTP Response Code: "+conn.getResponseCode()+" for "+url.toString());
		    } else {
	            Log.e(logTag, "HTTP Response Code: "+conn.getResponseCode()+" for "+url.toString());
		    }
	        return readResponseStream(conn.getInputStream());
	    } catch (UnknownHostException e) {
			RfcxLog.logExc(logTag, e);
			return logTag+"-UnknownHostException";
	    } catch (ProtocolException e) {
			RfcxLog.logExc(logTag, e);
	    } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
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
			RfcxLog.logExc(logTag, e);
	    } finally {
	        if (bufferedReader != null) {
	            try {
	                bufferedReader.close();
	            } catch (IOException e) {
	    			RfcxLog.logExc(logTag, e);
	            }
	        }
	    }
	    return stringBuilder.toString();
	} 
	
}
