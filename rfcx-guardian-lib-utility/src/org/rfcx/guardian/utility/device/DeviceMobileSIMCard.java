package org.rfcx.guardian.utility.device;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.telephony.TelephonyManager;

public class DeviceMobileSIMCard {
	
	public DeviceMobileSIMCard(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceMobileSIMCard.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", DeviceMobileSIMCard.class);
	
	public static String getSIMSerial(Context context) {
		return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSimSerialNumber();
	}	
	
	public static String getIMSI(Context context) {
		return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSubscriberId();
	}	

	public static String getIMEI(Context context) {
		return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
	}	
}
