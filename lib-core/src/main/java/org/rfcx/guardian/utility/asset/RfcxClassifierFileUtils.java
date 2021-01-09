package org.rfcx.guardian.utility.asset;

import android.content.Context;
import android.os.Environment;

import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RfcxClassifierFileUtils {

	public RfcxClassifierFileUtils(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxClassifierFileUtils");
		this.appRole = appRole;
		initializeClassifierDirectories(context);
	}

	private String logTag;
	private String appRole = "Utils";
	
	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	private static final String classifierFileType = "tflite";
	
	public static void initializeClassifierDirectories(Context context) {

		FileUtils.initializeDirectoryRecursively(classifierSdCardDir(), true);
		FileUtils.initializeDirectoryRecursively(classifierDownloadDir(context), false);
		FileUtils.initializeDirectoryRecursively(classifierCacheDir(context), false);
		FileUtils.initializeDirectoryRecursively(classifierActiveDir(context), false);
	}
	
	private static String classifierSdCardDir() {
		return Environment.getExternalStorageDirectory().toString() + "/rfcx/classifiers";
	}
	
	public static String classifierDownloadDir(Context context) {
		return context.getFilesDir().toString() + "/classifiers/download";
	}

	public static String classifierCacheDir(Context context) {
		return context.getFilesDir().toString() + "/classifiers/cache";
	}

	public static String classifierActiveDir(Context context) {
		return context.getFilesDir().toString() + "/classifiers/active";
	}

	public static String getClassifierFileName(String classificationTag, String versionTag, long timestamp) {
		return 	classificationTag
				+ "_" + "v" + versionTag
				+ "_" + timestamp
				+ "." + classifierFileType;
	}

	public static String getClassifierFileLocation_Download(Context context, long timestamp) {
		return classifierDownloadDir(context) + "/_" + timestamp + "." + classifierFileType;
	}

	public static String getClassifierFileLocation_Cache(Context context, long timestamp, String classificationTag, String versionTag) {
		return classifierCacheDir(context) + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + getClassifierFileName(classificationTag, versionTag, timestamp);
	}

	public static String getClassifierFileLocation_Active(Context context, long timestamp, String classificationTag, String versionTag) {
		return classifierActiveDir(context) + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + getClassifierFileName(classificationTag, versionTag, timestamp);
	}

	public static String getClassifierFileLocation_ExternalStorage(long timestamp, String classificationTag, String versionTag) {
		return classifierSdCardDir() + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + getClassifierFileName(classificationTag, versionTag, timestamp);
	}




	
}
