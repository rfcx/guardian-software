package org.rfcx.guardian.utility.asset;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RfcxScreenShotFileUtils {

    public static final String CAPTURE_FILETYPE = "png";
    private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM-dd/HH", Locale.US);
    private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);
    private final String logTag;
    private String appRole = "Utils";
    private String rfcxDeviceId = null;

    public RfcxScreenShotFileUtils(Context context, String appRole, String rfcxDeviceId) {
        this.logTag = RfcxLog.generateLogTag(appRole, "RfcxScreenShotUtils");
        this.appRole = appRole;
        this.rfcxDeviceId = rfcxDeviceId;
        initializeScreenShotDirectories(context);
    }

    public static void initializeScreenShotDirectories(Context context) {

        FileUtils.initializeDirectoryRecursively(screenShotCaptureDir(context), false);
        FileUtils.initializeDirectoryRecursively(screenShotQueueDir(context), false);
    }

    public static String screenShotQueueDir(Context context) {
        return context.getFilesDir().toString() + "/screenshots/queue";
    }

    public static String getScreenShotExecutableBinaryFilePath(Context context) {
        return "/system/bin/screencap";
    }

    public static String screenShotCaptureDir(Context context) {
        return context.getFilesDir().toString() + "/screenshots/capture";
    }

    public static String getScreenShotFileLocation_Capture(Context context, long timestamp) {
        return screenShotCaptureDir(context) + "/" + timestamp + "." + CAPTURE_FILETYPE;
    }

    public static String getScreenShotFileLocation_Queue(String rfcxDeviceId, Context context, long timestamp) {
        return screenShotQueueDir(context) + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + CAPTURE_FILETYPE;
    }


    public String[] launchCapture(Context context) {

        String executableBinaryFilePath = RfcxScreenShotFileUtils.getScreenShotExecutableBinaryFilePath(context);

        if ((new File(executableBinaryFilePath)).exists()) {

            try {

                long captureTimestamp = System.currentTimeMillis();

                String captureFilePath = RfcxScreenShotFileUtils.getScreenShotFileLocation_Capture(context, captureTimestamp);
                String finalFilePath = RfcxScreenShotFileUtils.getScreenShotFileLocation_Queue(this.rfcxDeviceId, context, captureTimestamp);

                Process shellProcess = Runtime.getRuntime().exec(new String[]{executableBinaryFilePath, captureFilePath});
                shellProcess.waitFor();
                FileUtils.chmod(captureFilePath, "rw", "rw");

                return completeCapture(captureTimestamp, captureFilePath, finalFilePath, context.getResources().getDisplayMetrics().widthPixels, context.getResources().getDisplayMetrics().heightPixels);

            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }
        } else {
            Log.e(logTag, "Executable not found: " + executableBinaryFilePath);
        }
        return null;
    }

    public String[] completeCapture(long timestamp, String captureFilePath, String finalFilePath, int width, int height) {
        try {
            File captureFile = new File(captureFilePath);
            File finalFile = new File(finalFilePath);

            if (captureFile.exists()) {
                FileUtils.copy(captureFile, finalFile);
                if (finalFile.exists()) {
                    captureFile.delete();
                    return new String[]{"" + timestamp, RfcxScreenShotFileUtils.CAPTURE_FILETYPE, FileUtils.sha1Hash(finalFilePath), "" + width, "" + height, finalFilePath};
                }
            }
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return null;
    }


}
