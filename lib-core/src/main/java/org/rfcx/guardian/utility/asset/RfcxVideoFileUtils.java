package org.rfcx.guardian.utility.asset;

import android.content.Context;
import android.os.Environment;

import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RfcxVideoFileUtils {

    private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM-dd/HH", Locale.US);
    private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);
    private static final String videoFileType = "mp4";
    private String logTag;
    private String appRole = "Utils";
    private String rfcxDeviceId = null;

    public RfcxVideoFileUtils(Context context, String appRole, String rfcxDeviceId) {
        this.logTag = RfcxLog.generateLogTag(appRole, "RfcxVideoUtils");
        this.appRole = appRole;
        this.rfcxDeviceId = rfcxDeviceId;
        initializeVideoDirectories(context);
    }

    private static void initializeVideoDirectories(Context context) {

        FileUtils.initializeDirectoryRecursively(videoSdCardDir(), true);
        FileUtils.initializeDirectoryRecursively(videoCaptureDir(context), false);
        FileUtils.initializeDirectoryRecursively(videoQueueDir(context), false);
    }

    private static String videoSdCardDir() {
        return Environment.getExternalStorageDirectory().toString() + "/rfcx/videos";
    }

    public static String videoQueueDir(Context context) {
        return context.getFilesDir().toString() + "/videos/queue";
    }

    public static String videoCaptureDir(Context context) {
        return context.getFilesDir().toString() + "/videos/capture";
    }

    public static String getVideoFileLocation_Capture(Context context, long timestamp) {
        return videoCaptureDir(context) + "/" + timestamp + "." + videoFileType;
    }

    public static String getVideoFileLocation_PostCapture(Context context, long timestamp) {
        return videoCaptureDir(context) + "/_" + timestamp + "." + videoFileType;
    }

    public static String getVideoFileLocation_Queue(String rfcxDeviceId, Context context, long timestamp) {
        return videoQueueDir(context) + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + videoFileType + ".gz";
    }

    public static String getVideoFileLocation_ExternalStorage(String rfcxDeviceId, long timestamp) {
        return videoSdCardDir() + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + videoFileType + ".gz";
    }


}
