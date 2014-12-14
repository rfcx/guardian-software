package org.rfcx.guardian.utility;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;

import android.util.Log;

public class HttpHttpsPostMultiPart {

	private static final String TAG = HttpHttpsPostMultiPart.class.getSimpleName();
	
	
 //   Bitmap bitmap = ...;
    String attachmentFileName = "filename.png";
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
    ContentBody contentPart = new ByteArrayBody(byteStream.toByteArray(), attachmentFileName);

    MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

//    reqEntity.addPart("picture", contentPart);
    String reqResponse = postMultiPart("http://server.com", reqEntity);
    
	
	private static String postMultiPart(String reqEndpoint, MultipartEntity reqEntity) {
	    try {
	        URL reqUrl = new URL(reqEndpoint);
	        HttpsURLConnection reqConn = (HttpsURLConnection) reqUrl.openConnection();
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
	        reqEntity.writeTo(reqConn.getOutputStream());
	        outputStream.close();
	        reqConn.connect();

	        if (reqConn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
	            return readResponseStream(reqConn.getInputStream());
	        }

	    } catch (Exception e) {
	        Log.e(TAG, "multipart post error " + e + "(" + reqEndpoint + ")");
	    }
	    return null;        
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
