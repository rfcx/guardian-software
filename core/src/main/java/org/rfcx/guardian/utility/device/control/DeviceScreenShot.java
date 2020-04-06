package org.rfcx.guardian.utility.device.control;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceScreenShot {
	
	public DeviceScreenShot(Context context, String appRole, String rfcxDeviceId) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceScreenShot.class);
		this.appRole = appRole;
		this.rfcxDeviceId = rfcxDeviceId;
		initializeScreenShotDirectories(context);
		checkSetScreenShotBinary(context);
	}

	private String logTag = RfcxLog.generateLogTag("Utils", DeviceScreenShot.class);
	private String appRole = "Utils";
	private String rfcxDeviceId = null;
	
	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM/yyyy-MM-dd/HH", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);

	public static final String FILETYPE = "png";

//	We are disabling fb2png in favor of the system-built screencap binary
//	public static final String BINARY_NAME = "fb2png";
	
	private static void initializeScreenShotDirectories(Context context) {
		(new File(captureDir(context))).mkdirs(); FileUtils.chmod(captureDir(context),  "rw", "rw");
		(new File(sdCardFilesDir())).mkdirs(); FileUtils.chmod(sdCardFilesDir(),  "rw", "rw");
		(new File(finalFilesDir(context))).mkdirs(); FileUtils.chmod(finalFilesDir(context),  "rw", "rw");
		(new File(getExecutableBinaryDir(context))).mkdirs(); FileUtils.chmod(getExecutableBinaryDir(context),  "rw", "rw");
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
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/bin").toString();
	}

	public static String getExecutableBinaryFilePath(Context context) {
	//	We are disabling fb2png in favor of the system-built screencap binary
	//	String binPath = (new StringBuilder()).append(getExecutableBinaryDir(context)).append("/").append(BINARY_NAME).toString();
		String binPath = "/system/bin/screencap";
		return binPath;
	}
	
	public static String captureDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/screenshot/capture").toString();
	}
	
	public static String getScreenShotFileLocation_Capture(Context context, long timestamp) {
		return (new StringBuilder()).append(captureDir(context)).append("/").append(timestamp).append(".png").toString(); 
	}

	public static String getScreenShotFileLocation_Complete(String rfcxDeviceId, Context context, long timestamp) {
		return (new StringBuilder()).append(finalFilesDir(context)).append("/").append(dirDateFormat.format(new Date(timestamp))).append("/").append(rfcxDeviceId).append("_").append(fileDateTimeFormat.format(new Date(timestamp))).append(".png").toString(); 
	}
	
	
	
	public String[] launchCapture(Context context) {
		
		String executableBinaryFilePath = DeviceScreenShot.getExecutableBinaryFilePath(context);
		
		if ((new File(executableBinaryFilePath)).exists()) {
			
			try {
				
				long captureTimestamp = System.currentTimeMillis();
				
				String captureFilePath = DeviceScreenShot.getScreenShotFileLocation_Capture(context, captureTimestamp);
				String finalFilePath = DeviceScreenShot.getScreenShotFileLocation_Complete(this.rfcxDeviceId, context, captureTimestamp);

				Process shellProcess = Runtime.getRuntime().exec(new String[] { executableBinaryFilePath, captureFilePath });
				shellProcess.waitFor();
                FileUtils.chmod(captureFilePath,  "rw", "rw");

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
	
	private void checkSetScreenShotBinary(Context context) {

		(new File(getExecutableBinaryDir(context))).mkdirs();
		FileUtils.chmod(getExecutableBinaryDir(context),  "rwx", "rwx");

		//	We are disabling fb2png in favor of the system-built screencap binary

//		String executableBinaryFilePath = DeviceScreenShot.getExecutableBinaryFilePath(context);
//		if (!(new File(executableBinaryFilePath)).exists()) {
//			try {
//				InputStream inputStream = context.getAssets().open("fb2png");
//				OutputStream outputStream = new FileOutputStream(executableBinaryFilePath);
//				byte[] buf = new byte[1024]; int len; while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
//				inputStream.close(); outputStream.close();
//				FileUtils.chmod(executableBinaryFilePath,  "rwx", "rx");
//			} catch (IOException e) {
//				RfcxLog.logExc(logTag, e);
//			}
//		}
	}
	
}
