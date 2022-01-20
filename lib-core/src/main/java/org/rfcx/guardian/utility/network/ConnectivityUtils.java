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

    public Double getDownloadSpeedTest() {
        return downloadSpeedTest;
    }

    public Double getUploadSpeedTest() {
        return uploadSpeedTest;
    }

    public void setDownloadSpeedTest(Context context, String role) {
        HttpGet httpGet = new HttpGet(context, role);
        try {
            downloadSpeedTest = httpGet.getDownloadSpeedTest("http://ipv4.ikoula.testdebit.info/1M.iso"); // test url
        } catch (IOException e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    public void setUploadSpeedTest(Context context, String role) {
        HttpPostMultipart httpPostMultipart = new HttpPostMultipart(context, role);
        try {
            uploadSpeedTest = httpPostMultipart.getUploadSpeedTest("http://ipv4.ikoula.testdebit.info"); // test url
        } catch (IOException e) {
            RfcxLog.logExc(logTag, e);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }
}
