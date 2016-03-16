package org.rfcx.guardian.system.device;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.ShellCommands;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class DeviceLogcat {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+DeviceLogcat.class.getSimpleName();
	
	private RfcxGuardian app = null;
	private String filesDir = null;
	private String sdCardFilesDir = Environment.getExternalStorageDirectory().toString()+"/rfcx";
	private String binDir = null;
	private String logDir = null;

	public void setupLogcatCapture(Context context) {
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (filesDir == null) filesDir = app.getFilesDir().getAbsolutePath();

		if (binDir == null) binDir = app.getFilesDir().getAbsolutePath()+"/bin";
		(new File(binDir)).mkdirs();
		
		logDir = filesDir+"/log";
		if ((new File(sdCardFilesDir)).isDirectory()) { logDir = sdCardFilesDir+"/log"; }
		(new File(logDir)).mkdirs();
	}
    
	public String launchLogcatCapture(Context context) {
		setupLogcatCapture(context);
		if (findOrCreateBin()) {
			
//			if ((new File("adsfasdf")).lastModified()) {
//			try {
//				(new ShellCommands()).executeCommand(binDir+"/logcat_capture ", null, false, context);
//		        if ((new File("adsfasdf")).exists()) {
//		        	app.screenShotDb.dbCaptured.insert(timestamp, "png", (new FileUtils()).sha1Hash(logPath));
//		        	// GZipping PNGs doesn't really do anything, so we just leave it as PNG
//		        	return timestamp;
//			    }
//			} catch (Exception e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//			}
		} else {
			Log.e(TAG, "Failed to find and/or install logcat_capture. Cannot launch logcat capture.");
		}
		return null;
	}
	
	private boolean findOrCreateBin() {
    	try {
	     	File binFile = new File(binDir+"/logcat_capture");
	     	
	        if (!binFile.exists()) {
	    		try {
	    			InputStream inputStream = app.getAssets().open("logcat_capture");
	    		    OutputStream outputStream = new FileOutputStream(binDir+"/logcat_capture");
	    		    byte[] buf = new byte[1024];
	    		    int len;
	    		    while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
	    		    inputStream.close();
	    		    outputStream.close();
	    		    (new FileUtils()).chmod(binFile, 0755);
	    		    return binFile.exists();
	    		} catch (IOException e) {
	    			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
	    			return false;
	    		}
	        } else {
	        	return true;
	        }
    	} catch (Exception e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
    		return false;
    	}
    }
	
    public String getLogDirectory(Context context) {
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (filesDir == null) filesDir = app.getFilesDir().getAbsolutePath();
		logDir = filesDir+"/img";
		if ((new File(sdCardFilesDir)).isDirectory()) { logDir = sdCardFilesDir+"/img"; }
		return logDir;
    }
}
