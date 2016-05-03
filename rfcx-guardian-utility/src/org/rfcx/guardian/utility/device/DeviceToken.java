package org.rfcx.guardian.utility.device;

import java.security.MessageDigest;
import java.util.UUID;

import org.rfcx.guardian.utility.RfcxConstants;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class DeviceToken {
	
	private static final String TAG = "Rfcx-Utils-"+DeviceToken.class.getSimpleName();
    protected static String deviceToken;

    public DeviceToken(Context context) {
        if (deviceToken == null) {
            synchronized (DeviceToken.class) {
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

    public String getDeviceToken() {
        return deviceToken;
    }
    
}

