package org.rfcx.src_receiver;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		((RfcxSource) context.getApplicationContext()).launchAllIntentServices(context);
	}

}
