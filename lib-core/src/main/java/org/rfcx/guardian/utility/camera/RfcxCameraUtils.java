package org.rfcx.guardian.utility.camera;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;

import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class RfcxCameraUtils {
	
	public RfcxCameraUtils(Context context, String appRole, String rfcxDeviceId) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxCameraUtils");
		this.appRole = appRole;
		this.rfcxDeviceId = rfcxDeviceId;
		initializeCameraCaptureDirectories(context);
	}

	private String logTag;
	private String appRole = "Utils";
	private String rfcxDeviceId = null;
	
	private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM/yyyy-MM-dd/HH", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);

	private static final String photoFileType = "jpg";
	private static final String videoFileType = "mp4";
	
	private static void initializeCameraCaptureDirectories(Context context) {

		FileUtils.initializeDirectoryRecursively(photoSdCardDir(), true);
		FileUtils.initializeDirectoryRecursively(videoSdCardDir(), true);

		FileUtils.initializeDirectoryRecursively(photoCaptureDir(context), false);
		FileUtils.initializeDirectoryRecursively(videoCaptureDir(context), false);

		FileUtils.initializeDirectoryRecursively(photoFinalDir(context), false);
		FileUtils.initializeDirectoryRecursively(videoFinalDir(context), false);
	}
	
	private static String photoSdCardDir() {
		return Environment.getExternalStorageDirectory().toString() + "/rfcx/photos";
	}
	
	private static String photoFinalDir(Context context) {
		return context.getFilesDir().toString() + "/photos/final";
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
		
	public static String getPhotoFileLocation_Complete_PostGZip(String rfcxDeviceId, Context context, long timestamp) {
		return (DeviceStorage.isExternalStorageWritable() ? photoSdCardDir() : photoFinalDir(context) )
				+ "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + photoFileType + ".gz";
	}

	public static String getPhotoFileLocation_ExternalStorage(String rfcxDeviceId, long timestamp) {
		return photoSdCardDir() + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + photoFileType + ".gz";
	}


	
	
	private static String videoSdCardDir() {
		return Environment.getExternalStorageDirectory().toString() + "/rfcx/videos";
	}
	
	private static String videoFinalDir(Context context) {
		return context.getFilesDir().toString() + "/videos/final";
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
		
	public static String getVideoFileLocation_Complete_PostGZip(String rfcxDeviceId, Context context, long timestamp) {
		return (DeviceStorage.isExternalStorageWritable() ? videoSdCardDir() : videoFinalDir(context) )
				+"/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + videoFileType + ".gz";
	}

	public static String getVideoFileLocation_ExternalStorage(String rfcxDeviceId, long timestamp) {
		return videoSdCardDir() + "/" + dirDateFormat.format(new Date(timestamp)) + "/" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(timestamp)) + "." + videoFileType + ".gz";
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
