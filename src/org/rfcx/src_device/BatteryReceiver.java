package org.rfcx.src_device;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BatteryReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
//		((RfcxSource) context.getApplicationContext()).deviceState.setBatteryState(context, intent);
	}
	
}
