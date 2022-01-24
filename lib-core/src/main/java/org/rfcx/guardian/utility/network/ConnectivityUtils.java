package org.rfcx.guardian.utility.network;

import android.content.Context;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class ConnectivityUtils {

    private static final String logTag = RfcxLog.generateLogTag("Utils", "ConnectivityUtils");

    private Double downloadSpeedTest = -1.0;
    private Double uploadSpeedTest = -1.0;
    private boolean isFailed = false;

    public Double getDownloadSpeedTest() {
        return downloadSpeedTest;
    }

    public Double getUploadSpeedTest() {
        return uploadSpeedTest;
    }

    public boolean getFailed() {
        return isFailed;
    }

    public void setDownloadSpeedTest(Context context, String role) {
        HttpGet httpGet = new HttpGet(context, role);
        try {
            isFailed = false;
            downloadSpeedTest = httpGet.getDownloadSpeedTest("http://ipv4.ikoula.testdebit.info/1M.iso"); // test url
        } catch (IOException e) {
            isFailed = true;
            RfcxLog.logExc(logTag, e);
        }
    }

    public void setUploadSpeedTest(Context context, String role) {
        HttpPostMultipart httpPostMultipart = new HttpPostMultipart(context, role);
        try {
            isFailed = false;
            uploadSpeedTest = httpPostMultipart.getUploadSpeedTest("http://ipv4.ikoula.testdebit.info", 1000000); // test url
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            isFailed = true;
            RfcxLog.logExc(logTag, e);
        }
    }
}
