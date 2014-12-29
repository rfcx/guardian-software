package org.rfcx.guardian.utility;

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
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class HttpGet {

	private static final String TAG = HttpGet.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	private static final String DOWNLOAD_TIME_LABEL = "Download time: ";

    // need to make these longer...
	private static int requestReadTimeout = 10000;
	private static int requestConnectTimeout = 30000;
	private static boolean useCaches = false;
	
	
	public JSONObject getAsJson(String fullUrl, List<String[]> keyValueParameters) throws ParseException {
		long startTime = Calendar.getInstance().getTimeInMillis();
		String str = doGetString(fullUrl,keyValueParameters);
		Log.d(TAG,DOWNLOAD_TIME_LABEL+(Calendar.getInstance().getTimeInMillis()-startTime)+"ms");
		return (JSONObject) (new JSONParser()).parse(str);
	}
	
	public JSONObject getAsJson(String fullUrl) throws ParseException {
		return getAsJson(fullUrl, (new ArrayList<String[]>()));
	}
	
	public String getAsString(String fullUrl, List<String[]> keyValueParameters) {
		long startTime = Calendar.getInstance().getTimeInMillis();
		String str = doGetString(fullUrl,keyValueParameters);
		Log.d(TAG,DOWNLOAD_TIME_LABEL+(Calendar.getInstance().getTimeInMillis()-startTime)+"ms");
		return str;
	}
	
	public String getAsString(String fullUrl) {
		return getAsString(fullUrl,(new ArrayList<String[]>()));
	}
	
	public boolean getAsFile(String fullUrl, List<String[]> keyValueParameters, String outputFileName, Context context) throws MalformedURLException, FileNotFoundException, IOException {
		long startTime = Calendar.getInstance().getTimeInMillis();
		StringBuilder url = (new StringBuilder()).append(fullUrl).append("?");
		for (String[] keyValue : keyValueParameters) {
			url.append(keyValue[0]).append("=").append(keyValue[1]).append("&");
		}
		writeFileResponseStream(
				httpGetFileInputStream(url.toString()),
				httpGetFileOutputStream(outputFileName,context)
			);
		Log.d(TAG,DOWNLOAD_TIME_LABEL+(Calendar.getInstance().getTimeInMillis()-startTime)+"ms");
		return (new File(context.getFilesDir(), outputFileName)).exists();
	}
	
	public boolean getAsFile(String fullUrl, String outputFileName, Context context) throws MalformedURLException, FileNotFoundException, IOException {
		return getAsFile(fullUrl, (new ArrayList<String[]>()), outputFileName, context);
	}	
	
	private static String doGetString(String fullUrl, List<String[]> keyValueParameters) {
		StringBuilder url = (new StringBuilder()).append(fullUrl).append("?");
		for (String[] keyValue : keyValueParameters) {
			url.append(keyValue[0]).append("=").append(keyValue[1]).append("&");
		}
		return executeGet(url.toString());
	}
    
	private static String executeGet(String fullUrl) {
		try {
	    	String inferredProtocol = fullUrl.substring(0, fullUrl.indexOf(":"));
			if (inferredProtocol.equals("http")) {
				return sendInsecureGetRequest((new URL(fullUrl)));
			} else if (inferredProtocol.equals("https")) {
				return sendSecureGetRequest((new URL(fullUrl)));
			} else {
				return "Inferred protocol was neither HTTP nor HTTPS.";
			}
		} catch (MalformedURLException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() + TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			return "Bad URL";
		}
	}
	
	private static String sendInsecureGetRequest(URL url) {
	    try {
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setReadTimeout(requestReadTimeout);
	        conn.setConnectTimeout(requestConnectTimeout);
	        conn.setRequestMethod("GET");
	        conn.setUseCaches(useCaches);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Connection", "Keep-Alive");
	        conn.connect();
		    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
	            return readResponseStream(conn.getInputStream());
	        }
	    } catch (Exception e) {
	    	Log.e(TAG,(e!=null) ? (e.getMessage() + TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	    }
	    return "The request returned an error.";        
	}
	
	private static String sendSecureGetRequest(URL url) {
	    try {
	        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
	        conn.setReadTimeout(requestReadTimeout);
	        conn.setConnectTimeout(requestConnectTimeout);
	        conn.setRequestMethod("GET");
	        conn.setUseCaches(useCaches);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Connection", "Keep-Alive");
	        conn.connect();
		    if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
	            return readResponseStream(conn.getInputStream());
	        }
	    } catch (Exception e) {
	    	Log.e(TAG,(e!=null) ? (e.getMessage() + TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	    }
	    return "The request returned an error.";    
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
	    	Log.e(TAG,(e!=null) ? (e.getMessage() + TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	    } finally {
	        if (bufferedReader != null) {
	            try {
	                bufferedReader.close();
	            } catch (IOException e) {
	            	Log.e(TAG,(e!=null) ? (e.getMessage() + TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	            }
	        }
	    }
	    return stringBuilder.toString();
	} 

	private static void writeFileResponseStream(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
		byte[] buffer = new byte[8192];
		int bufferLength = 0;
		while ((bufferLength = inputStream.read(buffer)) != -1) {
			fileOutputStream.write(buffer, 0, bufferLength);
		}
		inputStream.close();
		fileOutputStream.flush();
		fileOutputStream.close();
	}
	
	private static FileOutputStream httpGetFileOutputStream(String fileName, Context context) throws FileNotFoundException {
		String filesDir = context.getFilesDir().toString();
		if ((new File(filesDir,fileName)).exists()) { (new File(filesDir,fileName)).delete(); }
		return context.openFileOutput(fileName, Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE);
	}
	
	private static InputStream httpGetFileInputStream(String fullUrl) throws MalformedURLException, IOException {
    	String inferredProtocol = fullUrl.substring(0, fullUrl.indexOf(":"));
		if (inferredProtocol.equals("http")) {
			HttpsURLConnection conn = (HttpsURLConnection) (new URL(fullUrl)).openConnection();
	        conn.setReadTimeout(requestReadTimeout);
	        conn.setConnectTimeout(requestConnectTimeout);
	        conn.setRequestMethod("GET");
	        conn.setUseCaches(useCaches);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Connection", "Keep-Alive");
	        conn.connect();
	        if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
	        	return conn.getInputStream();
	        }
		} else if (inferredProtocol.equals("https")) {
			HttpURLConnection conn = (HttpURLConnection) (new URL(fullUrl)).openConnection();
	        conn.setReadTimeout(requestReadTimeout);
	        conn.setConnectTimeout(requestConnectTimeout);
	        conn.setRequestMethod("GET");
	        conn.setUseCaches(useCaches);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Connection", "Keep-Alive");
	        conn.connect();
	        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
	        	return conn.getInputStream();
	        }
		} else {
			Log.e(TAG,"Inferred protocol was neither HTTP nor HTTPS.");
		}
		return null;
	}


}
