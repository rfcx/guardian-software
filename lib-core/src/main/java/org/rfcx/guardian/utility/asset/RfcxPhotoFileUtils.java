package org.rfcx.guardian.utility.asset;

import android.content.Context;
import android.os.Environment;

import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RfcxPhotoFileUtils {

	public RfcxPhotoFileUtils(Context context, String appRole, String rfcxDeviceId) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxPhotoUtils");
		this.appRole = appRole;
		this.rfcxDeviceId = rfcxDeviceId;
		initializePhotoDirectories(context);
	}

	private String logTag;
	private String appRole = "Utils";
	private String rfcxDeviceId = null;
	
	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM/yyyy-MM-dd/HH", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);

	private static final String photoFileType = "jpg";
	
	private static void initializePhotoDirectories(Context context) {

		FileUtils.initializeDirectoryRecursively(photoSdCardDir(), true);
		FileUtils.initializeDirectoryRecursively(photoCaptureDir(context), false);
		FileUtils.initializeDirectoryRecursively(photoQueueDir(context), false);
	}
	
	private static String photoSdCardDir() {
		return Environment.getExternalStorageDirectory().toString() + "/rfcx/photos";
	}
	
	public static String photoQueueDir(Context context) {
		return context.getFilesDir().toString() + "/photos/queue";
	}
	
	public static String photoCaptureDir(Context context) {
		return context.getFilesDir().toString() + "/photos/capture";
	}
	
	public static String getPhotoFileLocation_Capture(Context context, long timestamp) {
		return photoCaptureDir(context) + "/" + timestamp + "." + photoFileType;
	}

	public static String getPhotoFileLocation_PostCapture(Context context, long timestamp) {
		return photoCaptureDir(context) + "/_" + timestamp + "." + photoFileType;
	}
		
	public static String getPhotoFileLocation_Queue(String rfcxDeviceId, Context context, long timestamp) {
		return (DeviceStorage.isExternalStorageWritable() ? photoSdCardDir() : photoQueueDir(context) )
				+ "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + photoFileType + ".gz";
	}

	public static String getPhotoFileLocation_ExternalStorage(String rfcxDeviceId, long timestamp) {
		return photoSdCardDir() + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + photoFileType + ".gz";
	}



	
	
	
	public String[] completePhotoCapture(long timestamp, String captureFilePath, String finalFilePath) {
		try {
			File captureFile = new File(captureFilePath);
			File finalFile = new File(finalFilePath);
			
	        if (captureFile.exists()) {
	        	FileUtils.copy(captureFile, finalFile);
	        	if (finalFile.exists()) {
	        		captureFile.delete();
	        		return new String[] { ""+timestamp, photoFileType, FileUtils.sha1Hash(finalFilePath), finalFilePath };
	        	}
		    }
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	

	
}
