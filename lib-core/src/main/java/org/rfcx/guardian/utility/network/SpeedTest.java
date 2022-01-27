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

    public Double getDownloadSpeedTest() {
        return downloadSpeedKbps;
    }

    public Double getUploadSpeedTest() {
        return uploadSpeedKbps;
    }

    public boolean getFailed() {
        return isFailed;
    }

    public void setDownloadSpeedTest(Context context, String role) {
        DownloadSpeedTest downloadTest = new DownloadSpeedTest(context, role);
        try {
            isFailed = false;
            downloadSpeedKbps = downloadTest.getDownloadSpeedTest("http://ipv4.ikoula.testdebit.info/100k.iso"); // test url
        } catch (IOException e) {
            isFailed = true;
            RfcxLog.logExc(logTag, e);
        }
    }

    public void setUploadSpeedTest(Context context, String role) {
        UploadSpeedTest uploadTest = new UploadSpeedTest(context, role);
        try {
            isFailed = false;
            uploadSpeedKbps = uploadTest.getUploadSpeedTest("http://ipv4.ikoula.testdebit.info", 100000); // test url
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            isFailed = true;
            RfcxLog.logExc(logTag, e);
        }
    }
}
