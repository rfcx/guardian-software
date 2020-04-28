package org.rfcx.guardian.utility.rfcx;

import java.security.MessageDigest;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class RfcxGuardianIdentity {

	public RfcxGuardianIdentity(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxGuardianIdentity");
		this.context = context;
		this.appRole = appRole;
		checkSetTelephonyId();
		checkSetCustomGuid();
	}

	private String logTag;

	private Context context;
	private String appRole;
	private String telephonyId;
	private String guid;
	private String authToken;

	@SuppressLint("MissingPermission")
	private void checkSetTelephonyId() {
		try {
			if (this.telephonyId == null) {
				this.telephonyId = ((TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId().toString();
			}
		} catch (Exception e) {
			RfcxLog.logExc(this.logTag, e);
		}
	}

	public void setGuid(String guid) {
		RfcxPrefs.writeGuidToFile(this.context, this.logTag, guid);
		checkSetCustomGuid();
	}
	
	private void checkSetCustomGuid() {
		String customOrPreSetGuid = RfcxPrefs.getGuidFromFile(this.context, this.logTag, this.appRole, "org.rfcx.guardian.guardian");
		if (customOrPreSetGuid != null) {
			this.guid = customOrPreSetGuid;
		}
	}
	
    public String getGuid() {
	    	if (this.guid == null) {
	    		checkSetCustomGuid();
	    		if (this.guid == null) {
	    			checkSetTelephonyId();
				try {
		    		checkSetTelephonyId();
					MessageDigest telephonyIdMd5 = MessageDigest.getInstance("MD5");
					byte[] telephonyIdMd5Bytes = telephonyIdMd5.digest(telephonyId.getBytes("UTF-8"));
				    StringBuffer stringBuffer = new StringBuffer("");
				    for (int i = 0; i < telephonyIdMd5Bytes.length; i++) {
				    		stringBuffer.append(Integer.toString((telephonyIdMd5Bytes[i] & 0xff) + 0x100, 16).substring(1));
				    }
				    this.guid = stringBuffer.toString().substring(0,12);
				    RfcxPrefs.writeGuidToFile(this.context, this.logTag, this.guid);
				} catch (Exception e) {
					RfcxLog.logExc(this.logTag, e);
					String randomGuid = (UUID.randomUUID()).toString();
					guid = randomGuid.substring(1+randomGuid.lastIndexOf("-"));
					Log.d("guid", guid);
				}
	    		}
	    	}
	    	FirebaseCrashlytics.getInstance().setUserId(this.guid);
	    	return this.guid;
    }
    
    public String getAuthToken() {
	    	if (this.authToken == null) {
	    		checkSetTelephonyId();
			try {
				MessageDigest telephonyIdSha1 = MessageDigest.getInstance("SHA-1");
				telephonyIdSha1.update(telephonyId.getBytes("UTF-8"));
				byte[] telephonyIdSha1Bytes = telephonyIdSha1.digest();
			    StringBuffer stringBuffer = new StringBuffer("");
			    for (int i = 0; i < telephonyIdSha1Bytes.length; i++) {
			    		stringBuffer.append(Integer.toString((telephonyIdSha1Bytes[i] & 0xff) + 0x100, 16).substring(1));
			    }
			    this.authToken = stringBuffer.toString();
			} catch (Exception e) {
				RfcxLog.logExc(this.logTag, e);
				authToken = ((UUID.randomUUID()).toString()+(UUID.randomUUID()).toString()+(UUID.randomUUID()).toString()).replaceAll("-","").substring(0,40);
			}
	    	}
		return this.authToken;
    }

}
