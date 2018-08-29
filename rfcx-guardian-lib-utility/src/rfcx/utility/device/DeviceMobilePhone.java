package rfcx.utility.device;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceMobilePhone {
	
	public DeviceMobilePhone(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceMobilePhone.class);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", DeviceMobilePhone.class);
	
	public static String getSIMSerial(Context context) {
		return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSimSerialNumber();
	}	
	
	public static String getIMSI(Context context) {
		return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSubscriberId();
	}	

	public static String getIMEI(Context context) {
		return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
	}	
	
	public static String getConcatMobilePhoneInfo(Context context) {
		return (TextUtils.join("|", new String[] {
				"sim*"+getSIMSerial(context),
				"imsi*"+getIMSI(context),
				"imei*"+getIMEI(context)
			}));
	}
}
