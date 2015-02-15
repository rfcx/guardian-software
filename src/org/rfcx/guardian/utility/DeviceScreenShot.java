package org.rfcx.guardian.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.rfcx.guardian.RfcxGuardian;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class DeviceScreenShot {
	// Holds all methods relating to checking if screenshot module is setup, downloading/installing the module, and taking a picture.
	private static final String TAG = "RfcxGuardian-"+DeviceScreenShot.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
    public void checkModuleInstalled(Context context) {
        // setup variables
    	RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
     	String filePath = app.getFilesDir().getAbsolutePath() + "/fb2png";
        String repoUrl = "http://rfcx-install.s3.amazonaws.com/fb2png/fb2png";

        // check that module is not already installed before starting
        Log.i(TAG, "Checking for existance of screenshot module");
        if ((new File(filePath)).exists()) {
        	Log.i(TAG, "Screenshot module already installed.");
        } else {
        	// downloads screenshot code if not found at install time.
            Log.i(TAG,"Downloading screenshot module from server");
        	if ((new HttpGet()).getAsFile(repoUrl, "fb2png", app)) { 
        		Log.i(TAG,"File download complete");
            	(new FileUtils()).chmod(new File(filePath), 0755);
        	}
        }
    }
    
	public String saveScreenShot(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		String cachePath = app.getFilesDir().getAbsolutePath() + "/img.png";
		String modulePath = app.getFilesDir().getAbsolutePath() + "/fb2png";
		try {
			(new File(cachePath)).delete();
			(new ShellCommands()).executeCommandAsRoot(modulePath+" "+cachePath, null, context);
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
		return null;
	}
	
}
