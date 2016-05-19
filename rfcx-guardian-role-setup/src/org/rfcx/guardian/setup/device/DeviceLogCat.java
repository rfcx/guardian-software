package org.rfcx.guardian.setup.device;

import java.io.File;

import org.rfcx.guardian.setup.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
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

	private boolean initializeCapture() {
		
		if ((new File(this.sdCardFilesDir)).exists()) { this.filesDir = this.sdCardFilesDir+"/"+this.processTag; }
		
		if (!(new File(this.filesDir)).exists()) { (new File(this.filesDir)).mkdirs(); FileUtils.chmod(this.filesDir, 0755); }
		if (!(new File(this.captureDir)).exists()) { (new File(this.captureDir)).mkdirs(); FileUtils.chmod(this.captureDir, 0755); }
		
		return (new File(this.binFilePath)).exists();
	}
    
	public String[] launchCapture(Context context) {
		
		if (initializeCapture()) {
			
			try {
				
				String timestamp = ""+System.currentTimeMillis();
				String captureFilePath = this.captureDir+"/"+timestamp+"."+this.fileType;
				String finalFilePath = this.filesDir+"/"+timestamp+"."+this.fileType;
				
				// run framebuffer binary to save screenshot to file
				ShellCommands.executeCommand(this.binFilePath+" "+captureFilePath, null, false, context);
				
				return null;
						
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
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
	        	FileUtils.copy(captureFile, finalFile);
	        	if (finalFile.exists()) {
	        		captureFile.delete();
	        		return new String[] { timestamp, this.fileType, FileUtils.sha1Hash(finalFilePath), finalFilePath };
	        	}
		    }
		} catch (Exception e) {
			RfcxLog.logExc(TAG, e);
		}
		return null;
	}	
    
}
