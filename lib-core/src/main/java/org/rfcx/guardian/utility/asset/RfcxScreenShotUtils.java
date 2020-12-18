package org.rfcx.guardian.utility.asset;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RfcxScreenShotUtils {

	public RfcxScreenShotUtils(Context context, String appRole, String rfcxDeviceId) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxScreenShotUtils");
		this.appRole = appRole;
		this.rfcxDeviceId = rfcxDeviceId;
		initializeScreenShotDirectories(context);
	}

	private String logTag;
	private String appRole = "Utils";
	private String rfcxDeviceId = null;
	
	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM/yyyy-MM-dd/HH", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);

	public static final String CAPTURE_FILETYPE = "png";
	
	public static void initializeScreenShotDirectories(Context context) {

		FileUtils.initializeDirectoryRecursively(screenShotSdCardDir(), true);
		FileUtils.initializeDirectoryRecursively(screenShotCaptureDir(context), false);
		FileUtils.initializeDirectoryRecursively(screenShotQueueDir(context), false);
	}
	
	private static String screenShotSdCardDir() {
		return Environment.getExternalStorageDirectory().toString() + "/rfcx/screenshots";
	}
	
	private static String screenShotQueueDir(Context context) {
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
		return (DeviceStorage.isExternalStorageWritable() ? screenShotSdCardDir() : screenShotQueueDir(context) )
				+ "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + CAPTURE_FILETYPE;
	}

	public static String getScreenShotFileLocation_ExternalStorage(String rfcxDeviceId, long timestamp) {
		return screenShotSdCardDir() + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + CAPTURE_FILETYPE;
	}




	public String[] launchCapture(Context context) {

		String executableBinaryFilePath = RfcxScreenShotUtils.getScreenShotExecutableBinaryFilePath(context);

		if ((new File(executableBinaryFilePath)).exists()) {

			try {

				long captureTimestamp = System.currentTimeMillis();

				String captureFilePath = RfcxScreenShotUtils.getScreenShotFileLocation_Capture(context, captureTimestamp);
				String finalFilePath = RfcxScreenShotUtils.getScreenShotFileLocation_Queue(this.rfcxDeviceId, context, captureTimestamp);

				Process shellProcess = Runtime.getRuntime().exec(new String[] { executableBinaryFilePath, captureFilePath });
				shellProcess.waitFor();
				FileUtils.chmod(captureFilePath,  "rw", "rw");

				return completeCapture(captureTimestamp, captureFilePath, finalFilePath, context.getResources().getDisplayMetrics().widthPixels, context.getResources().getDisplayMetrics().heightPixels );

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		} else {
			Log.e(logTag, "Executable not found: "+executableBinaryFilePath);
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
					return new String[] { ""+timestamp, RfcxScreenShotUtils.CAPTURE_FILETYPE, FileUtils.sha1Hash(finalFilePath), ""+width, ""+height, finalFilePath };
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}



}
