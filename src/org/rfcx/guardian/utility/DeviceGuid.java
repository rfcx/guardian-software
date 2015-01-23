package org.rfcx.guardian.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

public class DeviceGuid {
	
	private static final String TAG = DeviceGuid.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	protected static final String PREFS_DEVICE_GUID = "device_guid";
    protected static String deviceGuid;

    public DeviceGuid(Context context, SharedPreferences prefs) {
        if (deviceGuid == null) {
            synchronized (DeviceGuid.class) {
                if (deviceGuid == null) {
                	final String prefsDeviceGuid = prefs.getString(PREFS_DEVICE_GUID,null);
                    if ((prefsDeviceGuid != null) && (prefsDeviceGuid.length() > 10)) {
                        deviceGuid = prefsDeviceGuid;
                    } else{
                    	deviceGuid = getExistingGuidFromUpdaterApp(context);
                    	if (deviceGuid == null) { 
                    		String randomUuid = (UUID.randomUUID()).toString();
                    		deviceGuid = randomUuid.substring(1+randomUuid.lastIndexOf("-"));
                    	}
                    	prefs.edit().putString(PREFS_DEVICE_GUID, deviceGuid).commit();
                    }
                }
            }
        }
    }


    public String getDeviceId() {
        return deviceGuid;
    }
    
    private static String getExistingGuidFromUpdaterApp(Context context) {
    	try {
    		String mainAppPath = context.getFilesDir().getAbsolutePath();
    		Log.d(TAG,mainAppPath.substring(0,mainAppPath.lastIndexOf("/files"))+".updater/files/txt/device_guid.txt;");
    		File guidFile = new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/files"))+".updater/files/txt","device_guid.txt");
    		if (guidFile.exists()) {
				FileInputStream input = new FileInputStream(guidFile);
				StringBuffer fileContent = new StringBuffer("");
				byte[] buffer = new byte[12];
				while (input.read(buffer) != -1) {
				    fileContent.append(new String(buffer));
				}
	    		String guidString = fileContent.toString();
	    		input.close();
	    		Log.d(TAG, "Fetched GUID from RfcxGuardianUpdater: "+guidString);
	    		return guidString;
    		} else {
    			Log.e(TAG, "No previous GUID saved by RfcxGuardianUpdater...");
    		}
    	} catch (FileNotFoundException e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
    	} catch (IOException e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
    	return null;
    }
}
