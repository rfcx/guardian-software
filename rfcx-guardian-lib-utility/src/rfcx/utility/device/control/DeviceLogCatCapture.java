
package rfcx.utility.device.control;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceLogCatCapture {
	
	public DeviceLogCatCapture(Context context, String appRole, String rfcxDeviceId) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceLogCatCapture.class);
		this.appRole = appRole;
		this.rfcxDeviceId = rfcxDeviceId;
		initializeLogCatDirectories(context);
//		reSetLogCatCaptureScript(context);
		saveExecutableScript(context);
	}

	private String logTag = RfcxLog.generateLogTag("Utils", DeviceLogCatCapture.class);
	private String appRole = "Utils";
	private String rfcxDeviceId = null;
	
	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM/yyyy-MM-dd/HH", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);

	public static final String SCRIPT_NAME = "logcat_capture.sh";
	public static final String FILETYPE = "log";
	
	private static void initializeLogCatDirectories(Context context) {
		(new File(captureDir(context))).mkdirs(); FileUtils.chmod(captureDir(context), 0777);
		(new File(postCaptureDir(context))).mkdirs(); FileUtils.chmod(postCaptureDir(context), 0777);
		(new File(sdCardFilesDir())).mkdirs(); FileUtils.chmod(sdCardFilesDir(), 0777);
		(new File(finalFilesDir(context))).mkdirs(); FileUtils.chmod(finalFilesDir(context), 0777);
		(new File(getExecutableScriptDir(context))).mkdirs(); FileUtils.chmod(getExecutableScriptDir(context), 0777);
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
	
	
	
	
	
	
	
	
//	private void reSetLogCatCaptureScript(Context context) {
//		
//		String logCatCaptureScriptFilePath = DeviceLogCatCapture.getExecutableScriptFilePath(context);
//		
//		if ((new File(logCatCaptureScriptFilePath)).exists()) { (new File(logCatCaptureScriptFilePath)).delete(); }
//
//		try {
//			InputStream inputStream = context.getAssets().open("logcat_capture.sh");
//			OutputStream outputStream = new FileOutputStream(logCatCaptureScriptFilePath);
//			byte[] buf = new byte[1024]; int len; while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
//			inputStream.close(); outputStream.close();
//			FileUtils.chmod(logCatCaptureScriptFilePath, 0755);
//		} catch (IOException e) {
//			RfcxLog.logExc(logTag, e);
//		}
//		
//	}
	
	
	
	
	
	
	
	
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
