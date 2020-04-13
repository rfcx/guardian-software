package org.rfcx.guardian.utility.camera;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
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

	private static final String photoDirName = "photos";
	private static final String videoDirName = "videos";
	
	private static void initializeCameraCaptureDirectories(Context context) {
		
		(new File(photoCaptureDir(context))).mkdirs(); FileUtils.chmod(photoCaptureDir(context),  "rw", "rw");
		(new File(photoSdCardFilesDir())).mkdirs(); FileUtils.chmod(photoSdCardFilesDir(),  "rw", "rw");
		(new File(photoFinalFilesDir(context))).mkdirs(); FileUtils.chmod(photoFinalFilesDir(context),  "rw", "rw");

		(new File(videoCaptureDir(context))).mkdirs(); FileUtils.chmod(videoCaptureDir(context),  "rw", "rw");
		(new File(videoSdCardFilesDir())).mkdirs(); FileUtils.chmod(videoSdCardFilesDir(),  "rw", "rw");
		(new File(videoFinalFilesDir(context))).mkdirs(); FileUtils.chmod(videoFinalFilesDir(context),  "rw", "rw");
	}
	
	private static String photoSdCardFilesDir() {
		return (new StringBuilder()).append(Environment.getExternalStorageDirectory().toString()).append("/rfcx/").append(photoDirName).toString(); 
	}
	
	private static String photoFinalFilesDir(Context context) {
		if ((new File(photoSdCardFilesDir())).isDirectory()) {
			return photoSdCardFilesDir();
		} else {
			return (new StringBuilder()).append(context.getFilesDir().toString()).append("/").append(photoDirName).append("/final").toString();
		}
	}
	
	public static String photoCaptureDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/").append(photoDirName).append("/capture").toString(); 
	}
	
	public static String getPhotoFileLocation_Capture(Context context, long timestamp) {
		return (new StringBuilder()).append(photoCaptureDir(context)).append("/").append(timestamp).append(".").append(photoFileType).toString(); 
	}

	public static String getPhotoFileLocation_PostCapture(Context context, long timestamp) {
		return (new StringBuilder()).append(photoCaptureDir(context)).append("/_").append(timestamp).append(".").append(photoFileType).toString(); 
	}
		
	public static String getPhotoFileLocation_Complete_PostGZip(String rfcxDeviceId, Context context, long timestamp) {
		return (new StringBuilder()).append(photoFinalFilesDir(context)).append("/").append(dirDateFormat.format(new Date(timestamp))).append("/").append(rfcxDeviceId).append("_").append(fileDateTimeFormat.format(new Date(timestamp))).append(".").append(photoFileType).append(".gz").toString(); 
	}


	
	
	private static String videoSdCardFilesDir() {
		return (new StringBuilder()).append(Environment.getExternalStorageDirectory().toString()).append("/rfcx/").append(videoDirName).toString(); 
	}
	
	private static String videoFinalFilesDir(Context context) {
		if ((new File(videoSdCardFilesDir())).isDirectory()) {
			return videoSdCardFilesDir();
		} else {
			return (new StringBuilder()).append(context.getFilesDir().toString()).append("/").append(videoDirName).append("/final").toString();
		}
	}
	
	public static String videoCaptureDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/").append(videoDirName).append("/capture").toString(); 
	}
	
	public static String getVideoFileLocation_Capture(Context context, long timestamp) {
		return (new StringBuilder()).append(videoCaptureDir(context)).append("/").append(timestamp).append(".").append(videoFileType).toString(); 
	}

	public static String getVideoFileLocation_PostCapture(Context context, long timestamp) {
		return (new StringBuilder()).append(videoCaptureDir(context)).append("/_").append(timestamp).append(".").append(videoFileType).toString(); 
	}
		
	public static String getVideoFileLocation_Complete_PostGZip(String rfcxDeviceId, Context context, long timestamp) {
		return (new StringBuilder()).append(videoFinalFilesDir(context)).append("/").append(dirDateFormat.format(new Date(timestamp))).append("/").append(rfcxDeviceId).append("_").append(fileDateTimeFormat.format(new Date(timestamp))).append(".").append(videoFileType).append(".gz").toString(); 
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
