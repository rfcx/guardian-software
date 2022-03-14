package org.rfcx.guardian.utility.network;

import android.content.Context;
import android.util.Log;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

public class HttpPostMultipart {

    private final Context context;
    private final String logTag;
    // These hard coded timeout values are just defaults.
    // They may be customized through the setTimeOuts method.
    private int requestReadTimeout = 300000;
    private int requestConnectTimeout = 300000;
    private final boolean useCaches = false;
    private List<String[]> customHttpHeaders = new ArrayList<String[]>();

    public HttpPostMultipart(Context context, String appRole) {
        this.context = context;
        this.logTag = RfcxLog.generateLogTag(appRole, "HttpPostMultipart");
    }

    public void setTimeOuts(int connectTimeOutMs, int readTimeOutMs) {
        this.requestConnectTimeout = connectTimeOutMs;
        this.requestReadTimeout = readTimeOutMs;
    }

    public List<String[]> getCustomHttpHeaders() {
        return this.customHttpHeaders;
    }

    public void setCustomHttpHeaders(List<String[]> keyValueHeaders) {
        List<String[]> newCustomHttpHeaders = new ArrayList<String[]>();
        for (String[] keyValueHeader : keyValueHeaders) {
            newCustomHttpHeaders.add(keyValueHeader);
        }
        this.customHttpHeaders = newCustomHttpHeaders;
    }

    public String doMultipartPost(String fullUrl, List<String[]> keyValueParameters, List<String[]> keyFilepathMimeAttachments) throws IOException, NoSuchAlgorithmException, KeyManagementException {

        /* fullUrl: url as a string
         * keyValueParameters: List of arrays of strings, with the indices: [fieldname, fieldvalue]
         * keyFilepathMimeAttachments: List of arrays of string, with indices [fieldname, filepath, file-mime]
         */

        MultipartEntity requestEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        if (keyFilepathMimeAttachments != null) {
            for (String[] keyFilepathMime : keyFilepathMimeAttachments) {
                ContentBody contentBody = new FileBody(
                        (new File(keyFilepathMime[1])),
                        keyFilepathMime[1].substring(1 + keyFilepathMime[1].lastIndexOf("/")),
                        keyFilepathMime[2], null);
                requestEntity.addPart(keyFilepathMime[0], contentBody);
            }
        }
        for (String[] keyValue : keyValueParameters) {
            requestEntity.addPart(keyValue[0], new StringBody(URLEncoder.encode(keyValue[1], "UTF-8")));
        }

        long startTime = System.currentTimeMillis();
        Log.v(logTag, "Sending " + FileUtils.bytesAsReadableString(requestEntity.getContentLength()) + " to " + fullUrl);
        String rtrnStr = executeMultipartPost(fullUrl, requestEntity);
        Log.v(logTag, "Completed (" + DateTimeUtils.milliSecondDurationAsReadableString(System.currentTimeMillis() - startTime) + ") from " + fullUrl);
        return rtrnStr;
    }

    protected String executeMultipartPost(String fullUrl, MultipartEntity requestEntity) throws IOException, KeyManagementException, NoSuchAlgorithmException {

        String inferredProtocol = fullUrl.substring(0, fullUrl.indexOf(":"));
        if (inferredProtocol.equals("http")) {
            return sendInsecurePostRequest((new URL(fullUrl)), requestEntity);
        } else if (inferredProtocol.equals("https")) {
            return sendSecurePostRequest((new URL(fullUrl)), requestEntity);
        } else {
            Log.e(logTag, "Inferred protocol was neither HTTP nor HTTPS.");
            return null;
        }
    }

    private String sendInsecurePostRequest(URL url, MultipartEntity entity) throws IOException {
        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(requestReadTimeout);
        conn.setConnectTimeout(requestConnectTimeout);
        conn.setRequestMethod("POST");
        conn.setUseCaches(useCaches);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        for (String[] keyValueHeader : this.customHttpHeaders) {
            conn.setRequestProperty(keyValueHeader[0], keyValueHeader[1]);
        }
        conn.setFixedLengthStreamingMode((int) entity.getContentLength());
        conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());
        OutputStream outputStream = conn.getOutputStream();
        entity.writeTo(outputStream);
        outputStream.close();
        conn.connect();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            Log.v(logTag, "Downloading " + FileUtils.bytesAsReadableString(conn.getContentLength()) + " from " + url);
            return readResponseStream("gzip".equalsIgnoreCase(conn.getContentEncoding()) ? (new GZIPInputStream(conn.getInputStream())) : conn.getInputStream());
        } else {
            Log.e(logTag, "HTTP Failure Code: " + conn.getResponseCode() + " for " + url);
        }
        return null;
    }

    private String sendSecurePostRequest(URL url, MultipartEntity entity) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setReadTimeout(requestReadTimeout);
        conn.setConnectTimeout(requestConnectTimeout);
        conn.setRequestMethod("POST");
        conn.setUseCaches(useCaches);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setSSLSocketFactory(new TLSSocketFactory());
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        for (String[] keyValueHeader : this.customHttpHeaders) {
            conn.setRequestProperty(keyValueHeader[0], keyValueHeader[1]);
        }
        conn.setFixedLengthStreamingMode((int) entity.getContentLength());
        conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());
        OutputStream outputStream = conn.getOutputStream();
        entity.writeTo(outputStream);
        outputStream.close();
        conn.connect();
        if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            Log.v(logTag, "Downloading " + FileUtils.bytesAsReadableString(conn.getContentLength()) + " from " + url);
            return readResponseStream("gzip".equalsIgnoreCase(conn.getContentEncoding()) ? (new GZIPInputStream(conn.getInputStream())) : conn.getInputStream());
        } else {
            Log.e(logTag, "HTTP Failure Code: " + conn.getResponseCode() + " for " + url);
        }
        return null;
    }

    private String readResponseStream(InputStream inputStream) {
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
