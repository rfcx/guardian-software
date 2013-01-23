package org.rfcx.src_state;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class AirplaneMode {

	private static final String TAG = AirplaneMode.class.getSimpleName();
	
	private boolean isEnabled;
	
	private boolean isEnabled(Context context) {
		isEnabled = Settings.System.getInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		return isEnabled;
	}
	
	public void setOn(Context context) {
    	Log.d(TAG, "setOn()");
    	if (!isEnabled(context)) {
    		set(context, 1);
    	}
	}
	
	public void setOff(Context context) {
    	Log.d(TAG, "setOff()");
    	if (isEnabled(context)) {
    		set(context, 0);
    	}
	}
	
	private void set(Context context, int value) {
		try {
			Settings.System.putInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, value);
        	Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        	intent.putExtra("state", (value == 1) ? true : false);
        	context.sendBroadcast(intent);
		} catch (Exception e) {
			Log.d(TAG, "Failed: "+e.getMessage());
		}
	}
	
}
