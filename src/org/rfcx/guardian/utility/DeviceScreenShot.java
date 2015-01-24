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

	private static final String TAG = DeviceScreenShot.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public String saveScreenShot(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		FileUtils fileUtils = new FileUtils();
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
