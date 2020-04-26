
package org.rfcx.guardian.utility.device.capture;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceLogCat {
	
	public DeviceLogCat(Context context, String appRole, String rfcxDeviceId, String logLevel) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceLogCat");
		this.appRole = appRole;
		this.rfcxDeviceId = rfcxDeviceId;
		initializeLogCatDirectories(context);
		saveExecutableScript(context, logLevel);
	}

	private String logTag;
	private String appRole = "Utils";
	private String rfcxDeviceId = null;
	
	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM/yyyy-MM-dd/HH", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);

	public static final String SCRIPT_NAME = "logcat_capture.sh";
	public static final String FILETYPE = "log";
	
	private static void initializeLogCatDirectories(Context context) {
		(new File(captureDir(context))).mkdirs(); FileUtils.chmod(captureDir(context),  "rw", "rw");
		(new File(postCaptureDir(context))).mkdirs(); FileUtils.chmod(postCaptureDir(context),  "rw", "rw");
		(new File(sdCardFilesDir())).mkdirs(); FileUtils.chmod(sdCardFilesDir(),  "rw", "rw");
		(new File(finalFilesDir(context))).mkdirs(); FileUtils.chmod(finalFilesDir(context),  "rw", "rw");
		(new File(getExecutableScriptDir(context))).mkdirs(); FileUtils.chmod(getExecutableScriptDir(context),  "rw", "rw");
	}
	
	private static String sdCardFilesDir() {
		return (new StringBuilder()).append(Environment.getExternalStorageDirectory().toString()).append("/rfcx/logs").toString(); 
	}
	
	private static String finalFilesDir(Context context) {
		if ((new File(sdCardFilesDir())).isDirectory()) {
			return sdCardFilesDir();
		} else {
			return (new StringBuilder()).append(context.getFilesDir().toString()).append("/logs/final").toString();
		}
	}
	
	private static String getExecutableScriptDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/scr").toString();
	}

	public static String getExecutableScriptFilePath(Context context) {
		return (new StringBuilder()).append(getExecutableScriptDir(context)).append("/").append(SCRIPT_NAME).toString(); 
	}
	
	public static String captureDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/logs/capture").toString(); 
	}
	
	public static String postCaptureDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/logs/complete").toString(); 
	}
	
	public static String getLogFileLocation_Capture(Context context, long timestamp) {
		return (new StringBuilder()).append(captureDir(context)).append("/").append(timestamp).append(".").append(FILETYPE).toString(); 
	}
	
	public static String getLogFileLocation_PostCapture(Context context, long timestamp) {
		return (new StringBuilder()).append(postCaptureDir(context)).append("/_").append(timestamp).append(".").append(FILETYPE).toString(); 
	}

	public static String getLogFileLocation_Complete_PostZip(String rfcxDeviceId, Context context, long timestamp) {
		return (new StringBuilder()).append(finalFilesDir(context)).append("/").append(dirDateFormat.format(new Date(timestamp))).append("/").append(rfcxDeviceId).append("_").append(fileDateTimeFormat.format(new Date(timestamp))).append(".").append(FILETYPE).append(".gz").toString(); 
	}
	
	
	public boolean saveExecutableScript(Context context, String logLevel) {
		
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
			String scriptFilePath = getExecutableScriptFilePath(context);
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
