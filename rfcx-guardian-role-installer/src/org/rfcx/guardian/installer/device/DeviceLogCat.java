package org.rfcx.guardian.installer.device;

import java.io.File;

import org.rfcx.guardian.installer.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxConstants;
import org.rfcx.guardian.utility.ShellCommands;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class DeviceLogCat {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceLogCat.class.getSimpleName();

	private String processTag = "logcat";
	private String binName = "logcat_capture";
	private String fileType = "log";
	
	private String sdCardFilesDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/rfcx";
	private String nonSdCardFilesDir = Environment.getDownloadCacheDirectory().getAbsolutePath()+"/rfcx";
	
	private String filesDir = nonSdCardFilesDir+"/"+processTag;
	private String captureDir = nonSdCardFilesDir+"/capture/"+processTag;
	private String binFilePath = nonSdCardFilesDir+"/bin/"+binName;

	private boolean isInitialized = false;
	private FileUtils fileUtils = new FileUtils();

	private boolean initializeCapture(Context context) {
		if (!this.isInitialized) {
			if ((new File(this.sdCardFilesDir)).exists()) { this.filesDir = this.sdCardFilesDir+"/logcat"; }
			(new File(this.filesDir)).mkdirs(); fileUtils.chmod(this.filesDir, 0755);
			(new File(this.captureDir)).mkdirs(); fileUtils.chmod(this.captureDir, 0755);
			this.isInitialized = true;
		}
		return (new File(this.binFilePath)).exists();
	}
    
	public String[] launchCapture(Context context) {
		
		if (initializeCapture(context)) {
			
			try {
				
				String timestamp = ""+System.currentTimeMillis();
				String captureFilePath = this.captureDir+"/"+timestamp+"."+this.fileType;
				String finalFilePath = this.filesDir+"/"+timestamp+"."+this.fileType;
				
				// run framebuffer binary to save screenshot to file
				(new ShellCommands()).executeCommand(this.binFilePath+" "+captureFilePath, null, false, context);
				
				return null;
						
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		} else {
			Log.e(TAG, "Executable not found: "+this.binFilePath);
		}
		return null;
	}
	
	public String[] completeCapture(String timestamp, String captureFilePath, String finalFilePath) {
		try {
			File captureFile = new File(captureFilePath);
			File finalFile = new File(finalFilePath);
			
	        if (captureFile.exists()) {
	        	fileUtils.copy(captureFile, finalFile);
	        	if (finalFile.exists()) {
	        		captureFile.delete();
	        		return new String[] { timestamp, this.fileType, fileUtils.sha1Hash(finalFilePath), finalFilePath };
	        	}
		    }
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return null;
	}	
    
}
