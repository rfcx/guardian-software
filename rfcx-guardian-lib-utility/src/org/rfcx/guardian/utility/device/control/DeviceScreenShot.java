package org.rfcx.guardian.utility.device.control;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class DeviceScreenShot {
	
	public DeviceScreenShot(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceScreenShot.class);
		initializeScreenShotDirectories(context);
	}

	private String logTag = RfcxLog.generateLogTag("Utils", DeviceScreenShot.class);
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM/dd-a", Locale.US);

	public static final String BINARY_NAME = "fb2png";
	public static final String FILETYPE = "png";
	
	private static void initializeScreenShotDirectories(Context context) {
		(new File(captureDir(context))).mkdirs(); FileUtils.chmod(captureDir(context), 0777);
		(new File(sdCardFilesDir())).mkdirs(); FileUtils.chmod(sdCardFilesDir(), 0777);
		(new File(finalFilesDir(context))).mkdirs(); FileUtils.chmod(finalFilesDir(context), 0777);
		(new File(getExecutableBinaryDir(context))).mkdirs(); FileUtils.chmod(getExecutableBinaryDir(context), 0777);
		
		// find copy/manage executable binary...
	}
	
	private static String sdCardFilesDir() {
		return (new StringBuilder()).append(Environment.getExternalStorageDirectory().toString()).append("/rfcx/screenshot").toString(); 
	}
	
	private static String finalFilesDir(Context context) {
		if ((new File(sdCardFilesDir())).isDirectory()) {
			return sdCardFilesDir();
		} else {
			return (new StringBuilder()).append(context.getFilesDir().toString()).append("/screenshot/final").toString();
		}
	}
	
	private static String getExecutableBinaryDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/screenshot/bin").toString(); 
	}

	public static String getExecutableBinaryFilePath(Context context) {
		return (new StringBuilder()).append(getExecutableBinaryDir(context)).append("/").append(BINARY_NAME).toString(); 
	}
	
	public static String captureDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/screenshot/capture").toString(); 
	}
	
	public static String getScreenShotFileLocation_Capture(Context context, long timestamp) {
		return (new StringBuilder()).append(captureDir(context)).append("/").append(timestamp).append(".png").toString(); 
	}

	public static String getScreenShotFileLocation_Complete(Context context, long timestamp) {
		return (new StringBuilder()).append(finalFilesDir(context)).append("/").append(dateFormat.format(new Date(timestamp))).append("/").append(timestamp).append(".png").toString(); 
	}
	
	
	
	public String[] launchCapture(Context context) {
		
		String executableBinaryFilePath = DeviceScreenShot.getExecutableBinaryFilePath(context);
		
		if ((new File(executableBinaryFilePath)).exists()) {
			
			try {
				
				long captureTimestamp = System.currentTimeMillis();
				
				String captureFilePath = DeviceScreenShot.getScreenShotFileLocation_Capture(context, captureTimestamp);
				String finalFilePath = DeviceScreenShot.getScreenShotFileLocation_Complete(context, captureTimestamp);
				
				// run framebuffer binary to save screenshot to file
				ShellCommands.executeCommand(executableBinaryFilePath+" "+captureFilePath, null, true, context);
				
				return completeCapture(captureTimestamp, captureFilePath, finalFilePath);
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		} else {
			Log.e(logTag, "Executable not found: "+executableBinaryFilePath);
		}
		return null;
	}
	
	public String[] completeCapture(long timestamp, String captureFilePath, String finalFilePath) {
		try {
			File captureFile = new File(captureFilePath);
			File finalFile = new File(finalFilePath);
			
	        if (captureFile.exists()) {
	        	FileUtils.copy(captureFile, finalFile);
	        	if (finalFile.exists()) {
	        		captureFile.delete();
	        		return new String[] { ""+timestamp, DeviceScreenShot.FILETYPE, FileUtils.sha1Hash(finalFilePath), finalFilePath };
	        	}
		    }
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
}
