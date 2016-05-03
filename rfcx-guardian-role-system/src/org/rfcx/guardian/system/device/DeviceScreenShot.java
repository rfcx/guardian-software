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

public class DeviceScreenShot {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceScreenShot.class.getSimpleName();
	
	private RfcxGuardian app = null;

	private String sdCardFilesDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/rfcx";
	private String nonSdCardFilesDir = Environment.getDownloadCacheDirectory().getAbsolutePath()+"/rfcx";
	
	private String binDir = null;
	private String imgDir = null;

	public void setupScreenShot(Context context) {
		
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();

		if (binDir == null) binDir = app.getFilesDir().getAbsolutePath()+"/bin";
		(new File(binDir)).mkdirs();
		
		imgDir = nonSdCardFilesDir+"/img";
		if ((new File(sdCardFilesDir)).exists()) { imgDir = sdCardFilesDir+"/img"; }
		(new File(imgDir)).mkdirs();
	}
    
	public String saveScreenShot(Context context) {
		setupScreenShot(context);
		if (findOrCreateBin()) {
			try {
				
				String timestamp = ""+System.currentTimeMillis();
				String imgFilePath = imgDir+"/"+timestamp+".png";
				
				// run framebuffer binary to save screenshot to file
				(new ShellCommands()).executeCommand(binDir+"/fb2png "+imgFilePath, null, false, context);
				
		        if ((new File(imgFilePath)).exists()) {
		        	app.screenShotDb.dbCaptured.insert(timestamp, "png", (new FileUtils()).sha1Hash(imgFilePath), imgFilePath);
		        	// PNG images are already GZipped
		        	return timestamp;
			    }
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		} else {
			Log.e(TAG, "Failed to find and/or install fb2png. Cannot produce screenshot");
		}
		return null;
	}
	
    private boolean findOrCreateBin() {
    	try {
	     	File binFile = new File(binDir+"/fb2png");
	     	
	        if (!binFile.exists()) {
	    		try {
	    			InputStream inputStream = app.getAssets().open("fb2png");
	    		    OutputStream outputStream = new FileOutputStream(binDir+"/fb2png");
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
	
//    public String getScreenShotDirectory(Context context) {
//		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
//		if (filesDir == null) filesDir = app.getFilesDir().getAbsolutePath();
//		imgDir = filesDir+"/img";
//		if ((new File(sdCardFilesDir)).isDirectory()) { imgDir = sdCardFilesDir+"/img"; }
//		return imgDir;
//    }
}
