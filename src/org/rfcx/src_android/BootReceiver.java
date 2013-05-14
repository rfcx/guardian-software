package org.rfcx.src_android;

import org.rfcx.src_api.ApiCommService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	private static final String TAG = BootReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (RfcxSource.VERBOSE) Log.d(TAG, "BroadcastReceiver: "+TAG);
		((RfcxSource) context.getApplicationContext()).launchAllServices(context);
		((RfcxSource) context.getApplicationContext()).launchAllIntentServices(context);
	}

}
