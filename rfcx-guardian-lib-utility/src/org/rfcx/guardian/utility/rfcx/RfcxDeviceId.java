package org.rfcx.guardian.utility.rfcx;

import java.security.MessageDigest;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class RfcxDeviceId {
	
	private static final String TAG = "Rfcx-Utils-"+RfcxDeviceId.class.getSimpleName();
	
	private String telephonyId;
	private String deviceGuid;
	private String deviceToken;

	public RfcxDeviceId init(Context context) {
		if (telephonyId == null) {
			telephonyId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId().toString();
		}
		return this;
	}
	
    public String getDeviceGuid() {
    	if (deviceGuid == null) {
			try {
				MessageDigest telephonyIdMd5 = MessageDigest.getInstance("MD5");
				byte[] telephonyIdMd5Bytes = telephonyIdMd5.digest(telephonyId.getBytes("UTF-8"));
			    StringBuffer stringBuilder = new StringBuffer("");
			    for (int i = 0; i < telephonyIdMd5Bytes.length; i++) {
			    	stringBuilder.append(Integer.toString((telephonyIdMd5Bytes[i] & 0xff) + 0x100, 16).substring(1));
			    }
			    deviceGuid = stringBuilder.toString().substring(0,12);
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//				String randomGuid = (UUID.randomUUID()).toString();
//				deviceGuid = randomGuid.substring(1+randomGuid.lastIndexOf("-"));
			}
    	}
    	return deviceGuid;
    }
    
    public String getDeviceToken() {
    	if (deviceToken == null) {
			try {
				MessageDigest telephonyIdSha1 = MessageDigest.getInstance("SHA-1");
				telephonyIdSha1.update(telephonyId.getBytes("UTF-8"));
				byte[] telephonyIdSha1Bytes = telephonyIdSha1.digest();
			    StringBuffer stringBuilder = new StringBuffer("");
			    for (int i = 0; i < telephonyIdSha1Bytes.length; i++) {
			    	stringBuilder.append(Integer.toString((telephonyIdSha1Bytes[i] & 0xff) + 0x100, 16).substring(1));
			    }
			    deviceToken = stringBuilder.toString();
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//				deviceToken = ((UUID.randomUUID()).toString()+(UUID.randomUUID()).toString()+(UUID.randomUUID()).toString()).replaceAll("-","").substring(0,40);
			}
    	}
		return deviceToken;
    }

}
