package org.rfcx.guardian.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rfcx.guardian.RfcxGuardian;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class DeviceScreenShot {
	/* 	
	 	Holds all methods relating to checking if screenshot module
    	is setup, downloading/installing the module, and taking a picture.
    */
	private RfcxGuardian app = null;
	
	private static final String TAG = DeviceScreenShot.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
    public void checkModuleInstalled() {
        // setup variables
    	HttpGet httpGet = new HttpGet();
    	Context context = app.getApplicationContext();
//    	String fileName = context.getFilesDir().getAbsolutePath();
    	String fileName = "/data/data/org.rfcx.guardian/files/fb2png";
        String repo = "https://android-fb2png.googlecode.com/files/fb2png";
        String serverSha1 = "b6084874174209b544dd2dadcb668e71584f8bf4";

        // check that module is not already installed before starting
        Log.i(TAG, "Checking for existance of screenshot module");
        if ((new File(fileName)).exists()) {
            Log.i(TAG, "Screenshot module already installed.");
        }
        else {
            // attempt to download until max attempts reached
            do {
            	// downloads and screenshot code if not found at install time.
                Log.i(TAG,"Downloading screenshot module from server");
                try {
                	if (httpGet.getAsFile(repo, fileName, context)) { 
                		Log.i(TAG,"File download complete");
                	}
                }
                catch (Exception e) {
                    Log.e(TAG,"Failed to download file");
                }
            } while (serverSha1 != (new FileUtils()).sha1Hash(fileName));
            
            // setup the code by setting the proper chmod permissions
            Log.i(TAG,"Setting up screenshot module");
            try {
                ProcessBuilder pb = new ProcessBuilder("chmod", "755", fileName);
                pb.start();
                Log.i(TAG, "Screenshot module installed successfully.");
            }
            catch (Exception e) {
                Log.e(TAG,"Failed to setup the screenshot module");
            }
        }
    }
    
	public String saveScreenShot(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		String cachePath = "/data/local/img.png";
		try {
			(new File(cachePath)).delete();
	        ProcessBuilder pb = new ProcessBuilder("su", "-c", "/data/local/fb2png "+cachePath);
	        Process pc = pb.start();
	        pc.waitFor();
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
		} catch (InterruptedException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return null;
	}
	
}
