package org.rfcx.guardian.utility.rfcx;

import java.security.MessageDigest;
import java.util.UUID;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class RfcxDeviceId {
	
	private static final String TAG = "Rfcx-Utils-"+RfcxDeviceId.class.getSimpleName();
	
    protected static String deviceGuid;
    protected static String deviceToken;

    public RfcxDeviceId(Context context) {
    	
        if (deviceGuid == null) {
            synchronized (RfcxDeviceId.class) {
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
            	}
            }
        }
        
        if (deviceToken == null) {
            synchronized (RfcxDeviceId.class) {
                if (deviceToken == null) {
            		try {
            			String telephonyId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId().toString();
            			MessageDigest telephonyIdDigest = MessageDigest.getInstance("SHA-1");
            			telephonyIdDigest.update(telephonyId.getBytes("UTF-8"));
            			byte[] telephonyIdDigestBytes = telephonyIdDigest.digest();
            		    StringBuffer telephonyIdDigestStringBuilder = new StringBuffer("");
            		    for (int i = 0; i < telephonyIdDigestBytes.length; i++) {
            		    	telephonyIdDigestStringBuilder.append(Integer.toString((telephonyIdDigestBytes[i] & 0xff) + 0x100, 16).substring(1));
            		    }
            		    deviceToken = telephonyIdDigestStringBuilder.toString();
            		} catch (Exception e) {
            			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
            			deviceToken = ((UUID.randomUUID()).toString()+(UUID.randomUUID()).toString()+(UUID.randomUUID()).toString()).replaceAll("-","").substring(0,40);
            		}
                    
                }
            }
        }
        
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }
    
    public String getDeviceToken() {
        return deviceToken;
    }

}
