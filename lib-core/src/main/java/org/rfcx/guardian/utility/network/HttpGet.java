package org.rfcx.guardian.utility.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class HttpGet {
		
	public HttpGet(Context context, String appRole) {
		this.context = context;
		this.logTag = RfcxLog.generateLogTag(appRole, "HttpGet");
	}
	
	private Context context;
	private String logTag;
	
	// These hard coded timeout values are just defaults.
	// They may be customized through the setTimeOuts method.
	private int requestReadTimeout = 600000;
	private int requestConnectTimeout = 30000;
	private boolean useCaches = false;
	
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
	
	public JSONObject getAsJson(String fullUrl, List<String[]> keyValueParameters) {
		long startTime = System.currentTimeMillis();
		String str = doGetString(fullUrl,keyValueParameters);
		Log.i(logTag,"Completed: (" + DateTimeUtils.milliSecondDurationAsReadableString(System.currentTimeMillis()-startTime ) +"): "+fullUrl);
		try {
			return new JSONObject(str);
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	public JSONObject getAsJson(String fullUrl) {
		return getAsJson(fullUrl, (new ArrayList<String[]>()));
	}
	
	public List<JSONObject> getAsJsonList(String fullUrl, List<String[]> keyValueParameters) {
		long startTime = System.currentTimeMillis();
		String str = doGetString(fullUrl,keyValueParameters);
		Log.i(logTag,"Completed: (" + DateTimeUtils.milliSecondDurationAsReadableString(System.currentTimeMillis()-startTime ) +"): "+fullUrl);
		try {
			List<JSONObject> jsonArray = new ArrayList<JSONObject>();
			JSONArray jsonAll = new JSONArray(str);
			for (int i = 0; i < jsonAll.length(); i++) {
				jsonArray.add(jsonAll.getJSONObject(i));
			}
			return jsonArray;
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	public List<JSONObject> getAsJsonList(String fullUrl) {
		return getAsJsonList(fullUrl, (new ArrayList<String[]>()));
	}
	
	public String getAsString(String fullUrl, List<String[]> keyValueParameters) {
		long startTime = System.currentTimeMillis();
		String str = doGetString(fullUrl,keyValueParameters);
		Log.i(logTag,"Completed: (" + DateTimeUtils.milliSecondDurationAsReadableString(System.currentTimeMillis()-startTime ) +"): "+fullUrl);
		return str;
	}
	
	public String getAsString(String fullUrl) {
		return getAsString(fullUrl,(new ArrayList<String[]>()));
	}
	
	public boolean getAsFile(String fullUrl, List<String[]> keyValueParameters, String outputFileName) {
		long startTime = System.currentTimeMillis();
		StringBuilder url = (new StringBuilder()).append(fullUrl);
		if (keyValueParameters.size() > 0) url.append("?");
		for (String[] keyValue : keyValueParameters) {
			url.append(keyValue[0]).append("=").append(keyValue[1]).append("&");
		}
		Log.v(logTag,"HTTP GET: "+url.toString());
		FileOutputStream fileOutputStream = httpGetFileOutputStream(outputFileName, this.context, this.logTag);
		InputStream inputStream = httpGetFileInputStream(url.toString());
		if ((inputStream != null) && (fileOutputStream != null)) {
			writeFileResponseStream(inputStream, fileOutputStream, this.logTag);
			closeInputOutputStreams(inputStream, fileOutputStream, this.logTag);
			Log.i(logTag,"Completed: (" + DateTimeUtils.milliSecondDurationAsReadableString(System.currentTimeMillis()-startTime ) +"): "+fullUrl);
			return (new File(this.context.getFilesDir(), outputFileName)).exists();
		}
		return false;
	}
	
	public boolean getAsFile(String fullUrl, String outputFileName) {
		return getAsFile(fullUrl, (new ArrayList<String[]>()), outputFileName);
	}	
	
	private String doGetString(String fullUrl, List<String[]> keyValueParameters) {
		StringBuilder url = (new StringBuilder()).append(fullUrl);
		if (keyValueParameters != null) {
			if (keyValueParameters.size() > 0) url.append("?");
			for (String[] keyValue : keyValueParameters) {
				url.append(keyValue[0]).append("=").append(keyValue[1]).append("&");
			}
		}
		Log.v(logTag,"HTTP GET: "+url.toString());
		return executeGet(url.toString());
	}
    
	private String executeGet(String fullUrl) {
		try {
	    	String inferredProtocol = fullUrl.substring(0, fullUrl.indexOf(":"));
			if (inferredProtocol.equals("http")) {
				return sendInsecureGetRequest((new URL(fullUrl)));
			} else if (inferredProtocol.equals("https")) {
				return sendSecureGetRequest((new URL(fullUrl)));
			} else {
				Log.e(logTag, "Inferred protocol was neither HTTP nor HTTPS.");
			}
		} catch (MalformedURLException e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	private String sendInsecureGetRequest(URL url) {
	    try {
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setReadTimeout(requestReadTimeout);
	        conn.setConnectTimeout(requestConnectTimeout);
	        conn.setRequestMethod("GET");
	        conn.setUseCaches(useCaches);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Connection", "Keep-Alive");
	        conn.setRequestProperty("Accept-Encoding", "gzip");
			for (String[] keyValueHeader : this.customHttpHeaders) { conn.setRequestProperty(keyValueHeader[0], keyValueHeader[1]); }
	        conn.connect();
		    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Log.i(logTag, "Requested: ("+ FileUtils.bytesAsReadableString(conn.getContentLength()) +"): "+url.toString());
				return readResponseStream("gzip".equalsIgnoreCase(conn.getContentEncoding()) ? (new GZIPInputStream(conn.getInputStream())) : conn.getInputStream(), this.logTag);

	        } else {
	        	Log.e(logTag, "HTTP Failure Code: "+conn.getResponseCode());
	        }
	    } catch (Exception e) {
			RfcxLog.logExc(logTag, e);
	    }
	    return null;        
	}
	
	private String sendSecureGetRequest(URL url) {
	    try {
	        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
	        conn.setReadTimeout(requestReadTimeout);
	        conn.setConnectTimeout(requestConnectTimeout);
	        conn.setRequestMethod("GET");
	        conn.setUseCaches(useCaches);
	        conn.setDoInput(true);
	        conn.setDoOutput(false);
	        conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Accept-Encoding", "gzip");
			for (String[] keyValueHeader : this.customHttpHeaders) {
				conn.setRequestProperty(keyValueHeader[0], keyValueHeader[1]);
			}
	        conn.connect();
		    if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
				Log.i(logTag, "Requested: ("+ FileUtils.bytesAsReadableString(conn.getContentLength()) +"): "+url.toString());
		    	return readResponseStream("gzip".equalsIgnoreCase(conn.getContentEncoding()) ? (new GZIPInputStream(conn.getInputStream())) : conn.getInputStream(), this.logTag);

	        } else {
	        	Log.e(logTag, "HTTP Failure Code: "+conn.getResponseCode());
	        }
	    } catch (Exception e) {
			RfcxLog.logExc(logTag, e);
	    }
	    return null;    
	}

	private static String readResponseStream(InputStream inputStream, String logTag) {
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

	private static void writeFileResponseStream(InputStream inputStream, FileOutputStream fileOutputStream, String logTag) {
		try {
			byte[] buffer = new byte[8192];
			int bufferLength = 0;
			while ((bufferLength = inputStream.read(buffer)) != -1) {
				fileOutputStream.write(buffer, 0, bufferLength);
			}
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	private static void closeInputOutputStreams(InputStream inputStream, FileOutputStream fileOutputStream, String logTag) {
		try {
			inputStream.close();
			fileOutputStream.flush();
			fileOutputStream.close();
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	private static FileOutputStream httpGetFileOutputStream(String fileName, Context context, String logTag) {
		File targetFile = new File(context.getFilesDir().toString()+"/"+fileName);
		if (targetFile.exists()) { targetFile.delete(); }
		try {
			return context.openFileOutput(fileName, Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE);
		} catch (FileNotFoundException e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	private InputStream httpGetFileInputStream(String fullUrl) {
    	String inferredProtocol = fullUrl.substring(0, fullUrl.indexOf(":"));
    	try {
			if (inferredProtocol.equals("https")) {
				HttpsURLConnection conn = (HttpsURLConnection) (new URL(fullUrl)).openConnection();
		        conn.setReadTimeout(this.requestReadTimeout);
		        conn.setConnectTimeout(this.requestConnectTimeout);
		        conn.setRequestMethod("GET");
		        conn.setUseCaches(this.useCaches);
		        conn.setDoInput(true);
		        conn.setDoOutput(true);
		        conn.setRequestProperty("Connection", "Keep-Alive");
		        conn.connect();
		        if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
					Log.i(logTag, "Requested: ("+ FileUtils.bytesAsReadableString(conn.getContentLength()) +"): "+fullUrl);
			    } else {
		            Log.i(logTag, "Download Failure: (Response Code "+conn.getResponseCode()+"):"+fullUrl);
			    }
		        return conn.getInputStream();
			} else if (inferredProtocol.equals("http")) {
				HttpURLConnection conn = (HttpURLConnection) (new URL(fullUrl)).openConnection();
		        conn.setReadTimeout(this.requestReadTimeout);
		        conn.setConnectTimeout(this.requestConnectTimeout);
		        conn.setRequestMethod("GET");
		        conn.setUseCaches(this.useCaches);
		        conn.setDoInput(true);
		        conn.setDoOutput(false);
		        conn.setRequestProperty("Connection", "Keep-Alive");
		        conn.connect();
		        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
					Log.i(logTag, "Requested: ("+ FileUtils.bytesAsReadableString(conn.getContentLength()) +"): "+fullUrl);
			    } else {
		            Log.i(logTag, "Download Failure: (Response Code "+conn.getResponseCode()+"):"+fullUrl);
			    }
		        return conn.getInputStream();
			} else {
				Log.e(logTag,"Inferred protocol was neither HTTP nor HTTPS.");
				return null;
			}
    	} catch (MalformedURLException e) {
			RfcxLog.logExc(logTag, e);
    	} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
    	}
		return null;
	}
	
}
