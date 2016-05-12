package org.rfcx.guardian.utility.device;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class DeviceAirplaneMode {

	public DeviceAirplaneMode(String appRole) {
		this.logTag = "Rfcx-"+appRole+"-"+DeviceAirplaneMode.class.getSimpleName();
	}
	
	private String logTag = "Rfcx-Utils-"+DeviceAirplaneMode.class.getSimpleName();
	
	private boolean isEnabled;
	
	public boolean isEnabled(Context context) {
		isEnabled = Settings.System.getInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		return isEnabled;
	}
	
	public void setOn(Context context) {
		Log.v(logTag, "Turning AirplaneMode ON");
    	if (!isEnabled(context)) {
    		set(context, 1);
    	}
	}
	
	public void setOff(Context context) {
		Log.v(logTag, "Turning AirplaneMode OFF");
    	if (!isEnabled(context)) {
    		set(context, 1);
    		set(context, 0);
    	} else {
    		set(context, 0);
    	}
	}
	
	private void set(Context context, int value) {
		try {
			Settings.System.putInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, value);
        	Intent intentAp = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        	intentAp.putExtra("state", (value == 1) ? true : false);
        	context.sendBroadcast(intentAp);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
}
