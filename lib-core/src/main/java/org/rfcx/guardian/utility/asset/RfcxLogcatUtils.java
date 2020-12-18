
package org.rfcx.guardian.utility.asset;

import android.content.Context;
import android.os.Environment;

import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RfcxLogcatUtils {

	public RfcxLogcatUtils(Context context, String appRole, String rfcxDeviceId, String logLevel) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxLogcatUtils");
		this.appRole = appRole;
		this.rfcxDeviceId = rfcxDeviceId;
		initializeLogCatDirectories(context);
		saveLogExecutableScript(context, logLevel);
	}

	private String logTag;
	private String appRole = "Utils";
	private String rfcxDeviceId = null;
	
	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM/yyyy-MM-dd/HH", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);

	public static final String SCRIPT_NAME = "logcat_capture.sh";
	public static final String FILETYPE = "log";
	
	private static void initializeLogCatDirectories(Context context) {

		FileUtils.initializeDirectoryRecursively(logSdCardDir(), true);
		FileUtils.initializeDirectoryRecursively(logCaptureDir(context), false);
		FileUtils.initializeDirectoryRecursively(logPostCaptureDir(context), false);
		FileUtils.initializeDirectoryRecursively(logQueueDir(context), false);
		FileUtils.initializeDirectoryRecursively(getLogExecutableScriptDir(context), false);
	}
	
	private static String logSdCardDir() {
		return Environment.getExternalStorageDirectory().toString() + "/rfcx/logs";
	}
	
	private static String logQueueDir(Context context) {
		return context.getFilesDir().toString() + "/logs/queue";
	}
	
	private static String getLogExecutableScriptDir(Context context) {
		return context.getFilesDir().toString() + "/scr";
	}

	public static String getLogExecutableScriptFilePath(Context context) {
		return getLogExecutableScriptDir(context) + "/" + SCRIPT_NAME;
	}
	
	public static String logCaptureDir(Context context) {
		return context.getFilesDir().toString() + "/logs/capture";
	}
	
	public static String logPostCaptureDir(Context context) {
		return context.getFilesDir().toString() + "/logs/complete";
	}
	
	public static String getLogFileLocation_Capture(Context context, long timestamp) {
		return logCaptureDir(context) + "/" + timestamp + "." + FILETYPE;
	}
	
	public static String getLogFileLocation_PostCapture(Context context, long timestamp) {
		return logPostCaptureDir(context) + "/_" + timestamp + "." + FILETYPE;
	}

	public static String getLogFileLocation_Queue(String rfcxDeviceId, Context context, long timestamp) {
		return (DeviceStorage.isExternalStorageWritable() ? logSdCardDir() : logQueueDir(context) )
				+ "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + FILETYPE + ".gz";
	}

	public static String getLogFileLocation_ExternalStorage(String rfcxDeviceId, long timestamp) {
		return logSdCardDir() + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + FILETYPE + ".gz";
	}
	
	
	public boolean saveLogExecutableScript(Context context, String logLevel) {
		
		String scriptBody = (new StringBuilder())
			.append("#!/system/bin/sh").append("\n")
			
			.append("CAPTURE_FILE=$1;").append("\n")
			.append("FINAL_FILE=$2;").append("\n")
			.append("CAPTURE_CYCLE_DURATION=$(($3 * 1));").append("\n")
			
			.append("touch $CAPTURE_FILE").append("\n")

		//	.append("TIMESTAMP_BEGIN=$(date +\"%s\")").append("\n")
			.append("logcat -v time *:").append(logLevel.substring(0,1).toUpperCase(Locale.US)).append(" > $CAPTURE_FILE&").append("\n")
			.append("PID=$!").append("\n")

			.append("sleep $CAPTURE_CYCLE_DURATION").append("\n")

			.append("KILL=`kill -9 $PID`").append("\n")
		//	.append("TIMESTAMP_END=$(date +\"%s\")").append("\n")
			.append("logcat -c").append("\n")
			
			.append("chmod a+rw $CAPTURE_FILE").append("\n")
			.append("cp $CAPTURE_FILE $FINAL_FILE").append("\n")
			.append("chmod a+rw $FINAL_FILE").append("\n")
			.append("rm $CAPTURE_FILE").append("\n")

			.toString();
		
		try {
			String scriptFilePath = getLogExecutableScriptFilePath(context);
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(scriptFilePath));
			bufferedWriter.write(scriptBody);
			bufferedWriter.close();
			FileUtils.chmod(scriptFilePath,  "rwx", "rx");
			return (new File(scriptFilePath)).canExecute();
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}
	
	
}
