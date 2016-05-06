package org.rfcx.guardian.utility.device;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.UUID;

import org.rfcx.guardian.utility.RfcxConstants;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class DeviceGuid {
	
	private static final String TAG = "Rfcx-Utils-"+DeviceGuid.class.getSimpleName();
    protected static String deviceGuid;

    public DeviceGuid(Context context) {
        if (deviceGuid == null) {
            synchronized (DeviceGuid.class) {
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
            			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
            			String randomGuid = (UUID.randomUUID()).toString();
                		deviceGuid = randomGuid.substring(1+randomGuid.lastIndexOf("-"));
            		}
//            		Log.d(TAG,"GUID: "+deviceGuid);
            	}
            }
        }
    }

    public String getDeviceId() {
        return deviceGuid;
    }

}
