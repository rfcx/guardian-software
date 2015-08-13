package org.rfcx.guardian.system.device;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.GZipUtils;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.ShellCommands;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class DeviceScreenShot {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+DeviceScreenShot.class.getSimpleName();
	
	private RfcxGuardian app = null;
	private String filesDir = null;
	private String sdCardFilesDir = Environment.getExternalStorageDirectory().toString()+"/rfcx";
	private String binDir = null;
	private String imgDir = null;

	public void setupScreenShot(Context context) {
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (filesDir == null) filesDir = app.getFilesDir().getAbsolutePath();

		if (binDir == null) binDir = app.getFilesDir().getAbsolutePath()+"/bin";
		(new File(binDir)).mkdirs();
		
		imgDir = filesDir+"/img";
		if ((new File(sdCardFilesDir)).isDirectory()) { imgDir = sdCardFilesDir+"/img"; }
		(new File(imgDir)).mkdirs();
	}
    
	public String saveScreenShot(Context context) {
		setupScreenShot(context);
		if (findOrCreateBin()) {
			try {
				String timestamp = ""+System.currentTimeMillis();
				String imgPath = imgDir+"/"+timestamp+".png";
				(new ShellCommands()).executeCommand(binDir+"/fb2png "+imgPath, null, false, context);
		        if ((new File(imgPath)).exists()) {
		        	app.screenShotDb.dbCaptured.insert(timestamp, "png", (new FileUtils()).sha1Hash(imgPath));
		        	// GZipping PNGs doesn't really do anything, so we just leave it as PNG
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
	
//	public void purgeSingleScreenShot(String screenShotTimeStamp) {
//		Log.v(TAG, "Purging single screenshot: "+screenShotTimeStamp);
//		if (this.app != null) {
//			try {
//				File screenShotFile = new File(this.app.getApplicationContext().getFilesDir().toString()+"/img",screenShotTimeStamp+".png");
//			} catch (Exception e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//			}
//		}
//	}
	
//	public void purgeAllScreenShots(ScreenShotDb screenShotDb) {
//		Log.v(TAG, "Purging all existing screenshots...");
//		if (this.app != null) {
//			try {
//				String screenShotDir = this.app.getApplicationContext().getFilesDir().toString()+"/img";
//				for (File file : (new File(screenShotDir)).listFiles()) { file.delete(); }
//		
//			} catch (Exception e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//			}
//		}
//		screenShotDb.dbScreenShot.clearScreenShotsBefore(new Date());
//	}
	
    private boolean findOrCreateBin() {
    	try {
	     	File fb2pngFile = new File(binDir+"/fb2png");
	     	
	        // check that module is not already installed before starting
	        if (!fb2pngFile.exists()) {
	    		try {
	    			InputStream inputStream = app.getAssets().open("bin/fb2png");
	    		    OutputStream outputStream = new FileOutputStream(binDir+"/fb2png");
	    		    byte[] buf = new byte[1024];
	    		    int len;
	    		    while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
	    		    inputStream.close();
	    		    outputStream.close();
	    		    (new FileUtils()).chmod(fb2pngFile, 0755);
	    		    return fb2pngFile.exists();
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
	
    public String getScreenShotDirectory(Context context) {
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (filesDir == null) filesDir = app.getFilesDir().getAbsolutePath();
		imgDir = filesDir+"/img";
		if ((new File(sdCardFilesDir)).isDirectory()) { imgDir = sdCardFilesDir+"/img"; }
		return imgDir;
    }
}
