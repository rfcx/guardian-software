package org.rfcx.guardian.utility.rfcx;

import java.security.MessageDigest;

import android.content.Context;
import android.telephony.TelephonyManager;

public class RfcxDeviceGuid {
	
	public RfcxDeviceGuid(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, RfcxDeviceGuid.class);
		this.context = context;
		this.appRole = appRole;
		checkSetTelephonyId();
		checkSetCustomDeviceGuid();
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", RfcxDeviceGuid.class);
	
	private Context context;
	private String appRole;
	private String telephonyId;
	private String deviceGuid;
	private String deviceToken;
	
	private void checkSetTelephonyId() {
		try {
			if (this.telephonyId == null) {
				this.telephonyId = ((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId().toString();
			}
		} catch (Exception e) {
			RfcxLog.logExc(this.logTag, e);
		}
	}
	
	private void checkSetCustomDeviceGuid() {
		String customOrPreSetDeviceGuid = RfcxPrefs.getGuidFromFile(this.context, this.logTag, this.appRole, "setup");
		if (customOrPreSetDeviceGuid != null) {
			this.deviceGuid = customOrPreSetDeviceGuid;
		}
	}
	
    public String getDeviceGuid() {
	    	if (this.deviceGuid == null) {
	    		checkSetCustomDeviceGuid();
	    		if (this.deviceGuid == null) {
	    			checkSetTelephonyId();
				try {
		    			checkSetTelephonyId();
					MessageDigest telephonyIdMd5 = MessageDigest.getInstance("MD5");
					byte[] telephonyIdMd5Bytes = telephonyIdMd5.digest(telephonyId.getBytes("UTF-8"));
				    StringBuffer stringBuffer = new StringBuffer("");
				    for (int i = 0; i < telephonyIdMd5Bytes.length; i++) {
				    		stringBuffer.append(Integer.toString((telephonyIdMd5Bytes[i] & 0xff) + 0x100, 16).substring(1));
				    }
				    this.deviceGuid = stringBuffer.toString().substring(0,12);
				    RfcxPrefs.writeGuidToFile(this.context, this.logTag, this.deviceGuid);
				} catch (Exception e) {
					RfcxLog.logExc(this.logTag, e);
	//				String randomGuid = (UUID.randomUUID()).toString();
	//				deviceGuid = randomGuid.substring(1+randomGuid.lastIndexOf("-"));
				}
	    		}
	    	}
	    	return this.deviceGuid;
    }
    
    public String getDeviceToken() {
	    	if (this.deviceToken == null) {
	    		checkSetTelephonyId();
			try {
				MessageDigest telephonyIdSha1 = MessageDigest.getInstance("SHA-1");
				telephonyIdSha1.update(telephonyId.getBytes("UTF-8"));
				byte[] telephonyIdSha1Bytes = telephonyIdSha1.digest();
			    StringBuffer stringBuffer = new StringBuffer("");
			    for (int i = 0; i < telephonyIdSha1Bytes.length; i++) {
			    		stringBuffer.append(Integer.toString((telephonyIdSha1Bytes[i] & 0xff) + 0x100, 16).substring(1));
			    }
			    this.deviceToken = stringBuffer.toString();
			} catch (Exception e) {
				RfcxLog.logExc(this.logTag, e);
//				deviceToken = ((UUID.randomUUID()).toString()+(UUID.randomUUID()).toString()+(UUID.randomUUID()).toString()).replaceAll("-","").substring(0,40);
			}
	    	}
		return this.deviceToken;
    }

}
