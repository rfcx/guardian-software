package org.rfcx.guardian.utility.device.control;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class DeviceLogCatCapture {
	
	public DeviceLogCatCapture(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceLogCatCapture.class);
		initializeLogCatDirectories(context);
	}

	private String logTag = RfcxLog.generateLogTag("Utils", DeviceLogCatCapture.class);
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM/dd-a", Locale.US);

	public static final String SCRIPT_NAME = "logcat_capture";
	public static final String FILETYPE = "log";
	
	private static void initializeLogCatDirectories(Context context) {
		(new File(captureDir(context))).mkdirs(); FileUtils.chmod(captureDir(context), 0777);
		(new File(sdCardFilesDir())).mkdirs(); FileUtils.chmod(sdCardFilesDir(), 0777);
		(new File(finalFilesDir(context))).mkdirs(); FileUtils.chmod(finalFilesDir(context), 0777);
		(new File(getExecutableScriptDir(context))).mkdirs(); FileUtils.chmod(getExecutableScriptDir(context), 0777);
		
		// find copy/manage executable binary...
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
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/logs/scr").toString(); 
	}

	public static String getExecutableScriptFilePath(Context context) {
		return (new StringBuilder()).append(getExecutableScriptDir(context)).append("/").append(SCRIPT_NAME).toString(); 
	}
	
	public static String captureDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/logs/capture").toString(); 
	}
	
	public static String getLogFileLocation_Capture(Context context, long timestamp) {
		return (new StringBuilder()).append(captureDir(context)).append("/").append(timestamp).append(".").append(FILETYPE).toString(); 
	}

	public static String getLogFileLocation_Complete_PostZip(Context context, long timestamp) {
		return (new StringBuilder()).append(finalFilesDir(context)).append("/").append(dateFormat.format(new Date(timestamp))).append("/").append(timestamp).append(".").append(FILETYPE).append(".gz").toString(); 
	}
	
	
	public boolean saveExecutableScript(Context context) {
		
		String scriptBody = (new StringBuilder())
			.append("#!/system/bin/sh").append("\n")
			
			.append("CAPTURE_FILE=$1;").append("\n")
			.append("FINAL_FILE=$2;").append("\n")
			.append("CAPTURE_CYCLE_DURATION=$(($3 * 1));").append("\n")
			
			.append("touch $CAPTURE_FILE").append("\n")

			.append("TIMESTAMP_BEGIN=$(date +\"%s\")").append("\n")
			.append("logcat -v time > $CAPTURE_FILE&").append("\n")
			.append("PID=$!").append("\n")

			.append("sleep $CAPTURE_CYCLE_DURATION").append("\n")

			.append("KILL=`kill -9 $PID`").append("\n")
			.append("TIMESTAMP_END=$(date +\"%s\")").append("\n")
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
			FileUtils.chmod(scriptFilePath, 0755);
			return (new File(scriptFilePath)).canExecute();
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}
	
	
//	public String[] launchCapture(Context context) {
//		
//		String executableBinaryFilePath = DeviceLogCatCapture.getExecutableScriptFilePath(context);
//		
//		if ((new File(executableBinaryFilePath)).exists()) {
//			
//			try {
//				
//				long captureTimestamp = System.currentTimeMillis();
//				
//				String captureFilePath = DeviceLogCatCapture.getLogFileLocation_Capture(context, captureTimestamp);
//				String finalFilePath = DeviceLogCatCapture.getLogFileLocation_Complete(context, captureTimestamp);
//				
//				// run framebuffer binary to save screenshot to file
//				ShellCommands.executeCommand(executableBinaryFilePath+" "+captureFilePath, null, true, context);
//				
//				return completeCapture(captureTimestamp, captureFilePath, finalFilePath);
//				
//			} catch (Exception e) {
//				RfcxLog.logExc(logTag, e);
//			}
//		} else {
//			Log.e(logTag, "Executable not found: "+executableBinaryFilePath);
//		}
//		return null;
//	}
//	
//	public String[] completeCapture(long timestamp, String captureFilePath, String finalFilePath) {
//		try {
//			File captureFile = new File(captureFilePath);
//			File finalFile = new File(finalFilePath);
//			
//	        if (captureFile.exists()) {
//	        	FileUtils.copy(captureFile, finalFile);
//	        	if (finalFile.exists()) {
//	        		captureFile.delete();
//	        		return new String[] { ""+timestamp, DeviceLogCatCapture.FILETYPE, FileUtils.sha1Hash(finalFilePath), finalFilePath };
//	        	}
//		    }
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}
//		return null;
//	}
	
}
