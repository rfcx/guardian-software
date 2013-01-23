package org.rfcx.src_state;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AirplaneModeReceiver extends BroadcastReceiver {
	
	private static final String TAG = AirplaneModeReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG,"onReceive()");
	}

}
