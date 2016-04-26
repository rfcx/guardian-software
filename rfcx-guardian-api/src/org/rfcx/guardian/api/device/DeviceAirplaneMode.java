package org.rfcx.guardian.api.device;

import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class DeviceAirplaneMode {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceAirplaneMode.class.getSimpleName();
	
	private boolean isEnabled;
	
	private RfcxGuardian app = null;
	
	public boolean isEnabled(Context context) {
		isEnabled = Settings.System.getInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		return isEnabled;
	}
	
	public void setOn(Context context) {
		if (app == null) { app = (RfcxGuardian) context.getApplicationContext(); }
		Log.v(TAG, "Turning AirplaneMode ON");
    	if (!isEnabled(context)) {
    		set(context, 1);
    	}
	}
	
	public void setOff(Context context) {
		if (app == null) { app = (RfcxGuardian) context.getApplicationContext(); }
		Log.v(TAG, "Turning AirplaneMode OFF");
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
			Log.e(TAG,(e!=null) ? e.getMessage() : RfcxConstants.NULL_EXC);
		}
	}
	
}
