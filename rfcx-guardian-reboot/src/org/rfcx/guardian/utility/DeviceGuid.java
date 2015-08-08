package org.rfcx.guardian.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.UUID;

import org.rfcx.guardian.reboot.R;
import org.rfcx.guardian.reboot.RfcxGuardian;

public class DeviceGuid {
	
	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+DeviceGuid.class.getSimpleName();
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
                    	deviceGuid = getExistingGuidFromGuardianApp(context);
                    	if (deviceGuid == null) {
                    		try {
                    			String telephonyId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId().toString();
                    			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    			byte[] messageDigestBytes = messageDigest.digest(telephonyId.getBytes("UTF-8"));
                    		    StringBuffer stringBuilder = new StringBuffer("");
                    		    for (int i = 0; i < messageDigestBytes.length; i++) {
                    		    	stringBuilder.append(Integer.toString((messageDigestBytes[i] & 0xff) + 0x100, 16).substring(1));
                    		    }
                    		    deviceGuid = stringBuilder.toString().substring(0,12);
                    		} catch (Exception e) {
                    			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
                    			String randomGuid = (UUID.randomUUID()).toString();
                        		deviceGuid = randomGuid.substring(1+randomGuid.lastIndexOf("-"));
                    		}
                    		Log.d(TAG,deviceGuid);
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
    
    private static String getExistingGuidFromGuardianApp(Context context) {
    	try {
    		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
    		String mainAppPath = context.getFilesDir().getAbsolutePath();
    		Log.d(TAG,mainAppPath.substring(0,mainAppPath.lastIndexOf("/files")-(("."+app.thisAppRole).length()))+"."+app.targetAppRole+"/files/txt/guid.txt");
    		File guidFile = new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/files")-(("."+app.thisAppRole).length()))+"."+app.targetAppRole+"/files/txt","guid.txt");
    		if (guidFile.exists()) {
				FileInputStream input = new FileInputStream(guidFile);
				StringBuffer fileContent = new StringBuffer("");
				byte[] buffer = new byte[12];
				while (input.read(buffer) != -1) {
				    fileContent.append(new String(buffer));
				}
	    		String guidString = fileContent.toString();
	    		input.close();
	    		Log.d(TAG, "Fetched GUID from org.rfcx.guardian."+app.targetAppRole+": "+guidString);
	    		return guidString;
    		} else {
    			Log.e(TAG, "No previous GUID saved by org.rfcx.guardian."+app.targetAppRole+"...");
    		}
    	} catch (FileNotFoundException e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
    	} catch (IOException e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		}
    	return null;
    }
}
