package org.rfcx.src_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	private static final String TAG = BootReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (RfcxSource.VERBOSE) { Log.d(TAG, "onReceive()"); }	
		((RfcxSource) context.getApplicationContext()).launchServices(context);
	
	}

}
