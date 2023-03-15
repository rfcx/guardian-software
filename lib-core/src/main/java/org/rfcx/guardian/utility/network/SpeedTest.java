package org.rfcx.guardian.utility.network;

import android.content.Context;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class SpeedTest {

    private static final String logTag = RfcxLog.generateLogTag("Utils", "ConnectivityUtils");

    private Double downloadSpeedKbps = -1.0;
    private Double uploadSpeedKbps = -1.0;
    private boolean isFailed = false;
    private boolean isTesting = false;

    public Double getDownloadSpeedTest() {
        return downloadSpeedKbps;
    }

    public Double getUploadSpeedTest() {
        return uploadSpeedKbps;
    }

    public boolean getFailed() {
        return isFailed;
    }

    public boolean getTestingState() {
        return isTesting;
    }

    public void setDownloadSpeedTest(Context context, String role) {
        DownloadSpeedTest downloadTest = new DownloadSpeedTest(context, role);
        try {
            isTesting = true;
            isFailed = false;
            downloadSpeedKbps = downloadTest.getDownloadSpeedTest("http://ipv4.appliwave.testdebit.info/100k.iso"); // test url
        } catch (IOException e) {
            isFailed = true;
            RfcxLog.logExc(logTag, e);
        }
        isTesting = false;
    }

    public void setUploadSpeedTest(Context context, String role) {
        UploadSpeedTest uploadTest = new UploadSpeedTest(context, role);
        try {
            isTesting = true;
            isFailed = false;
            uploadSpeedKbps = uploadTest.getUploadSpeedTest("http://ipv4.appliwave.testdebit.info", 102400); // test url
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            isFailed = true;
            RfcxLog.logExc(logTag, e);
        }
        isTesting = false;
    }
}
