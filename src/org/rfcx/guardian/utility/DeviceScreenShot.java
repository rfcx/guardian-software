package org.rfcx.guardian.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.database.AudioDb;
import org.rfcx.guardian.database.ScreenShotDb;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class DeviceScreenShot {
	// Holds all methods relating to checking if screenshot module is setup, downloading/installing the module, and taking a picture.
	private static final String TAG = "RfcxGuardian-"+DeviceScreenShot.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
 	private static final String fb2pngDownloadUrl = "http://rfcx-install.s3.amazonaws.com/fb2png/fb2png";
	private static final String fb2pngSha1 = "b6084874174209b544dd2dadcb668e71584f8bf4";
	
	private RfcxGuardian app = null;
	private String appDir = null;
	private String imgDir = null;
	private String cachePath = null;
	private String binPath = null;

	public void setupScreenShot(Context context) {
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (appDir == null) appDir = app.getFilesDir().getAbsolutePath();
		if (imgDir == null) imgDir = appDir+"/img";
		if (cachePath == null) cachePath = appDir+"/screenshot.png";
		if (binPath == null) binPath = appDir+"/bin/fb2png";
		(new File(appDir+"/bin")).mkdirs();
		findOrCreateBin(context);
	}
	
    private boolean findOrCreateBin(Context context) {
    	try {
	    	FileUtils fileUtils = new FileUtils();
	     	File fb2pngFile = new File(binPath);
	     	if ((new File(appDir+"/fb2png")).exists()) { (new File(appDir+"/fb2png")).delete(); }
	     	
	        // check that module is not already installed before starting
	        if (!fb2pngFile.exists()) {
	        	// downloads screenshot code if not found at install time.
	            Log.i(TAG,"Downloading screenshot binary from server");
	        	if (	(new HttpGet()).getAsFile(fb2pngDownloadUrl, "fb2png", context)
	        		&& 	fileUtils.sha1Hash(appDir+"/fb2png").equals(fb2pngSha1)
	        		&& 	(new File(appDir+"/fb2png")).renameTo(new File(appDir+"/bin/fb2png"))
	        		) { 
	        		Log.i(TAG,"File download complete and checksum verified.");
	            	(new FileUtils()).chmod(fb2pngFile, 0755);
	            	return true;
	        	} else {
	        		fb2pngFile.delete();
	            	return false;
	        	}
	        } else {
	        	return true;
	        }
    	} catch (Exception e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
    		return false;
    	}
    }
    
	public String saveScreenShot(Context context) {
		
		setupScreenShot(context);
		
		if (findOrCreateBin(context)) {
			try {
				(new File(cachePath)).delete();
				(new ShellCommands()).executeCommandAsRoot(binPath+" "+cachePath, null, context);
		        File cacheFile = new File(cachePath);
		        if (cacheFile.exists()) {
		        	long timestamp = cacheFile.lastModified();
			    	(new File(imgDir)).mkdirs();
			    	String imgPath = imgDir+"/"+timestamp+".png";
			    	if (app.verboseLog) Log.d(TAG,"Screenshot saved: "+imgPath);
			    	File imgFile = new File(imgPath);
			    	
		    		InputStream cacheFileInputStream = new FileInputStream(cacheFile);
		    		OutputStream imgFileOutputStream = new FileOutputStream(imgFile);
			    	try {
				    	byte[] buf = new byte[1024];
				    	int len; while ((len = cacheFileInputStream.read(buf)) > 0) { imgFileOutputStream.write(buf, 0, len); };
			    	} finally {
				    	cacheFileInputStream.close();
				    	imgFileOutputStream.close();
			    	}
			    	if (imgFile.exists()) {
			    		app.screenShotDb.dbScreenShot.insert(timestamp);
			    		return ""+timestamp;
			    	}
		        }
			} catch (IOException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			}
		} else {
			Log.e(TAG, "Failed to find and/or install fb2png. Cannot produce screenshot");
		}
		return null;
	}
	
	public void purgeAllScreenShots(ScreenShotDb screenShotDb) {
		if (app.verboseLog) Log.d(TAG, "Purging all existing screenshots...");
		if (this.app != null) {
			try {
				String screenShotDir = this.app.getApplicationContext().getFilesDir().toString()+"/img";
				for (File file : (new File(screenShotDir)).listFiles()) { file.delete(); }
		
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			}
		}
		screenShotDb.dbScreenShot.clearScreenShotsBefore(new Date());
	}
	
}
