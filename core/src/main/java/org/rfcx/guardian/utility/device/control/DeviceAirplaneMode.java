package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceAirplaneMode {

	public DeviceAirplaneMode(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceAirplaneMode");
	}
	
	private String logTag;
	
	public static boolean isEnabled(Context context) {
		return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
	}
	
	public void setOn(Context context) {
		Log.v(logTag, "Turning AirplaneMode ON");
	    	if (!isEnabled(context)) {
	    		setAirplaneMode(context, 1, logTag);
	    	}
	}
	
	public void setOff(Context context) {
		Log.v(logTag, "Turning AirplaneMode OFF");
	    	if (!isEnabled(context)) {
	    		setAirplaneMode(context, 1, logTag);
	    		setAirplaneMode(context, 0, logTag);
	    	} else {
	    		setAirplaneMode(context, 0, logTag);
	    	}
	}
	
	private static void setAirplaneMode(Context context, int value, String logTag) {
		try {
			Settings.System.putInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, value);
	        	Intent airplaneModeIntent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
	        	airplaneModeIntent.putExtra("state", (value == 1) ? true : false);
	        	context.sendBroadcast(airplaneModeIntent);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
}
