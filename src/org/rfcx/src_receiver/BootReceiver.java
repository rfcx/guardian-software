package org.rfcx.src_receiver;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	private static final String TAG = BootReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "BootReceiver Launching Intent Services");
		((RfcxSource) context.getApplicationContext()).launchAllIntentServices(context);
	}

}
