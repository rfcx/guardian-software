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
import android.text.TextUtils;
import android.util.Log;

public class DeviceScreenShot {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+DeviceScreenShot.class.getSimpleName();

	FileUtils fileUtils = new FileUtils();
	GZipUtils gZipUtils = new GZipUtils();
	ShellCommands shellCommands = new ShellCommands();
	
	private RfcxGuardian app = null;
	private String filesDir = null;

	public void setupScreenShot(Context context) {
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();
		if (filesDir == null) filesDir = app.getFilesDir().getAbsolutePath();
		(new File(filesDir+"/bin")).mkdirs();
		(new File(filesDir+"/img")).mkdirs();
	}
    
	public String saveScreenShot(Context context) {
		setupScreenShot(context);
		if (findOrCreateBin()) {
			long timestamp = System.currentTimeMillis();
			String imgPath = filesDir+"/img/"+timestamp+".png";
			shellCommands.executeCommand(filesDir+"/bin/fb2png "+imgPath, null, false, context);
			File imgFile = new File(imgPath);
	        if (imgFile.exists()) {
//			    app.screenShotDb.dbScreenShot.insert(timestamp);
	        	File gzImgFile = new File(imgPath+".gz");
		    	gZipUtils.gZipFile(imgFile, gzImgFile);
		    	if (gzImgFile.exists()) {
		    		imgFile.delete();
		    		return ""+timestamp;
		    	}
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
	     	File fb2pngFile = new File(filesDir+"/bin/fb2png");
	     	
	        // check that module is not already installed before starting
	        if (!fb2pngFile.exists()) {
	    		try {
	    			InputStream inputStream = app.getAssets().open("bin/fb2png");
	    		    OutputStream outputStream = new FileOutputStream(filesDir+"/bin/fb2png");
	    		    byte[] buf = new byte[1024];
	    		    int len;
	    		    while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }
	    		    inputStream.close();
	    		    outputStream.close();
	    		    fileUtils.chmod(fb2pngFile, 0755);
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
	
}
