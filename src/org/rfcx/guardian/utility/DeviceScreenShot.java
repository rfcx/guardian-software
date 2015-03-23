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
	
    public boolean findOrCreateBin(Context context) {
    	
        // setup variables
    	this.app = (RfcxGuardian) context.getApplicationContext();
    	FileUtils fileUtils = new FileUtils();
     	String fb2pngLocation = app.getFilesDir().toString()+"/bin/fb2png";
     	File fb2pngFile = new File(fb2pngLocation);
     	fb2pngFile.mkdirs();

        // check that module is not already installed before starting
        if (!fb2pngFile.exists()) {
        	// downloads screenshot code if not found at install time.
            Log.i(TAG,"Downloading screenshot module from server");
        	if ((new HttpGet()).getAsFile(fb2pngDownloadUrl, fb2pngLocation, app) && fileUtils.sha1Hash(fb2pngLocation).equals(fb2pngSha1)) { 
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
    }
    
	public String saveScreenShot(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		String cachePath = app.getFilesDir().getAbsolutePath() + "/img.png";
		String binPath = app.getFilesDir().getAbsolutePath() + "/bin/fb2png";
		if (findOrCreateBin(context)) {
			try {
				(new File(cachePath)).delete();
				(new ShellCommands()).executeCommandAsRoot(binPath+" "+cachePath, null, context);
		        File cacheFile = new File(cachePath);
		        if (cacheFile.exists()) {
		        	long timestamp = cacheFile.lastModified();
		        	String imgDir = app.getApplicationContext().getFilesDir().toString()+"/img";
			    	(new File(imgDir)).mkdirs();
			    	String imgPath = imgDir+"/"+timestamp+".png";
			    	Log.d(TAG,"Screenshot saved: "+imgPath);
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
		Log.d(TAG, "Purging all existing screenshots...");
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
